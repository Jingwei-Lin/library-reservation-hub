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
    /**
     * Starts a sign-in attempt, before the password is checked: refuses it with TOO_MANY_REQUESTS while the e-mail
     * has used up its attempts, and otherwise counts it as a failure, in one atomic step. Counted first, guesses
     * sent at the same moment can't all pass the check while the slow password checks run; a successful sign-in
     * then clears the count ({@link #reset}). A refused attempt is not counted.
     */
    public void begin(String email) {
        Instant now = clock.instant();
        Window[] full = {null};
        // compute is atomic per key: of attempts at the same moment, exactly the first MAX_FAILURES are counted
        windows.compute(key(email), (k, w) -> {
            if (w == null || ended(w, now)) {
                return new Window(now, 1);
            }
            if (w.failures() >= MAX_FAILURES) {
                full[0] = w;
                return w;
            }
            return new Window(w.start(), w.failures() + 1);
        });
        if (full[0] != null) {
            long seconds = Duration.between(now, full[0].start().plus(WINDOW)).toSeconds();
            long minutes = Math.max(1, (seconds + 59) / 60);
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "Too many failed sign-in attempts. Try again in "
                    + minutes + (minutes == 1 ? " minute." : " minutes."));
        }
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
