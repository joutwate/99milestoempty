package com.nnmilestoempty.base.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Converter that can be used to transform encrypt/decrypt string based attributes.
 */
@Converter
public class QueryableTextEncryptorConverter implements AttributeConverter<String, String> {
    @Autowired
    @Qualifier("queryableTextEncryptor")
    private TextEncryptor encryptor;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        String result = null;
        if (attribute != null) {
            result = encryptor.encrypt(attribute);
        }

        return result;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        String result = null;
        if (dbData != null) {
            result = encryptor.decrypt(dbData);
        }

        return result;
    }
}
