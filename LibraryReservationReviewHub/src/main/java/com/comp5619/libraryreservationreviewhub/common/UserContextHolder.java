package com.comp5619.libraryreservationreviewhub.common;

import com.comp5619.libraryreservationreviewhub.model.entity.User;
import lombok.extern.slf4j.Slf4j;

/**
 * Holds the current request's user in a ThreadLocal,
 * because the HttpServletRequest is gone on the asynchronous SSE threads
 */
@Slf4j
public class UserContextHolder {

    private static final ThreadLocal<User> USER_CONTEXT = new ThreadLocal<>();

    public static void set(User user) {
        USER_CONTEXT.set(user);

    }

    public static User get() {

        return USER_CONTEXT.get();
    }

    // clear it afterwards, or a pooled thread keeps the user (a memory leak)
    public static void clear() {
        USER_CONTEXT.remove();
    }
}
