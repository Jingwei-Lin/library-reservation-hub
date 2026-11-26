package com.comp5619.libraryreservationreviewhub.job;

import com.comp5619.libraryreservationreviewhub.utils.PasswordUtil;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * A copy is held for a notified user for 3 days from the notification (plan D3); then it passes on.
 * Runs in a transaction that is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HoldExpiryIntegrationTest {

    private static final String PASSWORD = "123456789";

    @Autowired
    private WaitingListJanitor janitor;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long bookId;

    @BeforeEach
    void createBook() {
        // one copy, held for whoever is notified below
        bookId = insert("INSERT INTO book (title, author, genre, quantity, held_quantity, available) "
                + "VALUES ('Hold expiry test book', 'Test', 'Fantasy', 0, 1, FALSE)");
    }

    @Test
    void aHoldLastsThreeDaysFromTheNotificationNotFromJoining() {
        long b = newUser();
        entry(b, "10 DAY", "1 HOUR", "notified");   // waited long, notified an hour ago

        janitor.cancelOverdue();

        assertThat(status(b)).isEqualTo("notified");
        assertThat(stock()).isEqualTo("1 held, 0 on the shelf");
    }

    @Test
    void anExpiredHoldPassesToTheNextPersonWaiting() {
        long b = newUser(), c = newUser();
        entry(b, "5 DAY", "4 DAY", "notified");
        entry(c, "4 DAY", null, "waiting");

        janitor.cancelOverdue();

        assertThat(status(b)).isEqualTo("cancelled");
        assertThat(status(c)).isEqualTo("notified");
        assertThat(stock()).isEqualTo("1 held, 0 on the shelf");
    }

    @Test
    void anExpiredHoldWithNobodyWaitingGoesBackOnTheShelf() {
        long b = newUser();
        entry(b, "8 DAY", "4 DAY", "notified");

        janitor.cancelOverdue();

        assertThat(status(b)).isEqualTo("cancelled");
        assertThat(stock()).isEqualTo("0 held, 1 on the shelf");
    }

    @Test
    void waitingEntriesNeverRunOut() {
        jdbc.update("UPDATE book SET held_quantity = 0 WHERE book_id = ?", bookId);
        long b = newUser();
        entry(b, "30 DAY", null, "waiting");

        janitor.cancelOverdue();

        assertThat(status(b)).isEqualTo("waiting");
    }

    @Test
    void theWaitingListShowsWhenAHoldEnds() throws Exception {
        String email = "hold-" + UUID.randomUUID() + "@example.com";
        long b = newUser(email);
        insert("INSERT INTO waiting_list (user_id, book_id, join_date, notification_date, is_notified, status) VALUES ("
                + b + ", " + bookId + ", '2026-11-20 09:00:00', '2026-11-24 10:30:00', 1, 'notified')");

        mockMvc.perform(get("/waiting-list/my").session(login(email)))
                .andExpect(jsonPath("$.data[0].notificationDate").value("2026-11-24 10:30:00"))
                .andExpect(jsonPath("$.data[0].holdExpiresAt").value("2026-11-27 10:30:00"));
    }

    // ---- helpers ----

    /** A waiting-list entry that joined {@code joinedAgo} and, if given, was notified {@code notifiedAgo} */
    private void entry(long userId, String joinedAgo, String notifiedAgo, String status) {
        jdbc.update("INSERT INTO waiting_list (user_id, book_id, join_date, notification_date, is_notified, status) "
                + "VALUES (?, ?, NOW() - INTERVAL " + joinedAgo + ", "
                + (notifiedAgo == null ? "NULL" : "NOW() - INTERVAL " + notifiedAgo) + ", ?, ?)",
                userId, bookId, notifiedAgo != null, status);
    }

    private String status(long userId) {
        return jdbc.queryForObject("SELECT status FROM waiting_list WHERE user_id = ? AND book_id = ?",
                String.class, userId, bookId);
    }

    private String stock() {
        return jdbc.queryForObject("SELECT CONCAT(held_quantity, ' held, ', quantity, ' on the shelf') FROM book "
                + "WHERE book_id = ?", String.class, bookId);
    }

    private long newUser() {
        return newUser("hold-" + UUID.randomUUID() + "@example.com");
    }

    private long newUser(String email) {
        return insert("INSERT INTO users (email, password, first_name, last_name) VALUES ('" + email + "', '"
                + PasswordUtil.encode(PASSWORD) + "', 'Hold', 'Reader')");
    }

    private long insert(String sql) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
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
