package com.trading.journal.authentication.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.trading.journal.authentication.MongoInitializer;
import com.trading.journal.authentication.authentication.Login;
import com.trading.journal.authentication.authentication.LoginResponse;
import com.trading.journal.authentication.registration.UserRegistration;
import com.trading.journal.authentication.user.ApplicationUserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = MongoInitializer.class)
public class AuthenticationControllerTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ApplicationUserService applicationUserService;

    private WebTestClient webTestClient;

    @BeforeEach
    public void setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    @DisplayName("When signUp as new user return success")
    void signUp() {
        UserRegistration userRegistration = new UserRegistration(
                "firstName",
                "lastName",
                "UserName2",
                "mail2@mail.com",
                "dad231#$#4",
                "dad231#$#4");

        webTestClient
                .post()
                .uri("/authentication/signup")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(userRegistration)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    @DisplayName("When signIn user return success and token")
    void signIn() {
        UserRegistration userRegistration = new UserRegistration(
                "firstName",
                "lastName",
                "UserName",
                "mail@mail.com",
                "dad231#$#4",
                "dad231#$#4");

        applicationUserService.createNewUser(userRegistration).block();

        Login login = new Login("mail@mail.com", "dad231#$#4");

        webTestClient
                .post()
                .uri("/authentication/signin")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(login)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(LoginResponse.class)
                .value(response -> assertThat(response.token()).isNotBlank());
    }

    @Test
    @DisplayName("When signIn user that does not exist, return 401")
    void signInFails() {
        Login login = new Login("mail3@mail.com", "dad231#$#4");

        webTestClient
                .post()
                .uri("/authentication/signin")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(login)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response -> assertThat(response.get("error")).isEqualTo("User mail3@mail.com does not exist"));
    }

    @Test
    @DisplayName("When signIn with wrong password, return 401")
    void signInFailsPassword() {
        UserRegistration userRegistration = new UserRegistration(
                "firstName",
                "lastName",
                "UserName4",
                "mail4@mail.com",
                "dad231#$#4",
                "dad231#$#4");

        applicationUserService.createNewUser(userRegistration).block();

        Login login = new Login("mail@mail.com", "wrong_password");

        webTestClient
                .post()
                .uri("/authentication/signin")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(login)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response -> assertThat(response.get("error")).isEqualTo("Invalid Credentials"));
    }
}