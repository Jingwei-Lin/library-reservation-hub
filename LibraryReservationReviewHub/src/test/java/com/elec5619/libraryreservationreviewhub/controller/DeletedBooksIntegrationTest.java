package com.elec5619.libraryreservationreviewhub.controller;

import com.elec5619.libraryreservationreviewhub.exception.BusinessException;
import com.elec5619.libraryreservationreviewhub.model.dto.reservation.ReservationAddRequest;
import com.elec5619.libraryreservationreviewhub.model.entity.User;
import com.elec5619.libraryreservationreviewhub.service.ReservationService;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * A deleted book has its own flag (plan D6): stock changes never bring it back, users don't see it, admins do.
 * Runs in a transaction that is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeletedBooksIntegrationTest {

    private static final String PASSWORD = "123456789";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private JdbcTemplate jdbc;

    private MockHttpSession admin;
    private long bookId;

    @BeforeEach
    void setUp() throws Exception {
        admin = login("admin@admin.com");
        bookId = insert("INSERT INTO book (title, author, genre, quantity, held_quantity, available) "
                + "VALUES ('Deleted books test', 'Test', 'Deletion Genre', 2, 0, TRUE)");
    }

    @Test
    void aDeletedBookStaysDeletedWhenACopyComesBack() throws Exception {
        User reader = newUser();
        long reservationId = reservationService.createReservation(bookId, request(), reader);
        deleteBook().andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data").value(true));

        reservationService.cancelReservation(reservationId, reader);   // the copy comes back

        assertThatThrownBy(() -> reservationService.createReservation(bookId, request(), newUser()))
                .isInstanceOf(BusinessException.class);
        mockMvc.perform(get("/book/" + bookId)).andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void aBookWithNoCopiesLeftCanBeDeleted() throws Exception {
        jdbc.update("UPDATE book SET quantity = 0 WHERE book_id = ?", bookId);
        deleteBook().andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data").value(true));
        assertThat(jdbc.queryForObject("SELECT is_delete FROM book WHERE book_id = ?", Boolean.class, bookId)).isTrue();
    }

    @Test
    void usersNoLongerSeeADeletedBookButAdminsDo() throws Exception {
        deleteBook().andExpect(jsonPath("$.code").value(0));
        int id = (int) bookId;

        mockMvc.perform(get("/book/search").param("keyword", "Deleted books test"))
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/book/genre/Deletion Genre")).andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(get("/book/" + bookId).session(login("jingwei.lin2@example.com")))
                .andExpect(jsonPath("$.code").value(40400));

        // a user asking for deleted books doesn't get them; the admin page does
        mockMvc.perform(get("/book/search").param("keyword", "Deleted books test").param("includeDeleted", "true")
                        .session(login("jingwei.lin2@example.com")))
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/book/search").param("keyword", "Deleted books test").param("includeDeleted", "true")
                        .session(admin))
                .andExpect(jsonPath("$.data.records[0].bookId").value(id))
                .andExpect(jsonPath("$.data.records[0].deleted").value(true));
        mockMvc.perform(get("/book/" + bookId).session(admin))
                .andExpect(jsonPath("$.data.deleted").value(true));
    }

    @Test
    void aDeletedBookLeavesTheTopTen() throws Exception {
        // book 5 has the best rating in the seed data
        mockMvc.perform(get("/book-rating/top10-books")).andExpect(jsonPath("$.data[*].bookId", hasItem(5)));
        mockMvc.perform(delete("/book/5").session(admin)).andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/book-rating/top10-books")).andExpect(jsonPath("$.data[*].bookId", not(hasItem(5))));
        mockMvc.perform(get("/book-rating/top10")).andExpect(jsonPath("$.data[*].bookId", not(hasItem(5))));
    }

    @Test
    void deletingABookEndsItsQueueAndPutsTheHeldCopyBack() throws Exception {
        User waiting = newUser(), notified = newUser();
        jdbc.update("UPDATE book SET quantity = 0, held_quantity = 1 WHERE book_id = ?", bookId);
        insert("INSERT INTO waiting_list (user_id, book_id, status) VALUES (" + waiting.getId() + ", " + bookId + ", 'waiting')");
        insert("INSERT INTO waiting_list (user_id, book_id, status, is_notified, notification_date) VALUES ("
                + notified.getId() + ", " + bookId + ", 'notified', 1, NOW())");

        deleteBook().andExpect(jsonPath("$.code").value(0));

        assertThat(jdbc.queryForObject("SELECT CONCAT(quantity, '/', held_quantity) FROM book WHERE book_id = ?",
                String.class, bookId)).isEqualTo("1/0");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM waiting_list WHERE book_id = ? AND status IN "
                + "('waiting', 'notified')", Integer.class, bookId)).isZero();
    }

    @Test
    void joiningTheQueueForADeletedBookIsRefused() throws Exception {
        jdbc.update("UPDATE book SET quantity = 0 WHERE book_id = ?", bookId);
        deleteBook().andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/waiting-list/my/add").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":" + bookId + "}").session(login("jingwei.lin2@example.com")))
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void aRestoredBookIsBackForUsers() throws Exception {
        deleteBook().andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(put("/book/" + bookId + "/restore").session(admin)).andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/book/" + bookId)).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.available").value(true));
        reservationService.createReservation(bookId, request(), newUser());
    }

    private org.springframework.test.web.servlet.ResultActions deleteBook() throws Exception {
        return mockMvc.perform(delete("/book/" + bookId).session(admin));
    }

    private User newUser() {
        long id = insert("INSERT INTO users (email, password, first_name, last_name) VALUES ('deleted-"
                + UUID.randomUUID() + "@example.com', 'not-used', 'Deleted', 'Test')");
        User user = new User();
        user.setId(id);
        user.setStatus(1);
        return user;
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
