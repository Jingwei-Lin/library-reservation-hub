package com.elec5619.libraryreservationreviewhub.service.impl;

import com.elec5619.libraryreservationreviewhub.exception.BusinessException;
import com.elec5619.libraryreservationreviewhub.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Registration accepts what people actually type: apostrophes in names, any characters in a password (the
 * queries are parameterised), and phone numbers of 8 to 15 digits, which the column now holds.
 */
@SpringBootTest
@Transactional
class RegistrationInputIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void anApostropheInANameIsAccepted() {
        long id = userService.userRegister("conan.obrien@example.com", "password123", "password123",
                "Conan", "O'Brien", "0400000000");

        assertThat(userService.getById(id).getLastName()).isEqualTo("O'Brien");
    }

    @Test
    void aPasswordWithQuotesAndSqlWordsWorks() {
        String password = "it's--select 1";
        userService.userRegister("quotes@example.com", password, password, "Quote", "Marks", "0400000000");

        assertThat(userService.userLogin("quotes@example.com", password, new MockHttpServletRequest()).getId())
                .isNotNull();
    }

    @Test
    void anElevenDigitPhoneIsStored() {
        long id = userService.userRegister("long.phone@example.com", "password123", "password123",
                "Long", "Phone", "61412345678");

        assertThat(userService.getById(id).getPhone()).isEqualTo("61412345678");
    }

    @Test
    void aPhoneOutsideEightToFifteenDigitsIsStillRefused() {
        assertThatThrownBy(() -> userService.userRegister("short.phone@example.com", "password123", "password123",
                "Short", "Phone", "1234567")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> userService.userRegister("huge.phone@example.com", "password123", "password123",
                "Huge", "Phone", "1234567890123456")).isInstanceOf(BusinessException.class);
    }
}
