package com.floodrescue.shared.util;

import java.util.regex.Pattern;

/**
 * Utility class for phone number normalization and validation
 */
public class PhoneUtil {

    // Vietnamese phone number pattern
    private static final Pattern VIETNAM_PHONE_PATTERN = 
        Pattern.compile("^(\\+84|0)(3[2-9]|5[6|8|9]|7[0|6-9]|8[1-6|8|9]|9[0-4|6-9])[0-9]{7,8}$");

    /**
     * Normalizes a phone number to a standard format (0XXXXXXXXX)
     * Removes all whitespace, separators, and converts +84 to 0
     * 
     * @param phone Raw phone number input
     * @return Normalized phone number (10-11 digits starting with 0) or null if invalid
     */
    public static String normalize(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        // Remove all whitespace and common separators
        String cleaned = phone.replaceAll("[\\s\\-\\(\\)\\.]", "");

        // Convert +84 to 0 (Vietnamese international format)
        if (cleaned.startsWith("+84")) {
            cleaned = "0" + cleaned.substring(3);
        }

        // Remove leading zeros if more than one (but keep at least one)
        while (cleaned.length() > 1 && cleaned.startsWith("00")) {
            cleaned = cleaned.substring(1);
        }

        // Validate the normalized phone number
        if (!VIETNAM_PHONE_PATTERN.matcher(cleaned).matches()) {
            return null;
        }

        return cleaned;
    }

    /**
     * Validates if a phone number is a valid Vietnamese phone number
     * 
     * @param phone Phone number to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        return normalize(phone) != null;
    }

    /**
     * Sanitizes phone number for database storage
     * Ensures consistent format and prevents SQL injection risks
     * 
     * @param phone Raw phone number
     * @return Sanitized phone number or null if invalid
     */
    public static String sanitize(String phone) {
        return normalize(phone);
    }
}
