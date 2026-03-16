package com.floodrescue.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    // Vietnamese phone number patterns:
    // - Mobile: 03, 05, 07, 08, 09 (10 digits) or 03x, 05x, 07x, 08x, 09x (11 digits)
    // - Landline: 02 (10 digits) or 02x (11 digits)
    // - International format: +84 followed by 9-10 digits (without leading 0)
    private static final String VIETNAM_PHONE_PATTERN = 
        "^(\\+84|0)(3[2-9]|5[6|8|9]|7[0|6-9]|8[1-6|8|9]|9[0-4|6-9])[0-9]{7,8}$";

    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if (phone == null || phone.isBlank()) {
            return false;
        }

        // Remove all whitespace and common separators
        String normalized = phone.replaceAll("[\\s\\-\\(\\)\\.]", "");

        // Check if it matches Vietnamese phone pattern
        return normalized.matches(VIETNAM_PHONE_PATTERN);
    }
}
