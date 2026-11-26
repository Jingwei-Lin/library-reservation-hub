package com.elec5619.libraryreservationreviewhub.service.impl;

import com.elec5619.libraryreservationreviewhub.exception.BusinessException;
import com.elec5619.libraryreservationreviewhub.model.dto.reservation.ReservationAddRequest;
import com.elec5619.libraryreservationreviewhub.model.entity.User;
import com.elec5619.libraryreservationreviewhub.service.ReservationService;
import com.elec5619.libraryreservationreviewhub.service.WaitingListService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs reservations and cancellations at the same time against a real MySQL database, to check that the
 * stock counts stay correct under concurrency. Not @Transactional: each thread must commit for real.
 */
@SpringBootTest
class ReservationConcurrencyIntegrationTest {

    private static final int USERS = 8;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private WaitingListService waitingListService;

    @Autowired
    private JdbcTemplate jdbc;

    private long bookId;
    private final List<User> users = new ArrayList<>();

    @BeforeEach
    void createBookAndUsers() {
        bookId = insert("INSERT INTO book (title, author, genre, quantity, held_quantity, available) "
                + "VALUES ('Concurrency test book', 'Test', 'Fantasy', 0, 0, TRUE)");
        for (int i = 0; i < USERS; i++) {
            long id = insert("INSERT INTO users (email, password, first_name, last_name) VALUES ('race-"
                    + UUID.randomUUID() + "@example.com', 'not-used', 'Race', 'User" + i + "')");
            User user = new User();
            user.setId(id);
            user.setStatus(1);
            users.add(user);
        }
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM reservation WHERE book_id = ?", bookId);
        jdbc.update("DELETE FROM waiting_list WHERE book_id = ?", bookId);
        jdbc.update("DELETE FROM book WHERE book_id = ?", bookId);
        for (User user : users) {
            jdbc.update("DELETE FROM users WHERE user_id = ?", user.getId());
        }
        users.clear();
    }

    @Test
    void onlyOneOfManyConcurrentReservationsGetsTheLastCopy() throws Exception {
        setQuantity(1);

        List<Boolean> outcomes = runAtTheSameTime(users.stream().<Callable<Boolean>>map(user -> () -> {
            try {
                reservationService.createReservation(bookId, request(), user);
                return true;
            } catch (BusinessException e) {
                return false;   // "No available copies"
            }
        }).toList());

        assertThat(outcomes).filteredOn(ok -> ok).hasSize(1);
        assertThat(activeReservations()).isEqualTo(1);
        assertThat(quantity()).isZero();
    }

    @Test
    void concurrentCancellationsReturnEveryCopy() throws Exception {
        setQuantity(3);
        List<Long> reservationIds = new ArrayList<>();
        for (User user : users.subList(0, 3)) {
            reservationIds.add(reservationService.createReservation(bookId, request(), user));
        }
        assertThat(quantity()).isZero();

        List<Callable<Boolean>> cancels = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            long reservationId = reservationIds.get(i);
            User user = users.get(i);
            cancels.add(() -> reservationService.cancelReservation(reservationId, user));
        }
        assertThat(runAtTheSameTime(cancels)).containsOnly(true);

        assertThat(activeReservations()).isZero();
        assertThat(quantity()).isEqualTo(3);
    }

    @Test
    void concurrentReturnsGiveEachCopyToADifferentPersonWaiting() throws Exception {
        setQuantity(3);
        List<Long> reservationIds = new ArrayList<>();
        for (User user : users.subList(0, 3)) {
            reservationIds.add(reservationService.createReservation(bookId, request(), user));
        }
        for (int i = 3; i < 6; i++) {
            jdbc.update("INSERT INTO waiting_list (user_id, book_id, join_date, status) VALUES (?, ?, "
                    + "NOW() - INTERVAL ? MINUTE, 'waiting')", users.get(i).getId(), bookId, 10 - i);
        }

        List<Callable<Boolean>> cancels = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            long reservationId = reservationIds.get(i);
            User user = users.get(i);
            cancels.add(() -> reservationService.cancelReservation(reservationId, user));
        }
        assertThat(runAtTheSameTime(cancels)).containsOnly(true);

        // every copy went to a different person, and held_quantity matches the notified entries
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT user_id) FROM waiting_list WHERE book_id = ? "
                + "AND status = 'notified'", Integer.class, bookId)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT held_quantity FROM book WHERE book_id = ?", Integer.class, bookId))
                .isEqualTo(3);
        assertThat(quantity()).isZero();
    }

    /**
     * A copy coming back while someone joins the queue: either the join comes first and the copy is held for
     * them, or the return comes first and the join is refused (reserve it instead). Never both a shelf copy
     * and someone waiting for it.
     */
    @Test
    void aReturnAndAJoinAtTheSameTimeNeverLeaveSomeoneWaitingNextToAShelfCopy() throws Exception {
        User holder = users.get(0), joiner = users.get(1);
        for (int round = 0; round < 10; round++) {
            jdbc.update("DELETE FROM reservation WHERE book_id = ?", bookId);
            jdbc.update("DELETE FROM waiting_list WHERE book_id = ?", bookId);
            setQuantity(1);
            long reservationId = reservationService.createReservation(bookId, request(), holder);

            runAtTheSameTime(List.<Callable<Boolean>>of(
                    () -> reservationService.cancelReservation(reservationId, holder),
                    () -> {
                        try {
                            waitingListService.join(joiner.getId(), bookId);
                            return true;
                        } catch (BusinessException e) {
                            return false;   // "A copy is available: reserve it instead"
                        }
                    }));

            int waiting = jdbc.queryForObject("SELECT COUNT(*) FROM waiting_list WHERE book_id = ? AND status = 'waiting'",
                    Integer.class, bookId);
            int notified = jdbc.queryForObject("SELECT COUNT(*) FROM waiting_list WHERE book_id = ? AND status = 'notified'",
                    Integer.class, bookId);
            int held = jdbc.queryForObject("SELECT held_quantity FROM book WHERE book_id = ?", Integer.class, bookId);
            assertThat(quantity() > 0 && waiting > 0).as("round %d: a shelf copy and someone waiting", round).isFalse();
            assertThat(held).as("round %d: held = notified", round).isEqualTo(notified);
            assertThat(quantity() + held).as("round %d: the copy is somewhere", round).isEqualTo(1);
        }
    }

    /** Two tabs, or the user and the admin: one cancel wins, and one copy comes back (01-1) */
    @Test
    void cancellingTheSameReservationManyTimesAtOnceGivesBackOneCopy() throws Exception {
        setQuantity(1);
        User user = users.get(0);
        long reservationId = reservationService.createReservation(bookId, request(), user);

        List<Boolean> outcomes = runAtTheSameTime(users.stream().<Callable<Boolean>>map(u -> () -> {
            try {
                return reservationService.cancelReservation(reservationId, user);
            } catch (BusinessException e) {
                return false;   // "This reservation has already changed"
            }
        }).toList());

        assertThat(outcomes).filteredOn(ok -> ok).hasSize(1);
        assertThat(quantity()).isEqualTo(1);
    }

    /** Starts every task together and waits for all of them. */
    private <T> List<T> runAtTheSameTime(List<Callable<T>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static ReservationAddRequest request() {
        ZoneId zone = ZoneId.systemDefault();
        ReservationAddRequest req = new ReservationAddRequest();
        req.setPickupTime(Date.from(LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant()));
        req.setDueTime(Date.from(LocalDate.now().plusDays(14).atStartOfDay(zone).toInstant()));
        return req;
    }

    private long insert(String sql) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    private void setQuantity(int quantity) {
        jdbc.update("UPDATE book SET quantity = ?, held_quantity = 0, available = TRUE WHERE book_id = ?",
                quantity, bookId);
    }

    private int quantity() {
        return jdbc.queryForObject("SELECT quantity FROM book WHERE book_id = ?", Integer.class, bookId);
    }

    private int activeReservations() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM reservation WHERE book_id = ? "
                + "AND status IN ('reserved', 'borrowed')", Integer.class, bookId);
    }
}
