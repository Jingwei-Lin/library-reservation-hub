package com.elec5619.libraryreservationreviewhub.service;

import com.elec5619.libraryreservationreviewhub.exception.BusinessException;
import com.elec5619.libraryreservationreviewhub.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Failed sign-ins per e-mail. After {@link #MAX_FAILURES} failures within {@link #WINDOW} of the first one, every
 * attempt for that e-mail is refused until the window ends; a successful sign-in clears the count. Unknown e-mails
 * are counted the same way, so the answer never tells whether an account exists.
 * <p>
 * Kept in memory: a restart clears it, and each instance of the app counts on its own.
 */
@Component
public class LoginAttempts {

    public static final int MAX_FAILURES = 5;
    public static final Duration WINDOW = Duration.ofMinutes(15);
    /** Above this many e-mails, the ones whose window has ended are dropped, so guessing at many can't fill memory */
    private static final int PRUNE_AT = 10_000;

    private record Window(Instant start, int failures) {}

    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public LoginAttempts() {
        this(Clock.systemUTC());
    }

    LoginAttempts(Clock clock) {
        this.clock = clock;
    }

    /** Throws TOO_MANY_REQUESTS while the e-mail has used up its failed attempts */
    public void checkAllowed(String email) {
        Instant now = clock.instant();
        Window w = windows.get(key(email));
        if (w != null && !ended(w, now) && w.failures() >= MAX_FAILURES) {
            long seconds = Duration.between(now, w.start().plus(WINDOW)).toSeconds();
            long minutes = Math.max(1, (seconds + 59) / 60);
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "Too many failed sign-in attempts. Try again in "
                    + minutes + (minutes == 1 ? " minute." : " minutes."));
        }
    }

    public void recordFailure(String email) {
        Instant now = clock.instant();
        // compute is atomic per key, so failures at the same moment are all counted
        windows.compute(key(email), (k, w) -> w == null || ended(w, now) ? new Window(now, 1)
                : new Window(w.start(), w.failures() + 1));
        if (windows.size() > PRUNE_AT) {
            windows.entrySet().removeIf(e -> ended(e.getValue(), now));
        }
    }

    public void reset(String email) {
        windows.remove(key(email));
    }

    /** The failures counted in the e-mail's current window (for tests) */
    int failures(String email) {
        Window w = windows.get(key(email));
        return w == null || ended(w, clock.instant()) ? 0 : w.failures();
    }

    private static boolean ended(Window w, Instant now) {
        return !now.isBefore(w.start().plus(WINDOW));
    }

    private static String key(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
