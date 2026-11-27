package com.elec5619.libraryreservationreviewhub.controller;

import com.elec5619.libraryreservationreviewhub.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Guesses sent at the same moment are held to the same limit as guesses sent one by one: an attempt is counted
 * before the password is checked. Not transactional, so the requests really run in parallel.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ParallelSignInIntegrationTest {

    private static final String PASSWORD = "right-password-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String email = "parallel-" + UUID.randomUUID() + "@example.com";

    @AfterEach
    void deleteTheAccount() {
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", email);
    }

    private int login(String password) throws Exception {
        String body = mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"" + email + "\",\"userPassword\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return Integer.parseInt(body.replaceAll(".*\"code\":(\\d+).*", "$1"));
    }

    @Test
    void parallelGuessesAreHeldToTheLimit() throws Exception {
        userService.userRegister(email, PASSWORD, PASSWORD, "Para", "Llel", "0400000000");
        int guesses = 40;
        ExecutorService pool = Executors.newFixedThreadPool(guesses);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> codes = new ArrayList<>();
        for (int i = 0; i < guesses; i++) {
            int n = i;
            codes.add(pool.submit(() -> {
                start.await();
                return login("wrong-" + n);
            }));
        }
        start.countDown();
        int checked = 0;
        int refused = 0;
        for (Future<Integer> code : codes) {
            int c = code.get(60, TimeUnit.SECONDS);
            if (c == 40000) {
                checked++;
            } else if (c == 42900) {
                refused++;
            }
        }
        pool.shutdown();

        // at most 5 guesses reach the password check; the rest are refused by the limit
        assertThat(checked).isEqualTo(5);
        assertThat(refused).isEqualTo(guesses - 5);
        // and the right password is refused too until the window ends
        assertThat(login(PASSWORD)).isEqualTo(42900);
    }
}
