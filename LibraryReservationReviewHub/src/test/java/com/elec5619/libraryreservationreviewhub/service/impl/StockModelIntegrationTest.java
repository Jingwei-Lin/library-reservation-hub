package com.elec5619.libraryreservationreviewhub.service.impl;

import com.elec5619.libraryreservationreviewhub.model.dto.reservation.ReservationAddRequest;
import com.elec5619.libraryreservationreviewhub.model.entity.Book;
import com.elec5619.libraryreservationreviewhub.model.entity.User;
import com.elec5619.libraryreservationreviewhub.service.BookService;
import com.elec5619.libraryreservationreviewhub.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The stock model (plan D1): {@code quantity} is the shelf, {@code held_quantity} is the copies set aside for
 * notified users, and {@code available} means "a copy is on the shelf". Runs in a transaction that is rolled back.
 */
@SpringBootTest
@Transactional
class StockModelIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private BookService bookService;

    @Autowired
    private JdbcTemplate jdbc;

    private long bookId;

    @BeforeEach
    void createBook() {
        bookId = insert("INSERT INTO book (title, author, genre, quantity, held_quantity, available) "
                + "VALUES ('Stock model test book', 'Test', 'Fantasy', 2, 0, TRUE)");
    }

    /** The review's scenario (01-2): a held copy must not hide the copy on the shelf. */
    @Test
    void aCopyOnTheShelfCanBeReservedWhileAnotherIsHeld() {
        User a = newUser(), b = newUser(), c = newUser(), d = newUser();
        long reservationA = reservationService.createReservation(bookId, request(), a);
        long reservationC = reservationService.createReservation(bookId, request(), c);
        insert("INSERT INTO waiting_list (user_id, book_id, status) VALUES (" + b.getId() + ", " + bookId + ", 'waiting')");

        reservationService.cancelReservation(reservationA, a);      // A's copy is held for B
        jdbc.update("UPDATE waiting_list SET status = 'notified', is_notified = 1, notification_date = NOW() "
                + "WHERE user_id = ? AND book_id = ?", b.getId(), bookId);
        reservationService.cancelReservation(reservationC, c);      // nobody else waits: C's copy goes on the shelf

        assertThat(stock()).isEqualTo("quantity=1 held=1 available=true");

        reservationService.createReservation(bookId, request(), d);  // D takes the shelf copy
        assertThat(stock()).isEqualTo("quantity=0 held=1 available=false");
    }

    @Test
    void availableFollowsTheShelfCount() {
        User a = newUser(), b = newUser();
        long first = reservationService.createReservation(bookId, request(), a);
        assertThat(stock()).isEqualTo("quantity=1 held=0 available=true");
        reservationService.createReservation(bookId, request(), b);
        assertThat(stock()).isEqualTo("quantity=0 held=0 available=false");
        reservationService.cancelReservation(first, a);
        assertThat(stock()).isEqualTo("quantity=1 held=0 available=true");
    }

    /** The admin page's Available checkbox is read-only now (plan D5): the flag can't be set by hand */
    @Test
    void anAdminEditCannotSetAvailableByHand() {
        User admin = new User();
        admin.setId(3L);
        admin.setIsAdmin(true);

        Book edit = new Book();
        edit.setBookId(bookId);
        edit.setAvailable(false);
        bookService.updateBook(edit, null, admin);
        assertThat(stock()).isEqualTo("quantity=2 held=0 available=true");

        edit.setQuantity(0);
        edit.setAvailable(true);
        bookService.updateBook(edit, 2, admin);
        assertThat(stock()).isEqualTo("quantity=0 held=0 available=false");
    }

    private String stock() {
        return jdbc.queryForObject("SELECT CONCAT('quantity=', quantity, ' held=', held_quantity, ' available=', "
                + "IF(available, 'true', 'false')) FROM book WHERE book_id = ?", String.class, bookId);
    }

    private User newUser() {
        long id = insert("INSERT INTO users (email, password, first_name, last_name) VALUES ('stock-"
                + UUID.randomUUID() + "@example.com', 'not-used', 'Stock', 'User')");
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
}
