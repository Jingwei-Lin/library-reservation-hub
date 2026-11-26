package com.comp5619.libraryreservationreviewhub.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The database itself allows one active reservation and one active waiting-list entry per user and book (plan
 * D11), whatever the code above it does. Finished rows don't count. Runs in a transaction that is rolled back.
 */
@SpringBootTest
@Transactional
class ActiveRowUniquenessIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private long userId;
    private long bookId;

    @BeforeEach
    void setUp() {
        userId = insert("INSERT INTO users (email, password, first_name, last_name) VALUES ('unique-"
                + UUID.randomUUID() + "@example.com', 'not-used', 'Unique', 'Reader')");
        bookId = insert("INSERT INTO book (title, author, genre, quantity, held_quantity, available) "
                + "VALUES ('Uniqueness test book', 'Test', 'Fantasy', 5, 0, TRUE)");
    }

    @Test
    void aSecondActiveReservationForTheSameBookIsRefused() {
        reservation("reserved");
        assertThatThrownBy(() -> reservation("reserved")).isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> reservation("borrowed")).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void aSecondActiveWaitingEntryForTheSameBookIsRefused() {
        waiting("waiting");
        assertThatThrownBy(() -> waiting("waiting")).isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> waiting("notified")).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void finishedRowsDoNotBlockANewOne() {
        reservation("cancelled");
        reservation("returned");
        reservation("cancelled");
        reservation("reserved");
        waiting("cancelled");
        waiting("fulfilled");
        waiting("cancelled");
        waiting("waiting");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reservation WHERE user_id = ? AND book_id = ?",
                Integer.class, userId, bookId)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM waiting_list WHERE user_id = ? AND book_id = ?",
                Integer.class, userId, bookId)).isEqualTo(4);
    }

    @Test
    void otherUsersAndBooksAreNotAffected() {
        long otherBook = insert("INSERT INTO book (title, author, genre, quantity, held_quantity, available) "
                + "VALUES ('Uniqueness test book 2', 'Test', 'Fantasy', 5, 0, TRUE)");
        reservation("reserved");
        jdbc.update("INSERT INTO reservation (user_id, book_id, status) VALUES (?, ?, 'reserved')", userId, otherBook);
        jdbc.update("INSERT INTO reservation (user_id, book_id, status) VALUES (1, ?, 'reserved')", bookId);
    }

    private void reservation(String status) {
        jdbc.update("INSERT INTO reservation (user_id, book_id, reservation_date, status) VALUES (?, ?, NOW(), ?)",
                userId, bookId, status);
    }

    private void waiting(String status) {
        jdbc.update("INSERT INTO waiting_list (user_id, book_id, status) VALUES (?, ?, ?)", userId, bookId, status);
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
