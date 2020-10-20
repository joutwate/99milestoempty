package com.nnmilestoempty.base.model.dao.auth;

import com.nnmilestoempty.base.converter.QueryableTextEncryptorConverter;
import com.nnmilestoempty.base.converter.TextEncryptorConverter;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "Unique identifier for this contact", readOnly = true)
    @Getter
    @Setter
    private Long id;

    @Column
    @Getter
    @Setter
    private String firstName;

    @Column
    @Getter
    @Setter
    private String lastName;

    @Column
    @Convert(converter = QueryableTextEncryptorConverter.class)
    @Getter
    @Setter
    private String username;

    @Column
    @Getter
    @Setter
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Getter
    @Setter
    private Set<Role> roles;

    @Column
    @Convert(converter = QueryableTextEncryptorConverter.class)
    @Getter
    @Setter
    private String email;

    @Column
    @Getter
    @Setter
    private boolean enabled;

    @Column
    @Getter
    @Setter
    private boolean using2FA;

    @Column
    @Convert(converter = TextEncryptorConverter.class)
    @Getter
    @Setter
    private String secret2FA;

    @ApiModelProperty(value = "Date of when this contact was created within this system",
            example = "2016-07-31T04:20:41.689Z", readOnly = true)
    @Column
    @Getter
    private LocalDateTime creationDate;

    @ApiModelProperty(value = "Date of the last time this contact was modified", example = "2016-07-31T04:20:41.689Z",
            readOnly = true)
    @Column
    @Getter
    @Setter
    private LocalDateTime lastModifiedDate;

    public User() {
        creationDate = LocalDateTime.now();
    }

    public User(String firstName, String lastName, String email, String username, String password, boolean enabled) {
        creationDate = LocalDateTime.now();

        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
    }
}
