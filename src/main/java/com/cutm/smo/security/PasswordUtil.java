package com.cutm.smo.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Password Utility for secure password hashing and validation using BCrypt.
 * Provides methods to encode and match passwords securely.
 */
@Slf4j
@Component
public class PasswordUtil {

    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Encode a plain text password using BCrypt.
     *
     * @param rawPassword the plain text password
     * @return hashed password
     */
    public static String encodePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            log.warn("Attempted to encode an empty or null password");
            return null;
        }
        String encoded = passwordEncoder.encode(rawPassword);
        log.debug("Password encoded successfully");
        return encoded;
    }

    /**
     * Verify if a raw password matches the hashed password.
     *
     * @param rawPassword the plain text password to verify
     * @param hashedPassword the hashed password to compare against
     * @return true if passwords match, false otherwise
     */
    public static boolean matchPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            log.warn("Attempted to match null passwords");
            return false;
        }
        boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);
        log.debug("Password match verification completed");
        return matches;
    }

    /**
     * Check if a password is already hashed (starts with $2a$, $2b$, or $2y$).
     *
     * @param password the password to check
     * @return true if password appears to be BCrypt hashed, false otherwise
     */
    public static boolean isBcryptHashed(String password) {
        if (password == null) {
            return false;
        }
        return password.startsWith("$2a$") ||
               password.startsWith("$2b$") ||
               password.startsWith("$2y$");
    }
}
