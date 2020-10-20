package com.nnmilestoempty.base.model.dao.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
@NoArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    @Getter
    @Setter
    private Long id;

    @JsonIgnore
    @ManyToOne
    @Getter
    @Setter
    private User user;

    @Column
    @Getter
    @Setter
    private String role;

    public Role(String role) {
        this.role = role;
    }
}
