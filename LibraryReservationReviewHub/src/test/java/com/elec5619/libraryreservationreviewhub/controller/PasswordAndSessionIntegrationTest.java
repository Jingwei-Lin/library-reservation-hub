package com.elec5619.libraryreservationreviewhub.controller;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * BCrypt with the MD5 upgrade on login, a new session id at login, the session ended at logout, and the
 * current password for e-mail and password changes. Each test runs in a transaction that is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordAndSessionIntegrationTest {

    private static final String ACCOUNT = "legacy.md5@example.com";
    private static final String PASSWORD = "123456789";
    /** md5("password" + "123456789"): how every account was stored before BCrypt (as the seed accounts are) */
    private static final String LEGACY_MD5 = "0fcfcbbb420d3b6f81cd5a0d70a96579";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void insertAnAccountWithTheOldHash() {
        jdbc.update("INSERT INTO users (email, password, first_name, last_name, phone) VALUES (?, ?, 'Legacy', 'User', '0400000000')",
                ACCOUNT, LEGACY_MD5);
    }

    // ---- hashing (D9) ----

    @Test
    void anMd5AccountLogsInAndItsHashIsReplacedWithBcrypt() throws Exception {
        login(ACCOUNT, PASSWORD).andExpect(jsonPath("$.code").value(0));
        assertThat(storedHash(ACCOUNT)).startsWith("$2");

        // and the same password keeps working on the new hash
        login(ACCOUNT, PASSWORD).andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void aWrongPasswordIsRefusedWithTheSameMessageAndNothingIsUpgraded() throws Exception {
        login(ACCOUNT, "wrong-password").andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("Incorrect e-mail or password"));
        assertThat(storedHash(ACCOUNT)).isEqualTo(LEGACY_MD5);
    }

    @Test
    void registrationStoresBcrypt() throws Exception {
        mockMvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"userAccount\":\"new.member@example.com\",\"userPassword\":\"password123\","
                                + "\"checkPassword\":\"password123\",\"firstName\":\"New\",\"lastName\":\"Member\","
                                + "\"phone\":\"0400000001\"}"))
                .andExpect(jsonPath("$.code").value(0));
        assertThat(storedHash("new.member@example.com")).startsWith("$2");
        login("new.member@example.com", "password123").andExpect(jsonPath("$.code").value(0));
    }

    // ---- sessions (D10) ----

    @Test
    void loginGivesTheSessionANewId() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String before = session.getId();
        login(ACCOUNT, PASSWORD, session).andExpect(jsonPath("$.code").value(0));
        assertThat(session.getId()).isNotEqualTo(before);
        mockMvc.perform(get("/user/get/login").session(session)).andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        MockHttpSession session = loggedIn();
        mockMvc.perform(post("/user/logout").session(session)).andExpect(jsonPath("$.code").value(0));
        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/user/get/login").session(session)).andExpect(jsonPath("$.code").value(40100));
    }

    // ---- current password for profile changes (D11) ----

    @Test
    void changingTheEmailWithoutTheCurrentPasswordIsRefused() throws Exception {
        updateProfile(loggedIn(), "taken.over@example.com", null, null).andExpect(jsonPath("$.code").value(40000));
        updateProfile(loggedIn(), "taken.over@example.com", null, "wrong-password")
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, ACCOUNT)).isOne();
    }

    @Test
    void changingThePasswordWithoutTheCurrentPasswordIsRefused() throws Exception {
        updateProfile(loggedIn(), ACCOUNT, "newPassword123", null).andExpect(jsonPath("$.code").value(40000));
        updateProfile(loggedIn(), ACCOUNT, "newPassword123", "wrong-password").andExpect(jsonPath("$.code").value(40000));
        login(ACCOUNT, PASSWORD).andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void changingTheEmailAndPasswordWithTheCurrentPasswordWorks() throws Exception {
        updateProfile(loggedIn(), "renamed@example.com", "newPassword123", PASSWORD).andExpect(jsonPath("$.code").value(0));
        login("renamed@example.com", "newPassword123").andExpect(jsonPath("$.code").value(0));
        assertThat(storedHash("renamed@example.com")).startsWith("$2");
    }

    @Test
    void changingThePasswordSignsOutOtherSessionsButKeepsThisOne() throws Exception {
        MockHttpSession thisDevice = loggedIn();
        MockHttpSession otherDevice = loggedIn();
        String idBefore = thisDevice.getId();

        updateProfile(thisDevice, ACCOUNT, "newPassword123", PASSWORD).andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/user/get/login").session(otherDevice)).andExpect(jsonPath("$.code").value(40100));
        mockMvc.perform(get("/user/get/login").session(thisDevice)).andExpect(jsonPath("$.code").value(0));
        assertThat(thisDevice.getId()).isNotEqualTo(idBefore);
        // and this session can change it again with the new password
        updateProfile(thisDevice, ACCOUNT, "thirdPassword123", "newPassword123").andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void editsThatKeepThePasswordSignNobodyOut() throws Exception {
        MockHttpSession thisDevice = loggedIn();
        MockHttpSession otherDevice = loggedIn();
        updateProfile(thisDevice, "renamed@example.com", null, PASSWORD).andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/user/get/login").session(otherDevice)).andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/user/get/login").session(thisDevice)).andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void changingOnlyTheLetterCaseOfTheEmailWorks() throws Exception {
        // e-mails compare case-insensitively in MySQL, so the uniqueness check used to find the user's own row
        updateProfile(loggedIn(), "Legacy.MD5@example.com", null, PASSWORD).andExpect(jsonPath("$.code").value(0));
        assertThat(jdbc.queryForObject("SELECT email FROM users WHERE email = ?", String.class, ACCOUNT))
                .isEqualTo("Legacy.MD5@example.com");
    }

    @Test
    void changingOnlyNameOrPhoneStillNeedsNoPassword() throws Exception {
        mockMvc.perform(put("/user/updateProfile").session(loggedIn()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Renamed\",\"lastName\":\"User\",\"email\":\"" + ACCOUNT
                                + "\",\"phone\":\"0411111111\",\"password\":\"\",\"confirmPassword\":\"\"}"))
                .andExpect(jsonPath("$.code").value(0));
        assertThat(jdbc.queryForObject("SELECT first_name FROM users WHERE email = ?", String.class, ACCOUNT))
                .isEqualTo("Renamed");
    }

    private ResultActions updateProfile(MockHttpSession session, String email, String newPassword,
                                        String currentPassword) throws Exception {
        String body = "{\"firstName\":\"Legacy\",\"lastName\":\"User\",\"email\":\"" + email + "\",\"phone\":\"0400000000\""
                + (newPassword == null ? "" : ",\"password\":\"" + newPassword + "\",\"confirmPassword\":\"" + newPassword + "\"")
                + (currentPassword == null ? "" : ",\"currentPassword\":\"" + currentPassword + "\"") + "}";
        return mockMvc.perform(put("/user/updateProfile").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private MockHttpSession loggedIn() throws Exception {
        MockHttpSession session = new MockHttpSession();
        login(ACCOUNT, PASSWORD, session).andExpect(jsonPath("$.code").value(0));
        return session;
    }

    private ResultActions login(String account, String password) throws Exception {
        return login(account, password, new MockHttpSession());
    }

    private ResultActions login(String account, String password, MockHttpSession session) throws Exception {
        return mockMvc.perform(post("/user/login").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userAccount\":\"" + account + "\",\"userPassword\":\"" + password + "\"}"));
    }

    private String storedHash(String account) {
        return jdbc.queryForObject("SELECT password FROM users WHERE email = ?", String.class, account);
    }
}
