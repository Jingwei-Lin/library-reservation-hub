package com.elec5619.libraryreservationreviewhub.utils;

import java.util.regex.Pattern;

/**
 * Refuses input that looks like SQL injection
 */
public class SqlInjectionUtil {

    // only SQL keywords and comment markers, so an address like update@example.com still passes
    private static final Pattern SQL_PATTERN = Pattern.compile(
            "(?:')|(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|" +
                    "\\b(select|insert|delete|drop|truncate|exec|union|alter|create|shutdown)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Throws if the input looks like SQL injection
     */
    public static void checkSqlInjection(String input, String fieldName) {
        if (input == null) return;
        if (SQL_PATTERN.matcher(input).find()) {
            throw new RuntimeException("Illegal input detected in field: " + fieldName);
        }
    }


}
