package org.example.diplom.controller;

import org.example.diplom.dto.LoginRequest;
import org.example.diplom.dto.LoginResponse;
import org.example.diplom.service.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;



@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthController authController;

    @Test
    void login_validCredentials_shouldReturnToken() {
        // ARRANGE
        LoginRequest request = new LoginRequest();
        request.setLogin("ivan");
        request.setPassword("password");

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getName()).thenReturn("ivan");
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);

        when(tokenService.generateToken("ivan")).thenReturn("test-token-123");

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getAuthToken()).isEqualTo("test-token-123");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService).generateToken("ivan");
    }

    @Test
    void login_invalidCredentials_shouldThrow() {
        LoginRequest request = new LoginRequest();
        request.setLogin("ivan");
        request.setPassword("wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }


    @Test
    void logout_shouldRemoveToken() {
        authController.logout("test-token-123");

        verify(tokenService).removeToken("test-token-123");
    }
}