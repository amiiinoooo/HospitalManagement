package com.example.projectlabexam.utils;

import java.util.regex.Pattern;

public class InputSanitizer {
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)\\b(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|ALTER|EXEC|EXECUTE|TRUNCATE|DECLARE|XTYPE)\\b|[';\\\\]|/\\*|\\*/|--"
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "<script|javascript:|vbscript:|onload=|onerror=|eval\\(|expression\\(|<iframe|<object",
        Pattern.CASE_INSENSITIVE
    );

    private static String escapeHtml(String input) {
        if (input == null) return null;
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Trim whitespace
        input = input.trim();
        
        // Block potential XSS attempts
        if (XSS_PATTERN.matcher(input).find()) {
            return null;
        }
        
        // Block SQL injection attempts
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            return null;
        }
        
        // HTML encode for XSS prevention
        input = escapeHtml(input);
        
        return input;
    }
    
    public static String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }
        
        email = email.trim().toLowerCase();
        
        // Only allow valid email characters and check format
        if (!email.matches("^[a-z0-9._]*[a-z0-9]@[a-z.]+\\.[a-z]{2,}$")) {
            return null;
        }
        
        return sanitizeInput(email);
    }
    
    public static String sanitizeName(String name) {
        if (name == null) {
            return null;
        }
        name = name.trim();
        // Only allow letters, no spaces, hyphens, or apostrophes (original logic)
        if (!name.matches("^[a-zA-Z]{2,50}$")) {
            return null;
        }
        // Remove multiple consecutive spaces/hyphens (not needed in original, but keep for safety)
        name = name.replaceAll("\\s+", " ").replaceAll("-+", "-");
        return sanitizeInput(name);
    }
    
    public static String sanitizePassword(String password) {
        if (password == null) {
            return null;
        }
        
        password = password.trim();
        
        // Basic length and complexity check (detailed check in PasswordValidator)
        if (password.length() < 8 || password.length() > 128) {
            return null;
        }
        
        // Block common injection patterns
        if (SQL_INJECTION_PATTERN.matcher(password).find() || 
            XSS_PATTERN.matcher(password).find()) {
            return null;
        }
        
        // Hash the password before returning
        return PasswordHasher.hashPassword(password);
    }
    
    public static String sanitizeSearchQuery(String query) {
        if (query == null) {
            return null;
        }
        
        query = query.trim();
        
        // Only allow alphanumeric and basic punctuation
        query = query.replaceAll("[^a-zA-Z0-9\\s.,_-]", "");
        
        return sanitizeInput(query);
    }
    
    public static String sanitizeToken(String token) {
        if (token == null) {
            return null;
        }
        
        token = token.trim();
        
        // Only allow alphanumeric characters
        if (!token.matches("^[a-zA-Z0-9]{1,100}$")) {
            return null;
        }
        
        return token;
    }
    
    public static String sanitizeUrl(String url) {
        if (url == null) {
            return null;
        }
        
        url = url.trim();
        
        // Only allow valid URL characters and check basic format
        if (!url.matches("^https?://[\\w.-]+(:\\d+)?(/[\\w.-]*)*/?$")) {
            return null;
        }
        
        return sanitizeInput(url);
    }
}