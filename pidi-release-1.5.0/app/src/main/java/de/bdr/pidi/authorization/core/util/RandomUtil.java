/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.util.Random;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RandomUtil {
    private static final int RANDOM_CHARS_ARRAY_LENGTH = 22;
    private static final String DEFAULT_RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final char[] DEFAULT_RANDOM_CHARS_ARRAY = DEFAULT_RANDOM_CHARS.toCharArray();
    private static final Pattern DEFAULT_RANDOM_CHARS_PATTERN = Pattern.compile("[" + DEFAULT_RANDOM_CHARS + "]{"+ RANDOM_CHARS_ARRAY_LENGTH + "}");

    private static final Random SECURE_RANDOM = new SecureRandom();

    public static String randomString() {
        var result = new char[RANDOM_CHARS_ARRAY_LENGTH];
        for (int i = 0; i < RANDOM_CHARS_ARRAY_LENGTH; i++) {
            result[i] = DEFAULT_RANDOM_CHARS_ARRAY[SECURE_RANDOM.nextInt(DEFAULT_RANDOM_CHARS_ARRAY.length)];
        }
        return new String(result);
    }

    public static boolean isValid(String randomChars) {
        return randomChars != null && DEFAULT_RANDOM_CHARS_PATTERN.matcher(randomChars).matches();
    }
}
