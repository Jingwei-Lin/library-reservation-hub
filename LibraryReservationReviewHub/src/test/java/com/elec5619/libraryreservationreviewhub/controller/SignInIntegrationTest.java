package com.elec5619.libraryreservationreviewhub.controller;

import com.elec5619.libraryreservationreviewhub.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Sign-in gives one answer for an unknown e-mail and a wrong password, and refuses an e-mail after 5 failed
 * attempts in 15 minutes. Each test uses its own e-mails: the attempt counts last for the whole run.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SignInIntegrationTest {

    private static final String PASSWORD = "right-password-1";
    private static final String REFUSED = "Incorrect e-mail or password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    private String newAccount() {
        String email = "signin-" + UUID.randomUUID() + "@example.com";
        userService.userRegister(email, PASSWORD, PASSWORD, "Sign", "In", "0400000000");
        return email;
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userAccount\":\"" + email + "\",\"userPassword\":\"" + password + "\"}"));
    }

    @Test
    void anUnknownEmailAndAWrongPasswordGetTheSameAnswer() throws Exception {
        String email = newAccount();

        login("nobody-" + UUID.randomUUID() + "@example.com", PASSWORD)
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value(REFUSED));
        login(email, "wrong-password")
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value(REFUSED));
    }

    @Test
    void aSixthAttemptIsRefusedEvenWithTheRightPassword() throws Exception {
        String email = newAccount();
        String other = newAccount();

        for (int i = 0; i < 5; i++) {
            login(email, "wrong-password").andExpect(jsonPath("$.code").value(40000));
        }

        login(email, PASSWORD).andExpect(jsonPath("$.code").value(42900));
        // the limit is per e-mail
        login(other, PASSWORD).andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void anUnknownEmailIsLimitedLikeAKnownOne() throws Exception {
        String unknown = "nobody-" + UUID.randomUUID() + "@example.com";

        for (int i = 0; i < 5; i++) {
            login(unknown, "wrong-password").andExpect(jsonPath("$.code").value(40000));
        }

        login(unknown, "wrong-password").andExpect(jsonPath("$.code").value(42900));
    }

    @Test
    void aSuccessfulSignInStartsTheCountAgain() throws Exception {
        String email = newAccount();

        for (int i = 0; i < 4; i++) {
            login(email, "wrong-password").andExpect(jsonPath("$.code").value(40000));
        }
        login(email, PASSWORD).andExpect(jsonPath("$.code").value(0));
        for (int i = 0; i < 4; i++) {
            login(email, "wrong-password").andExpect(jsonPath("$.code").value(40000));
        }

        login(email, PASSWORD).andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void theLimitIgnoresLetterCase() throws Exception {
        String email = newAccount();

        for (int i = 0; i < 5; i++) {
            login(i % 2 == 0 ? email.toUpperCase() : email, "wrong-password");
        }

        login(email, PASSWORD).andExpect(jsonPath("$.code").value(42900));
    }
}
