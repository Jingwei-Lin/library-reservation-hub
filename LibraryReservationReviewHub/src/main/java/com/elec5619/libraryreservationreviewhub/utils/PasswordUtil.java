package com.elec5619.libraryreservationreviewhub.utils;

import com.elec5619.libraryreservationreviewhub.exception.ErrorCode;
import com.elec5619.libraryreservationreviewhub.exception.ThrowUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * Password hashing. New hashes are BCrypt. Accounts created before the switch (including the seed accounts in
 * Schema.sql) still hold the old salted-MD5 hash; {@link #matches} accepts it, and login then replaces it with a
 * BCrypt hash ({@link #needsUpgrade}).
 */
public final class PasswordUtil {

    /** BCrypt only uses the first 72 bytes of a password, so longer ones are refused rather than cut silently. */
    public static final int MAX_BYTES = 72;

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();
    private static final String LEGACY_MD5_SALT = "password";

    private PasswordUtil() {
    }

    /** A new BCrypt hash; PARAMS_ERROR if the password is longer than {@value #MAX_BYTES} bytes. */
    public static String encode(String rawPassword) {
        ThrowUtils.throwIf(!fitsBcrypt(rawPassword), ErrorCode.PARAMS_ERROR,
                "Password must be at most " + MAX_BYTES + " bytes");
        return BCRYPT.encode(rawPassword);
    }

    /** Whether {@code rawPassword} matches a stored BCrypt hash or a legacy MD5 hash. */
    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        if (isBcrypt(storedHash)) {
            // a password over the limit was never accepted by encode, so it can't be the stored one
            return fitsBcrypt(rawPassword) && BCRYPT.matches(rawPassword, storedHash);
        }
        return storedHash.equals(legacyMd5(rawPassword));
    }

    /** Whether a stored hash should be replaced with BCrypt after {@code rawPassword} matched it. */
    public static boolean needsUpgrade(String rawPassword, String storedHash) {
        return !isBcrypt(storedHash) && fitsBcrypt(rawPassword);
    }

    private static boolean isBcrypt(String storedHash) {
        return storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$");
    }

    private static boolean fitsBcrypt(String rawPassword) {
        return rawPassword.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }

    private static String legacyMd5(String rawPassword) {
        return DigestUtils.md5DigestAsHex((LEGACY_MD5_SALT + rawPassword).getBytes());
    }
}
