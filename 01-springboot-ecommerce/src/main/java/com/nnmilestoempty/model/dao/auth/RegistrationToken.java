package com.nnmilestoempty.model.dao.auth;

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
    private Long id;

    private String token;

    private String username;

    private LocalDateTime created;

    private LocalDateTime consumed;

    private RegistrationToken() {}

    public RegistrationToken(@NonNull String username) {
        this.username = username;
        this.token = RandomStringUtils.random(length, true, true);
        this.created = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public LocalDateTime getConsumed() {
        return consumed;
    }

    public void setConsumed(LocalDateTime consumed) {
        this.consumed = consumed;
    }
}
