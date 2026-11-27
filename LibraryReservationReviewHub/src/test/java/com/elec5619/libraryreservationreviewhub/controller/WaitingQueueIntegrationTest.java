package com.elec5619.libraryreservationreviewhub.controller;

import com.elec5619.libraryreservationreviewhub.model.dto.reservation.ReservationAddRequest;
import com.elec5619.libraryreservationreviewhub.model.entity.User;
import com.elec5619.libraryreservationreviewhub.service.ReservationService;
import com.elec5619.libraryreservationreviewhub.utils.PasswordUtil;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * A copy that comes back goes to the first person waiting, at once (plan D2), and the queue's rules (D8).
 * Runs in a transaction that is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WaitingQueueIntegrationTest {

    private static final String PASSWORD = "123456789";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private JdbcTemplate jdbc;

    private long bookId;

    @BeforeEach
    void createBook() {
        bookId = insert("INSERT INTO book (title, author, genre, quantity, held_quantity, available) "
                + "VALUES ('Waiting queue test book', 'Test', 'Fantasy', 1, 0, TRUE)");
    }

    @Test
    void aReturnedCopyGoesToTheFirstPersonWaitingAtOnce() throws Exception {
        Reader a = newReader(), b = newReader();
        long reservation = reservationService.createReservation(bookId, request(), a.user);
        join(b).andExpect(jsonPath("$.code").value(0));

        reservationService.cancelReservation(reservation, a.user);   // nobody opens a page in between

        assertThat(status(b)).isEqualTo("notified");
        assertThat(jdbc.queryForObject("SELECT notification_date IS NOT NULL FROM waiting_list WHERE user_id = ? "
                + "AND book_id = ?", Boolean.class, b.user.getId(), bookId)).isTrue();
        assertThat(stock()).isEqualTo("1 held, 0 on the shelf");
    }

    @Test
    void theQueueIsFirstComeFirstServedEvenWithinOneSecond() throws Exception {
        Reader a = newReader(), b = newReader(), c = newReader(), d = newReader();
        long reservation = reservationService.createReservation(bookId, request(), a.user);
        // the same second: the order of joining (wait_id) decides
        for (Reader r : new Reader[]{b, c, d}) {
            insert("INSERT INTO waiting_list (user_id, book_id, join_date, status) VALUES (" + r.user.getId() + ", "
                    + bookId + ", '2026-01-01 10:00:00', 'waiting')");
        }
        mockMvc.perform(get("/waiting-list/my/eligibility").session(c.session));   // C looking first changes nothing

        reservationService.cancelReservation(reservation, a.user);

        assertThat(status(b)).isEqualTo("notified");
        assertThat(status(c)).isEqualTo("waiting");
        assertThat(status(d)).isEqualTo("waiting");
    }

    @Test
    void cancellingANotifiedEntryPassesTheCopyToTheNextPerson() throws Exception {
        Reader a = newReader(), b = newReader(), c = newReader();
        long reservation = reservationService.createReservation(bookId, request(), a.user);
        join(b).andExpect(jsonPath("$.code").value(0));
        join(c).andExpect(jsonPath("$.code").value(0));
        reservationService.cancelReservation(reservation, a.user);
        jdbc.update("UPDATE waiting_list SET status = 'notified', is_notified = 1, notification_date = NOW() "
                + "WHERE user_id = ? AND book_id = ?", b.user.getId(), bookId);   // as above, also on the old code

        mockMvc.perform(delete("/waiting-list/my/" + bookId).session(b.session)).andExpect(jsonPath("$.code").value(0));

        assertThat(status(b)).isEqualTo("cancelled");
        assertThat(status(c)).isEqualTo("notified");
        assertThat(stock()).isEqualTo("1 held, 0 on the shelf");
    }

    @Test
    void cancellingANotifiedEntryWithNobodyElseWaitingPutsTheCopyOnTheShelf() throws Exception {
        Reader a = newReader(), b = newReader();
        long reservation = reservationService.createReservation(bookId, request(), a.user);
        join(b).andExpect(jsonPath("$.code").value(0));
        reservationService.cancelReservation(reservation, a.user);
        jdbc.update("UPDATE waiting_list SET status = 'notified', is_notified = 1, notification_date = NOW() "
                + "WHERE user_id = ? AND book_id = ?", b.user.getId(), bookId);

        mockMvc.perform(delete("/waiting-list/my/" + bookId).session(b.session)).andExpect(jsonPath("$.code").value(0));

        assertThat(stock()).isEqualTo("0 held, 1 on the shelf");
    }

    @Test
    void aNotifiedUserReservesTheCopyHeldForThem() throws Exception {
        Reader a = newReader(), b = newReader();
        long reservation = reservationService.createReservation(bookId, request(), a.user);
        join(b).andExpect(jsonPath("$.code").value(0));
        reservationService.cancelReservation(reservation, a.user);
        jdbc.update("UPDATE waiting_list SET status = 'notified', is_notified = 1, notification_date = NOW() "
                + "WHERE user_id = ? AND book_id = ?", b.user.getId(), bookId);

        reservationService.createReservation(bookId, request(), b.user);

        assertThat(status(b)).isEqualTo("fulfilled");
        assertThat(stock()).isEqualTo("0 held, 0 on the shelf");
    }

    @Test
    void joiningTheQueueWhileACopyIsOnTheShelfIsRefused() throws Exception {
        join(newReader()).andExpect(jsonPath("$.code").value(50001));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM waiting_list WHERE book_id = ?", Integer.class, bookId))
                .isZero();
    }

    @Test
    void joiningTheQueueForABookYouHaveReservedIsRefused() throws Exception {
        Reader a = newReader();
        reservationService.createReservation(bookId, request(), a.user);
        join(a).andExpect(jsonPath("$.code").value(50001));
    }

    @Test
    void copiesAnAdminAddsGoToTheQueueFirst() throws Exception {
        Reader a = newReader(), b = newReader(), c = newReader();
        reservationService.createReservation(bookId, request(), a.user);
        join(b).andExpect(jsonPath("$.code").value(0));
        join(c).andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(put("/book/" + bookId).session(login("admin@admin.com"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":3,\"expectedQuantity\":0}"))
                .andExpect(jsonPath("$.code").value(0));

        assertThat(status(b)).isEqualTo("notified");
        assertThat(status(c)).isEqualTo("notified");
        assertThat(stock()).isEqualTo("2 held, 1 on the shelf");
    }

    @Test
    void lookingAtTheQueueChangesNothing() throws Exception {
        Reader b = newReader();
        jdbc.update("UPDATE book SET quantity = 0, held_quantity = 1 WHERE book_id = ?", bookId);
        insert("INSERT INTO waiting_list (user_id, book_id, status) VALUES (" + b.user.getId() + ", " + bookId + ", 'waiting')");

        mockMvc.perform(get("/waiting-list/my/eligibility").session(b.session)).andExpect(jsonPath("$.code").value(0));

        assertThat(status(b)).isEqualTo("waiting");
    }

    // ---- helpers ----

    private record Reader(User user, MockHttpSession session) {}

    private Reader newReader() throws Exception {
        String email = "queue-" + UUID.randomUUID() + "@example.com";
        long id = insert("INSERT INTO users (email, password, first_name, last_name) VALUES ('" + email + "', '"
                + PasswordUtil.encode(PASSWORD) + "', 'Queue', 'Reader')");
        User user = new User();
        user.setId(id);
        user.setStatus(1);
        return new Reader(user, login(email));
    }

    private ResultActions join(Reader reader) throws Exception {
        return mockMvc.perform(post("/waiting-list/my/add").contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":" + bookId + "}").session(reader.session));
    }

    private String status(Reader reader) {
        return jdbc.queryForObject("SELECT status FROM waiting_list WHERE user_id = ? AND book_id = ? "
                + "ORDER BY wait_id DESC LIMIT 1", String.class, reader.user.getId(), bookId);
    }

    private String stock() {
        return jdbc.queryForObject("SELECT CONCAT(held_quantity, ' held, ', quantity, ' on the shelf') FROM book "
                + "WHERE book_id = ?", String.class, bookId);
    }

    private static ReservationAddRequest request() {
        ZoneId zone = ZoneId.systemDefault();
        ReservationAddRequest req = new ReservationAddRequest();
        req.setPickupTime(Date.from(LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant()));
        req.setDueTime(Date.from(LocalDate.now().plusDays(14).atStartOfDay(zone).toInstant()));
        return req;
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
