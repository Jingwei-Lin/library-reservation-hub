package com.elec5619.libraryreservationreviewhub.controller;

import com.elec5619.libraryreservationreviewhub.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** The profile page keeps the registration rules for the e-mail and the phone. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileFormatIntegrationTest {

    private static final String PASSWORD = "right-password-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String email = "profile-" + UUID.randomUUID() + "@example.com";
    private MockHttpSession session;

    @BeforeEach
    void signIn() throws Exception {
        userService.userRegister(email, PASSWORD, PASSWORD, "Pro", "File", "0400000000");
        session = (MockHttpSession) mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"" + email + "\",\"userPassword\":\"" + PASSWORD + "\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getRequest().getSession();
    }

    private ResultActions save(String newEmail, String phone) throws Exception {
        return mockMvc.perform(put("/user/updateProfile").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Pro\",\"lastName\":\"File\",\"email\":\"" + newEmail + "\",\"phone\":\""
                        + phone + "\",\"currentPassword\":\"" + PASSWORD + "\"}"));
    }

    private String stored(String column) {
        return jdbcTemplate.queryForObject("SELECT " + column + " FROM users WHERE email = ?", String.class, email);
    }

    @Test
    void aPhoneOutsideEightToFifteenDigitsIsRefusedClearly() throws Exception {
        save(email, "1234567890123456")
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("Invalid phone format"));

        assertThat(stored("phone")).isEqualTo("0400000000");
    }

    @Test
    void aMalformedEmailIsRefused() throws Exception {
        save("not an email", "0400000000")
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("Invalid email format"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = 'not an email'", Integer.class))
                .isZero();
    }

    @Test
    void validChangesAreSaved() throws Exception {
        save(email, "61412345678").andExpect(jsonPath("$.code").value(0));

        assertThat(stored("phone")).isEqualTo("61412345678");
    }

    @Test
    void anEmptyPhoneIsStillAllowed() throws Exception {
        save(email, "").andExpect(jsonPath("$.code").value(0));
    }
}
