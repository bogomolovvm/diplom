package org.example.diplom.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {
    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();

    public String generateToken(String username) {
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, username);
        return token;
    }

    public Optional<String> getUsernameByToken(String token) {
        return Optional.ofNullable(tokenStore.get(token));
    }

    public void removeToken(String token) {
        tokenStore.remove(token);
    }
}