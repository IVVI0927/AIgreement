package com.example.legalai.gateway.service;

import com.example.legalai.gateway.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {
    
    private final Map<String, User> userStore = new ConcurrentHashMap<>();
    private final Map<String, User> userByUsername = new ConcurrentHashMap<>();
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public UserService() {
        User adminUser = new User(
                UUID.randomUUID().toString(),
                "admin",
                "admin@legalai.com",
                "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG",
                "Admin",
                "User"
        );
        userStore.put(adminUser.getId(), adminUser);
        userByUsername.put(adminUser.getUsername(), adminUser);
    }
    
    public Mono<User> findByUsername(String username) {
        User user = userByUsername.get(username);
        return user != null ? Mono.just(user) : Mono.empty();
    }
    
    public Mono<User> createUser(String username, String email, String password, String firstName, String lastName) {
        if (userByUsername.containsKey(username)) {
            return Mono.error(new RuntimeException("Username already exists"));
        }
        
        User newUser = new User(
                UUID.randomUUID().toString(),
                username,
                email,
                passwordEncoder.encode(password),
                firstName,
                lastName
        );
        
        userStore.put(newUser.getId(), newUser);
        userByUsername.put(newUser.getUsername(), newUser);
        
        return Mono.just(newUser);
    }
    
    public Mono<Boolean> validateUser(String username, String password) {
        return findByUsername(username)
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .defaultIfEmpty(false);
    }
}