package com.comp5619.libraryreservationreviewhub.service.impl;

import com.comp5619.libraryreservationreviewhub.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Several devices log in to an account that still has the old MD5 hash at the same moment. Each login upgrades the
 * hash, and getLoginUser signs out a session whose hash no longer matches, so only one upgrade may be written and
 * every one of those sessions must stay signed in. Not transactional (the logins run on their own threads), so the
 * account is removed afterwards.
 */
@SpringBootTest
class ConcurrentFirstLoginIntegrationTest {

    private static final String ACCOUNT = "concurrent.first.login@example.com";
    private static final String PASSWORD = "123456789";
    private static final String LEGACY_MD5 = "0fcfcbbb420d3b6f81cd5a0d70a96579";

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void insertAnMd5Account() {
        removeAccount();
        jdbc.update("INSERT INTO users (email, password, first_name, last_name) VALUES (?, ?, 'Many', 'Devices')",
                ACCOUNT, LEGACY_MD5);
    }

    @AfterEach
    void removeAccount() {
        jdbc.update("DELETE FROM users WHERE email = ?", ACCOUNT);
    }

    @Test
    void simultaneousFirstLoginsAllStaySignedIn() throws Exception {
        int devices = 8;
        ExecutorService pool = Executors.newFixedThreadPool(devices);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<MockHttpServletRequest>> logins = new ArrayList<>();
        try {
            for (int i = 0; i < devices; i++) {
                logins.add(pool.submit(() -> {
                    MockHttpServletRequest request = new MockHttpServletRequest();
                    start.await();
                    userService.userLogin(ACCOUNT, PASSWORD, request);
                    return request;
                }));
            }
            start.countDown();
            for (Future<MockHttpServletRequest> login : logins) {
                MockHttpServletRequest request = login.get(60, TimeUnit.SECONDS);
                assertThat(userService.getLoginUser(request).getUserAccount()).isEqualTo(ACCOUNT);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT password FROM users WHERE email = ?", String.class, ACCOUNT))
                .startsWith("$2");
    }
}
