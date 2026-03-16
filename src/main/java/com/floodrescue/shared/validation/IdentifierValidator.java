package com.floodrescue.shared.validation;

import com.floodrescue.shared.util.PhoneUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class IdentifierValidator implements ConstraintValidator<ValidIdentifier, String> {

    // Basic email pattern
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    @Override
    public void initialize(ValidIdentifier constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String identifier, ConstraintValidatorContext context) {
        // Return true for null values - let @NotBlank handle null/blank validation
        if (identifier == null || identifier.isBlank()) {
            return true;
        }

        String trimmed = identifier.trim();

        // Check if it's a valid email
        if (EMAIL_PATTERN.matcher(trimmed).matches()) {
            return true;
        }

        // Check if it's a valid phone number
        return PhoneUtil.isValid(trimmed);
    }
}
