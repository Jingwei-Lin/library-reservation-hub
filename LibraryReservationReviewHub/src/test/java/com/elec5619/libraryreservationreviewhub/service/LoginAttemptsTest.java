package com.elec5619.libraryreservationreviewhub.service;

import com.elec5619.libraryreservationreviewhub.exception.BusinessException;
import com.elec5619.libraryreservationreviewhub.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptsTest {

    /** A clock the test moves by hand */
    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-11-26T09:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final TestClock clock = new TestClock();
    private final LoginAttempts attempts = new LoginAttempts(clock);

    /** Attempts that are counted (each began and then failed) */
    private void fail(String email, int times) {
        for (int i = 0; i < times; i++) {
            attempts.begin(email);
        }
    }

    @Test
    void aFifthAttemptIsStillAllowed() {
        fail("a@example.com", 4);

        assertThatCode(() -> attempts.begin("a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void afterFiveFailuresAttemptsAreRefusedUntilTheWindowEnds() {
        fail("a@example.com", 5);

        assertThatThrownBy(() -> attempts.begin("a@example.com"))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.TOO_MANY_REQUESTS.getCode());
                    assertThat(e.getMessage()).isEqualTo("Too many failed sign-in attempts. Try again in 15 minutes.");
                });
        // a refused attempt is not counted
        assertThat(attempts.failures("a@example.com")).isEqualTo(5);

        clock.advance(Duration.ofMinutes(14).plusSeconds(30));
        assertThatThrownBy(() -> attempts.begin("a@example.com")).hasMessageContaining("in 1 minute.");

        clock.advance(Duration.ofSeconds(30));
        assertThatCode(() -> attempts.begin("a@example.com")).doesNotThrowAnyException();
        // that attempt started a new window
        assertThat(attempts.failures("a@example.com")).isEqualTo(1);
    }

    @Test
    void theWindowStartsAtTheFirstFailure() {
        fail("a@example.com", 4);
        clock.advance(Duration.ofMinutes(16));

        // the old failures are over; this one starts a new window
        fail("a@example.com", 1);

        assertThat(attempts.failures("a@example.com")).isEqualTo(1);
    }

    @Test
    void aResetClearsTheCountAndOtherEmailsAreSeparate() {
        fail("a@example.com", 5);
        fail("b@example.com", 2);

        attempts.reset("a@example.com");

        assertThat(attempts.failures("a@example.com")).isZero();
        assertThat(attempts.failures("b@example.com")).isEqualTo(2);
        assertThatCode(() -> attempts.begin("a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void letterCaseAndSpacesDoNotMakeANewEmail() {
        fail("A@Example.com", 3);
        fail(" a@example.COM ", 2);

        assertThat(attempts.failures("a@example.com")).isEqualTo(5);
    }

    @Test
    void ofAttemptsAtTheSameMomentOnlyFiveAreCounted() throws Exception {
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger refused = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                start.await();
                try {
                    attempts.begin("a@example.com");
                } catch (BusinessException e) {
                    refused.incrementAndGet();
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(attempts.failures("a@example.com")).isEqualTo(LoginAttempts.MAX_FAILURES);
        assertThat(refused.get()).isEqualTo(threads - LoginAttempts.MAX_FAILURES);
    }
}
