package com.comp5619.libraryreservationreviewhub.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Admin user updates (guards and e-mail), the admin user read, and who may read a reservation.
 * Runs in a transaction that is rolled back, so the seeded accounts are left as they were.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserAdminAndReservationAccessIntegrationTest {

    private static final String PASSWORD = "123456789";
    private static final String USER = "jingwei.lin2@example.com";   // user_id 1
    private static final String OTHER_USER = "hanchen.wang@example.com"; // user_id 2
    private static final String ADMIN = "admin@admin.com";            // user_id 3

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private MockHttpSession admin;

    @BeforeEach
    void logInAdmin() throws Exception {
        admin = login(ADMIN);
    }

    // ---- admin user updates (D4) ----

    @Test
    void anAdminEmailEditIsSaved() throws Exception {
        update("{\"id\":1,\"email\":\"renamed.user@example.com\"}").andExpect(jsonPath("$.code").value(0));
        assertThat(jdbc.queryForObject("SELECT email FROM users WHERE user_id = 1", String.class))
                .isEqualTo("renamed.user@example.com");
    }

    @Test
    void anEmailAlreadyUsedByAnotherAccountIsRefused() throws Exception {
        update("{\"id\":1,\"email\":\"hanchen.wang@example.com\"}").andExpect(jsonPath("$.code").value(40000));
        assertThat(jdbc.queryForObject("SELECT email FROM users WHERE user_id = 1", String.class)).isEqualTo(USER);
    }

    @Test
    void anAdminCannotRemoveTheirOwnAdminRole() throws Exception {
        update("{\"id\":3,\"isAdmin\":false}").andExpect(jsonPath("$.code").value(50001));
        assertThat(jdbc.queryForObject("SELECT is_admin FROM users WHERE user_id = 3", Boolean.class)).isTrue();
    }

    @Test
    void anAdminCannotBanThemselves() throws Exception {
        update("{\"id\":3,\"status\":0}").andExpect(jsonPath("$.code").value(50001));
        assertThat(jdbc.queryForObject("SELECT status FROM users WHERE user_id = 3", Integer.class)).isEqualTo(1);
    }

    @Test
    void anAdminSavingTheirOwnRowUnchangedStillWorks() throws Exception {
        // the admin page always sends isAdmin, also for the admin's own row
        update("{\"id\":3,\"firstName\":\"admin\",\"lastName\":\"admin\",\"email\":\"admin@admin.com\",\"isAdmin\":true}")
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void anUnknownStatusIsRefused() throws Exception {
        update("{\"id\":1,\"status\":5}").andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void anAdminCanStillBanAndPromoteOtherUsers() throws Exception {
        update("{\"id\":1,\"status\":0}").andExpect(jsonPath("$.code").value(0));
        update("{\"id\":2,\"isAdmin\":true}").andExpect(jsonPath("$.code").value(0));
        assertThat(jdbc.queryForObject("SELECT status FROM users WHERE user_id = 1", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT is_admin FROM users WHERE user_id = 2", Boolean.class)).isTrue();
    }

    // ---- admin user read (D5) ----

    @Test
    void theAdminUserReadReturnsTheViewWithStatusOnly() throws Exception {
        mockMvc.perform(get("/user/get").param("id", "1").session(admin))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.userAccount").value(USER))
                .andExpect(jsonPath("$.data.userRole").value("user"))   // only the view has userRole
                .andExpect(jsonPath("$.data.userPassword").doesNotExist())
                .andExpect(jsonPath("$.data.isDelete").doesNotExist());
    }

    // ---- reading a reservation (D6) ----

    @Test
    void onlyTheOwnerOrAnAdminCanReadAReservation() throws Exception {
        long reservationId = insertReservationForUserOne();
        String path = "/reservation/" + reservationId;

        mockMvc.perform(get(path)).andExpect(jsonPath("$.code").value(40100));
        mockMvc.perform(get(path).session(login(OTHER_USER))).andExpect(jsonPath("$.code").value(40101));
        mockMvc.perform(get(path).session(login(USER))).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.reservationId").value(reservationId));
        mockMvc.perform(get(path).session(admin)).andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void aMissingReservationLooksTheSameAsSomeoneElsesToANonAdmin() throws Exception {
        long missing = insertReservationForUserOne() + 1_000_000;
        // the same answer as for another user's reservation, so a user can't tell which ids exist
        mockMvc.perform(get("/reservation/" + missing).session(login(OTHER_USER))).andExpect(jsonPath("$.code").value(40101));
        mockMvc.perform(get("/reservation/" + missing).session(admin)).andExpect(jsonPath("$.code").value(40400));
    }

    private org.springframework.test.web.servlet.ResultActions update(String body) throws Exception {
        return mockMvc.perform(post("/user/update").contentType(MediaType.APPLICATION_JSON).content(body).session(admin));
    }

    private long insertReservationForUserOne() {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("INSERT INTO reservation (user_id, book_id, reservation_date, "
                    + "pickup_date, due_date, status) VALUES (1, 1, NOW(), NOW(), NOW() + INTERVAL 7 DAY, 'reserved')",
                    Statement.RETURN_GENERATED_KEYS);
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    private MockHttpSession login(String account) throws Exception {
        MvcResult result = mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"" + account + "\",\"userPassword\":\"" + PASSWORD + "\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
