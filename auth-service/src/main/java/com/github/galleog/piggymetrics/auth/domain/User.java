package com.github.galleog.piggymetrics.auth.domain;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 * Entity for users.
 */
@EqualsAndHashCode(of = "username")
public class User implements UserDetails {
    /**
     * Unique name of the user used as his/her login name.
     */
    private String username;
    /**
     * Password of the user.
     */
    private String password;

    @Builder
    @SuppressWarnings("unused")
    private User(@NonNull String username, @NonNull String password) {
        setUsername(username);
        setPassword(password);
    }

    @NonNull
    @Override
    public String getUsername() {
        return this.username;
    }

    private void setUsername(String username) {
        Validate.notBlank(username);
        this.username = username;
    }

    @NonNull
    @Override
    public String getPassword() {
        return password;
    }

    private void setPassword(String password) {
        Validate.notBlank(password);
        this.password = password;
    }

    @NonNull
    @Override
    public List<GrantedAuthority> getAuthorities() {
        return ImmutableList.of();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(getUsername())
                .build();
    }
}
