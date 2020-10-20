package com.nnmilestoempty.base.model.dao.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.Instant;

@Entity
@NoArgsConstructor
public class LoginHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    @Getter
    private Long id;

    @Getter
    @Setter
    private String principal;

    @Getter
    @Setter
    private String type;

    @Getter
    @Setter
    private String ipAddress;

    @Getter
    @Setter
    private Instant eventTime;
}
