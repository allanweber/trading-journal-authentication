package com.trading.journal.authentication.api;

import com.trading.journal.authentication.MySqlTestContainerInitializer;
import com.trading.journal.authentication.authentication.Login;
import com.trading.journal.authentication.authentication.LoginResponse;
import com.trading.journal.authentication.authentication.service.AuthenticationService;
import com.trading.journal.authentication.email.service.EmailSender;
import com.trading.journal.authentication.registration.UserRegistration;
import com.trading.journal.authentication.user.UserInfo;
import com.trading.journal.authentication.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = MySqlTestContainerInitializer.class)
public class MeControllerTest {
    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationService authenticationService;

    @MockBean
    EmailSender emailSender;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    public void setUp() {
        doNothing().when(emailSender).send(any());
    }

    @DisplayName("When logged user hit Me endpoint, return its information")
    @ParameterizedTest
    @MethodSource("feedUsers")
    void meEndpoint(UserRegistration user) {
        userService.createNewUser(user, null);

        Login login = new Login(user.getEmail(), user.getPassword());

        LoginResponse loginResponse = authenticationService.signIn(login);

        assert loginResponse != null;
        webTestClient
                .get()
                .uri("/me")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + loginResponse.accessToken())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getUserName()).isEqualTo(user.getUserName());
                    assertThat(response.getFirstName()).isEqualTo(user.getFirstName());
                    assertThat(response.getLastName()).isEqualTo(user.getLastName());
                    assertThat(response.getEmail()).isEqualTo(user.getEmail());
                    assertThat(response.getAuthorities()).containsExactly("ROLE_USER");
                });
    }

    private static Stream<UserRegistration> feedUsers() {
        return Stream.of(
                new UserRegistration(null,
                        "John",
                        "Wick",
                        "johnwick",
                        "johnwick@mail.com",
                        "dad231#$#4",
                        "dad231#$#4"),
                new UserRegistration(
                        null,
                        "John",
                        "Rambo",
                        "johnrambo",
                        "johnrambo@mail.com",
                        "dad231#$#4",
                        "dad231#$#4"),
                new UserRegistration(
                        null,
                        "Han",
                        "Solo ",
                        "hansolo",
                        "hansolo@mail.com",
                        "dad231#$#4",
                        "dad231#$#4"));
    }
}
