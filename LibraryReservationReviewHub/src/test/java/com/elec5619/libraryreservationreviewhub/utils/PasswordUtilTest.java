package com.elec5619.libraryreservationreviewhub.utils;

import com.elec5619.libraryreservationreviewhub.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordUtilTest {

    /** md5("password" + "123456789"), the hash of the seed accounts in Schema.sql */
    private static final String SEED_MD5 = "0fcfcbbb420d3b6f81cd5a0d70a96579";

    @Test
    void newHashesAreSaltedBcryptAndMatchTheirPassword() {
        String a = PasswordUtil.encode("password123");
        String b = PasswordUtil.encode("password123");
        assertThat(a).startsWith("$2").isNotEqualTo(b);
        assertThat(PasswordUtil.matches("password123", a)).isTrue();
        assertThat(PasswordUtil.matches("password124", a)).isFalse();
        assertThat(PasswordUtil.needsUpgrade("password123", a)).isFalse();
    }

    @Test
    void theLegacyMd5HashStillMatchesAndIsMarkedForUpgrade() {
        assertThat(PasswordUtil.matches("123456789", SEED_MD5)).isTrue();
        assertThat(PasswordUtil.matches("12345678", SEED_MD5)).isFalse();
        assertThat(PasswordUtil.needsUpgrade("123456789", SEED_MD5)).isTrue();
    }

    @Test
    void aPasswordLongerThanBcryptCanUseIsRefusedNotCut() {
        String seventyTwo = "a".repeat(72);
        assertThat(PasswordUtil.matches(seventyTwo, PasswordUtil.encode(seventyTwo))).isTrue();
        assertThatThrownBy(() -> PasswordUtil.encode(seventyTwo + "b"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("72");
        // BCrypt would ignore everything after byte 72; matches must not let a longer password in
        assertThat(PasswordUtil.matches(seventyTwo + "b", PasswordUtil.encode(seventyTwo))).isFalse();
    }

    @Test
    void nullsNeverMatch() {
        assertThat(PasswordUtil.matches(null, SEED_MD5)).isFalse();
        assertThat(PasswordUtil.matches("123456789", null)).isFalse();
    }
}
