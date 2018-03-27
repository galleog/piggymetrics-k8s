package com.piggymetrics.auth.service;

import com.piggymetrics.auth.domain.User;
import com.piggymetrics.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    private UserRepository repository;

    @Override
    public void create(User user) {
        repository.findById(user.getUsername()).ifPresent(u -> {
            throw new IllegalArgumentException("User " + user.getUsername() + " already exists");
        });

        String hash = encoder.encode(user.getPassword());
        user.setPassword(hash);
        repository.save(user);

        logger.info("New user {} has been created", user.getUsername());
    }
}
