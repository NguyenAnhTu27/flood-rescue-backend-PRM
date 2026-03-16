package com.floodrescue.shared.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter that transparently encrypts/decrypts String fields.
 * Uses deterministic encryption so Spring Data derived queries (findByPhone, existsByEmail, etc.)
 * continue to work — the query parameter is converted before being sent to the database.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static StringEncryptor encryptor;

    @Value("${app.encryption.secret}")
    public void setSecret(String secret) {
        EncryptedStringConverter.encryptor = new StringEncryptor(secret);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return encryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return encryptor.decrypt(dbData);
    }
}
