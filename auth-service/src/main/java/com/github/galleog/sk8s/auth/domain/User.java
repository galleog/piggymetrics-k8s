package com.github.galleog.sk8s.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.domain.Persistable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * Entity for users.
 */
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@JsonDeserialize(builder = User.UserBuilder.class)
@EqualsAndHashCode(of = "username")
public class User implements UserDetails, Persistable<String>, Serializable {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /**
     * Unique name of the user used as his/her login name.
     */
    @Id
    @NonNull
    @Access(AccessType.PROPERTY)
    private String username;
    /**
     * Password of the user.
     */
    @NonNull
    private String password;

    @Version
    @Nullable
    @JsonIgnore
    private Integer version;

    @Builder
    private User(@NonNull String username, @NonNull String password) {
        setUsername(username);
        setPassword(password);
    }

    @NonNull
    @Override
    @Transient
    public String getId() {
        return username;
    }

    @Override
    @Transient
    public boolean isNew() {
        return version == null;
    }

    @NonNull
    @Override
    public String getUsername() {
        return username;
    }

    @NonNull
    @Override
    public String getPassword() {
        return password;
    }

    @Override
    @Nullable
    @Transient
    public List<GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    @Transient
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @Transient
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @Transient
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @Transient
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(getUsername()).build();
    }

    private void setUsername(@NonNull String username) {
        Validate.notBlank(username);
        this.username = username;
    }

    private void setPassword(@NonNull String password) {
        Validate.notBlank(password);
        this.password = ENCODER.encode(password);
    }

    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class UserBuilder {
    }
}
