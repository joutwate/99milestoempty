package com.nnmilestoempty.model.auth;

import com.nnmilestoempty.converter.QueryableTextEncryptorConverter;
import com.nnmilestoempty.converter.TextEncryptorConverter;
import io.swagger.annotations.ApiModelProperty;

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
    private Long id;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column
    @Convert(converter = QueryableTextEncryptorConverter.class)
    private String username;

    @Column
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Role> roles;

    @Column
    @Convert(converter = QueryableTextEncryptorConverter.class)
    private String email;

    @Column
    private boolean enabled;

    @Column
    private boolean using2FA;

    @Column
    @Convert(converter = TextEncryptorConverter.class)
    private String secret2FA;

    @ApiModelProperty(value = "Date of when this contact was created within this system",
            example = "2016-07-31T04:20:41.689Z", readOnly = true)
    @Column
    private LocalDateTime creationDate;

    @ApiModelProperty(value = "Date of the last time this contact was modified", example = "2016-07-31T04:20:41.689Z",
            readOnly = true)
    @Column
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Role> getRoles() {
        return this.roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isUsing2FA() {
        return using2FA;
    }

    public void setUsing2FA(boolean using2FA) {
        this.using2FA = using2FA;
    }

    public String getSecret2FA() {
        return secret2FA;
    }

    public void setSecret2FA(String secret2FA) {
        this.secret2FA = secret2FA;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
