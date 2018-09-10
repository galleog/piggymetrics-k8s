package com.github.galleog.piggymetrics.account.acl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.lang.NonNull;

/**
 * User of the system.
 */
@Getter
@JsonDeserialize(builder = User.UserBuilder.class)
public class User {
    @NonNull
    private String username;
    @NonNull
    private String password;

    @Builder
    private User(@NonNull String username, @NonNull String password) {
        setUsername(username);
        setPassword(password);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(getUsername()).build();
    }

    private void setUsername(String username) {
        Validate.notBlank(username);
        Validate.isTrue(username.length() >= 3 && username.length() <= 20);
        this.username = username;
    }

    private void setPassword(String password) {
        Validate.notBlank(password);
        Validate.isTrue(password.length() >= 6 && password.length() <= 40);
        this.password = password;
    }

    @JsonPOJOBuilder(withPrefix = StringUtils.EMPTY)
    public static final class UserBuilder {
    }
}
