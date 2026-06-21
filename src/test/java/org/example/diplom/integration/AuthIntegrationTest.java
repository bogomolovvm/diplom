package org.example.diplom.integration;

import org.example.diplom.dto.LoginRequest;
import org.example.diplom.model.User;
import org.example.diplom.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureRestTestClient
public class AuthIntegrationTest extends BaseIntegrationTest{

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired
    protected RestTestClient restTestClient;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User user = new User();
        user.setUsername("ivan");
        user.setPassword(passwordEncoder.encode("password"));
        userRepository.save(user);
    }

    @Test
    void login_validCredentials_shouldReturn200WithToken() {
        restTestClient.post().uri("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("ivan", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.auth-token").isNotEmpty();
    }

    @Test
    void login_wrongPassword_shouldReturn400() {
        restTestClient.post().uri("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("wrong", "wrong"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void protectedEndpoint_withoutToken_shouldReturn401() {
        restTestClient.get().uri("/list?limit=10")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedEndpoint_withValidToken_shouldReturn200() {
        Map<String, String> loginBody = restTestClient.post().uri("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("ivan", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, String>>() {})
                .returnResult()
                .getResponseBody();

        assert loginBody != null;

        String token = loginBody.get("auth-token");
        assertThat(token).isNotBlank();

        restTestClient.get().uri("/list?limit=10")
                .header("auth-token", token)
                .exchange()
                .expectStatus().isOk();
    }
}
