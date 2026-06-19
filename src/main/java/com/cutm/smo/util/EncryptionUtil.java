package com.cutm.smo.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption utility for sensitive data fields.
 * Provides transparent encryption/decryption for PII and financial data.
 *
 * FIELDS ENCRYPTED:
 * - aadhar_number (government ID)
 * - pan_card_number (tax ID)
 * - salary (financial data)
 * - phone (PII)
 * - dob (PII)
 * - emergency_contact (PII)
 * - blood_group (health data)
 * - address (PII)
 */
@Component
public class EncryptionUtil {

    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";

    @Value("${encryption.key:}")
    private String encryptionKeyEnv;

    @Value("${encryption.enabled:true}")
    private boolean encryptionEnabled;

    /**
     * Encrypts a plaintext string using AES-256-GCM
     *
     * @param plaintext The data to encrypt
     * @return Base64-encoded ciphertext with IV prepended
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty() || !encryptionEnabled) {
            return plaintext;
        }

        try {
            SecretKey key = getEncryptionKey();
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);

            // Generate random IV (nonce)
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Initialize cipher with IV
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            // Encrypt data
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext and return as Base64
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts an AES-256-GCM encrypted string
     *
     * @param encryptedData Base64-encoded data with IV prepended
     * @return Decrypted plaintext
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty() || !encryptionEnabled) {
            return encryptedData;
        }

        try {
            SecretKey key = getEncryptionKey();
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);

            // Decode Base64
            byte[] data = Base64.getDecoder().decode(encryptedData);

            // Extract IV and ciphertext
            ByteBuffer buffer = ByteBuffer.wrap(data);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            // Initialize cipher with IV for decryption
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a random AES-256 key for testing/dev purposes
     * NEVER use this in production - use external key management!
     */
    public static String generateRandomKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM);
        keyGen.init(256);
        SecretKey key = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Get encryption key from environment or configuration
     * CRITICAL: Key must be:
     * - 256 bits (32 bytes)
     * - Base64 encoded
     * - Retrieved from secure key management service
     * - NEVER hardcoded or logged
     */
    private SecretKey getEncryptionKey() {
        if (encryptionKeyEnv == null || encryptionKeyEnv.isEmpty()) {
            throw new RuntimeException(
                "ENCRYPTION_KEY environment variable not set. " +
                "Generate with: java -cp smo.jar com.cutm.smo.util.EncryptionUtil"
            );
        }

        try {
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKeyEnv);

            if (decodedKey.length != 32) {
                throw new RuntimeException("Encryption key must be 256 bits (32 bytes), got: " + (decodedKey.length * 8) + " bits");
            }

            return new SecretKeySpec(decodedKey, 0, decodedKey.length, KEY_ALGORITHM);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid encryption key format. Must be Base64 encoded.", e);
        }
    }

    /**
     * Check if a string appears to be encrypted (Base64 with length > 20)
     */
    public static boolean isEncrypted(String value) {
        if (value == null || value.length() < 20) {
            return false;
        }

        try {
            Base64.getDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Check if encryption is enabled
     */
    public boolean isEnabled() {
        return encryptionEnabled;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("AES-256 Key Generation for SMO Application");
        System.out.println("=".repeat(50));
        String key = generateRandomKey();
        System.out.println("\nGenerated Encryption Key (256-bit):");
        System.out.println(key);
        System.out.println("\nAdd to production environment variable:");
        System.out.println("export ENCRYPTION_KEY=\"" + key + "\"");
        System.out.println("\nOr in application-prod.properties:");
        System.out.println("encryption.key=" + key);
        System.out.println("\nWARNING: Keep this key secure! Store in:");
        System.out.println("- AWS Secrets Manager");
        System.out.println("- HashiCorp Vault");
        System.out.println("- Azure Key Vault");
        System.out.println("- Google Cloud KMS");
    }
}
