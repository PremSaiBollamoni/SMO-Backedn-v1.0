package com.cutm.smo.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA Attribute Converter for transparent AES-256 encryption/decryption
 *
 * Usage:
 * @Convert(converter = EncryptedStringConverter.class)
 * private String aadharNumber;
 *
 * Now the field is automatically encrypted when saved and decrypted when loaded.
 */
@Converter(autoApply = false)  // Manual application on fields
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static EncryptionUtil encryptionUtil;

    @Autowired
    public void setEncryptionUtil(EncryptionUtil encryptionUtil) {
        EncryptedStringConverter.encryptionUtil = encryptionUtil;
    }

    /**
     * Convert entity attribute (plaintext) to database column (encrypted)
     * Called when SAVING to database
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }

        if (encryptionUtil == null || !encryptionUtil.isEnabled()) {
            return attribute;
        }

        // Check if already encrypted (avoid double encryption)
        if (EncryptionUtil.isEncrypted(attribute)) {
            return attribute;
        }

        return encryptionUtil.encrypt(attribute);
    }

    /**
     * Convert database column (encrypted) to entity attribute (plaintext)
     * Called when LOADING from database
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }

        if (encryptionUtil == null || !encryptionUtil.isEnabled()) {
            return dbData;
        }

        // Check if encrypted before attempting decryption
        if (!EncryptionUtil.isEncrypted(dbData)) {
            return dbData;  // Return as-is if not encrypted
        }

        try {
            return encryptionUtil.decrypt(dbData);
        } catch (Exception e) {
            // Log error but don't crash - may be legacy unencrypted data
            System.err.println("Failed to decrypt field: " + e.getMessage());
            return dbData;  // Return encrypted value as fallback
        }
    }
}
