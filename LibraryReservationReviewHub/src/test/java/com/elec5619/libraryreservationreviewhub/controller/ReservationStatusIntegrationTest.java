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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Reservation status changes (plan D4, D9, D10): users cancel only reserved reservations, admins make only the
 * changes the library has, and stock follows every change. Runs in a transaction that is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private JdbcTemplate jdbc;

    private User admin;
    private User reader;
    private long bookId;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(3L);
        admin.setIsAdmin(true);
        admin.setStatus(1);
        long id = insert("INSERT INTO users (email, password, first_name, last_name) VALUES ('status-"
                + UUID.randomUUID() + "@example.com', 'not-used', 'Status', 'Reader')");
        reader = new User();
        reader.setId(id);
        reader.setStatus(1);
        bookId = insert("INSERT INTO book (title, author, genre, quantity, held_quantity, available) "
                + "VALUES ('Status test book', 'Test', 'Fantasy', 1, 0, TRUE)");
    }

    @Test
    void aUserCannotCancelABorrowedReservation() {
        long id = reservationService.createReservation(bookId, request(), reader);
        reservationService.updateReservationStatus(id, "borrowed", admin);

        assertThatThrownBy(() -> reservationService.cancelReservation(id, reader)).isInstanceOf(BusinessException.class);
        assertThat(status(id)).isEqualTo("borrowed");
        assertThat(shelf()).isZero();   // the copy is still at the reader's home
    }

    @Test
    void anAdminCancellingAReservationGivesTheCopyBack() {
        long id = reservationService.createReservation(bookId, request(), reader);
        reservationService.updateReservationStatus(id, "cancelled", admin);

        assertThat(status(id)).isEqualTo("cancelled");
        assertThat(shelf()).isEqualTo(1);
    }

    @Test
    void overdueIsRefusedAsABadRequestNotAServerError() throws Exception {
        long id = reservationService.createReservation(bookId, request(), reader);
        mockMvc.perform(put("/reservation/status/" + id).param("status", "overdue").session(adminSession()))
                .andExpect(jsonPath("$.code").value(40000));
        assertThat(status(id)).isEqualTo("reserved");
    }

    @Test
    void changesTheLibraryDoesNotHaveAreRefused() {
        long id = reservationService.createReservation(bookId, request(), reader);
        reservationService.updateReservationStatus(id, "borrowed", admin);
        reservationService.updateReservationStatus(id, "returned", admin);
        assertThat(shelf()).isEqualTo(1);

        // a returned copy can't be "borrowed" again without taking a copy
        assertThatThrownBy(() -> reservationService.updateReservationStatus(id, "reserved", admin))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> reservationService.updateReservationStatus(id, "borrowed", admin))
                .isInstanceOf(BusinessException.class);
        assertThat(status(id)).isEqualTo("returned");
        assertThat(shelf()).isEqualTo(1);
    }

    @Test
    void aReturnIsDated() {
        long id = reservationService.createReservation(bookId, request(), reader);
        reservationService.updateReservationStatus(id, "borrowed", admin);
        reservationService.updateReservationStatus(id, "returned", admin);
        assertThat(jdbc.queryForObject("SELECT return_date IS NOT NULL FROM reservation WHERE reservation_id = ?",
                Boolean.class, id)).isTrue();
    }

    /** The admin page's Confirm Borrow and Confirm Return, and the user's own dates are kept on borrow */
    @Test
    void confirmBorrowAndConfirmReturnWorkAsBefore() {
        long id = reservationService.createReservation(bookId, request(), reader);
        String datesBefore = dates(id);

        assertThat(reservationService.updateReservationStatus(id, "borrowed", admin)).isTrue();
        assertThat(status(id)).isEqualTo("borrowed");
        assertThat(dates(id)).isEqualTo(datesBefore);
        assertThat(shelf()).isZero();

        assertThat(reservationService.updateReservationStatus(id, "returned", admin)).isTrue();
        assertThat(status(id)).isEqualTo("returned");
        assertThat(shelf()).isEqualTo(1);
    }

    @Test
    void aMissingUserIsRefusedNotANullPointer() {
        assertThatThrownBy(() -> reservationService.createReservation(bookId, request(), null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> reservationService.cancelReservation(1L, null))
                .isInstanceOf(BusinessException.class);
    }

    // ---- helpers ----

    private String status(long reservationId) {
        return jdbc.queryForObject("SELECT status FROM reservation WHERE reservation_id = ?", String.class, reservationId);
    }

    private String dates(long reservationId) {
        return jdbc.queryForObject("SELECT CONCAT(pickup_date, ' / ', due_date) FROM reservation WHERE reservation_id = ?",
                String.class, reservationId);
    }

    private int shelf() {
        return jdbc.queryForObject("SELECT quantity FROM book WHERE book_id = ?", Integer.class, bookId);
    }

    private MockHttpSession adminSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"admin@admin.com\",\"userPassword\":\"123456789\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
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
}
