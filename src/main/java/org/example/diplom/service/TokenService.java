package org.example.diplom.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TokenService {
    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();

    public String generateToken(String username) {
        log.debug("Token generation was called for user {}", username);
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, username);
        return token;
    }

    public Optional<String> getUsernameByToken(String token) {
        return Optional.ofNullable(tokenStore.get(token));
    }

    public void removeToken(String token) {
        String username = tokenStore.get(token);
        tokenStore.remove(token);
        log.debug("{} token was removed", username);
    }
}