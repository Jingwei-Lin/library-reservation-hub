package com.comp5619.libraryreservationreviewhub.utils;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

/**
 * Format checks for registration
 */
public class ValidCheckUtil {

    public static boolean isValidEmail(String email) {
        if (StrUtil.isBlank(email)) return false;
        // a simple e-mail pattern: name@domain.tld
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.matches(emailRegex, email);
    }

    public static boolean isValidPhone(String phone) {
        if (StrUtil.isBlank(phone)) return false;
        // digits only, 8 to 15 of them
        String phoneRegex = "^\\d{8,15}$";
        return Pattern.matches(phoneRegex, phone);
    }


}
