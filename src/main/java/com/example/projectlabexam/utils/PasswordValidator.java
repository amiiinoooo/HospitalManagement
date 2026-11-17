package com.example.projectlabexam.utils;

import java.util.regex.Pattern;

public class PasswordValidator {
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 32;
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern NUMBERS = Pattern.compile("\\d");
    private static final Pattern CONSECUTIVE_CHARS = Pattern.compile("(.)\\1{2,}");
    private static final Pattern COMMON_PATTERNS = Pattern.compile(
        "(?i)(password|12345|qwerty|admin|letmein|welcome|abc123|monkey|dragon|football|baseball|123123|shadow)"
    );
    
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Password cannot be empty");
        }
        
        if (password.length() < MIN_LENGTH) {
            return new ValidationResult(false, "Password must be at least " + MIN_LENGTH + " characters long");
        }
        
        if (password.length() > MAX_LENGTH) {
            return new ValidationResult(false, "Password cannot exceed " + MAX_LENGTH + " characters");
        }
        
        if (!UPPERCASE.matcher(password).find()) {
            return new ValidationResult(false, "Password must contain at least one uppercase letter");
        }
        
        if (!LOWERCASE.matcher(password).find()) {
            return new ValidationResult(false, "Password must contain at least one lowercase letter");
        }
        
        if (!NUMBERS.matcher(password).find()) {
            return new ValidationResult(false, "Password must contain at least one number");
        }
        
        if (!SPECIAL_CHARS.matcher(password).find()) {
            return new ValidationResult(false, "Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;:'\",.<>/?))");
        }
        
        if (CONSECUTIVE_CHARS.matcher(password).find()) {
            return new ValidationResult(false, "Password cannot contain more than 2 consecutive identical characters");
        }
        
        if (COMMON_PATTERNS.matcher(password).find()) {
            return new ValidationResult(false, "Password contains a common pattern that is too easy to guess");
        }
        
        // Check for keyboard patterns
        if (hasKeyboardPattern(password)) {
            return new ValidationResult(false, "Password contains a keyboard pattern that is too easy to guess");
        }
        
        return new ValidationResult(true, "Password is valid");
    }
    
    private static boolean hasKeyboardPattern(String password) {
        String[] keyboardRows = {
            "qwertyuiop",
            "asdfghjkl",
            "zxcvbnm"
        };
        
        String lowerPass = password.toLowerCase();
        
        // Check for sequences of 3 or more characters in any keyboard row
        for (String row : keyboardRows) {
            for (int i = 0; i <= row.length() - 3; i++) {
                String sequence = row.substring(i, i + 3);
                if (lowerPass.contains(sequence) || new StringBuilder(sequence).reverse().toString().contains(sequence)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
