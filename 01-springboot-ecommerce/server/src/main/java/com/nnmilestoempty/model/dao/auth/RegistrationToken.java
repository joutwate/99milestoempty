package com.nnmilestoempty.model.dao.auth;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.lang.NonNull;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Registration token that is created when someone signs up for our site. This token is persisted and used to create a
 * unique URL that can be emailed to a user to verify their account.
 */
@Entity
@Table
public class RegistrationToken {
    private static final int length = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Getter
    private String token;

    @Getter
    private String username;

    @Getter
    private LocalDateTime created;

    @Getter
    @Setter
    private LocalDateTime consumed;

    private int expirationInMinutes = 30;

    protected RegistrationToken() {}

    public RegistrationToken(@NonNull String username) {
        this.username = username;
        this.token = RandomStringUtils.random(length, true, true);
        this.created = LocalDateTime.now();
    }

    public boolean isExpired() {
        boolean result = false;
        if (created.plusMinutes(expirationInMinutes).isBefore(LocalDateTime.now())) {
            // This token has expired.
            result = true;
        }

        return result;
    }
}