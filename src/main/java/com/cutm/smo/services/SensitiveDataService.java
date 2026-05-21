package com.cutm.smo.services;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.cutm.smo.util.LoggingUtil;

@Slf4j
@Service
public class SensitiveDataService {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public SensitiveDataService(@Value("${app.security.aes-key:SMO_DEFAULT_32_CHAR_SECRET_KEY_!}") String key) {
        this.secretKey = new SecretKeySpec(normalizeKey(key), "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            log.debug("Encryption skipped: plainText is null or blank");
            return null;
        }
        long startTime = System.currentTimeMillis();
        try {
            log.debug("=== ENCRYPTION START ===");
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            String result = Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Data Encryption", startTime, endTime);
            log.debug("=== ENCRYPTION END - SUCCESS ===");
            return result;
        } catch (GeneralSecurityException ex) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Encryption failed", ex);
            LoggingUtil.logPerformance(log, "Data Encryption (Failed)", startTime, endTime);
            throw new IllegalStateException("Encryption failed", ex);
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            log.debug("Decryption skipped: encrypted is null or blank");
            return null;
        }
        long startTime = System.currentTimeMillis();
        try {
            log.debug("=== DECRYPTION START ===");
            String[] parts = encrypted.split(":", 2);
            if (parts.length != 2) {
                log.warn("Decryption failed: Invalid encrypted format");
                return encrypted;
            }
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(cipherBytes);
            String result = new String(plain, StandardCharsets.UTF_8);
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, "Data Decryption", startTime, endTime);
            log.debug("=== DECRYPTION END - SUCCESS ===");
            return result;
        } catch (Exception ex) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Decryption failed", ex);
            LoggingUtil.logPerformance(log, "Data Decryption (Failed)", startTime, endTime);
            log.warn("Returning encrypted value as-is due to decryption failure");
            return encrypted;
        }
    }

    public String maskLast4(String value) {
        if (value == null || value.isBlank()) {
            log.debug("Masking skipped: value is null or blank");
            return "";
        }
        try {
            log.debug("=== DATA MASKING START ===");
            String trimmed = value.trim();
            String result;
            if (trimmed.length() <= 4) {
                result = trimmed;
                log.debug("Value length <= 4, returning as-is");
            } else {
                result = "*".repeat(trimmed.length() - 4) + trimmed.substring(trimmed.length() - 4);
                log.debug("Masked {} characters, kept last 4", trimmed.length() - 4);
            }
            log.debug("=== DATA MASKING END - SUCCESS ===");
            return result;
        } catch (Exception ex) {
            LoggingUtil.logError(log, "Data masking failed", ex);
            return value;
        }
    }

    private byte[] normalizeKey(String key) {
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        byte[] normalized = new byte[32];
        int copyLen = Math.min(raw.length, normalized.length);
        System.arraycopy(raw, 0, normalized, 0, copyLen);
        return normalized;
    }
}
