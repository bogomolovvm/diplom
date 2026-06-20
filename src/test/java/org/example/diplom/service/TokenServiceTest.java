package org.example.diplom.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {
    private final TokenService tokenService = new TokenService();

    @Test
    void generateToken_shouldReturnTokenAndStoreUsername() {
        String username = "ivan";

        String token = tokenService.generateToken(username);


        assertThat(token).isNotBlank();
        assertThat(tokenService.getUsernameByToken(token))
                .isPresent()
                .contains(username);
    }

    @Test
    void removeToken_shouldInvalidateToken() {
        String token = tokenService.generateToken("ivan");

        tokenService.removeToken(token);

        assertThat(tokenService.getUsernameByToken(token)).isEmpty();
    }

    @Test
    void getUsernameByToken_unknownToken_shouldReturnEmpty() {
        assertThat(tokenService.getUsernameByToken("несуществующий-токен")).isEmpty();
    }
}