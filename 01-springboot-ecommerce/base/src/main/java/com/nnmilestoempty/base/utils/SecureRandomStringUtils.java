package com.nnmilestoempty.base.utils;

import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;

/**
 * Utility class based on RandomStringUtils that generates random ascii strings using {@code SecureRandom}.
 */
public class SecureRandomStringUtils {
    private static final String CHARACTERS =
            "!@#$%^&*()_+{}|:\"<>?-=[]\\;',./1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static SecureRandom secureRandom = new SecureRandom();

    public static String randomAscii(int count) {
        return RandomStringUtils
                .random(count, 0, CHARACTERS.length(), false, false, CHARACTERS.toCharArray(), secureRandom);
    }
}
