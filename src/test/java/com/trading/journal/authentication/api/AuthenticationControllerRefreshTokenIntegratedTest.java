package com.trading.journal.authentication.api;

import com.allanweber.jwttoken.data.JwtProperties;
import com.allanweber.jwttoken.helper.JwtConstants;
import com.allanweber.jwttoken.service.PrivateKeyProvider;
import com.trading.journal.authentication.PostgresTestContainerInitializer;
import com.trading.journal.authentication.authentication.Login;
import com.trading.journal.authentication.authentication.LoginResponse;
import com.trading.journal.authentication.authentication.service.AuthenticationService;
import com.trading.journal.authentication.email.service.EmailSender;
import com.trading.journal.authentication.helper.DateHelper;
import com.trading.journal.authentication.registration.UserRegistration;
import com.trading.journal.authentication.user.UserRepository;
import com.trading.journal.authentication.user.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = PostgresTestContainerInitializer.class)
public class AuthenticationControllerRefreshTokenIntegratedTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private PrivateKeyProvider privateKeyProvider;

    @Autowired
    private JwtProperties properties;

    @Autowired
    UserRepository userRepository;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    EmailSender emailSender;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
        doNothing().when(emailSender).send(any());
    }

    @Test
    @DisplayName("When refreshing token, return success and new token")
    void refreshToken() {
        UserRegistration user = new UserRegistration(
                null,
                "John",
                "Travolta",
                "johntravolta@mail.com",
                "dad231#$#4",
                "dad231#$#4",
                false
        );

        userService.createNewUser(user, null);

        Login login = new Login(user.getEmail(), user.getPassword());

        LoginResponse loginResponse = authenticationService.signIn(login);

        assert loginResponse != null;
        webTestClient
                .post()
                .uri("/auth/refresh-token")
                .header("refresh-token", loginResponse.refreshToken())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(LoginResponse.class)
                .value(response -> {
                    assertThat(response.accessToken()).isNotBlank();
                    assertThat(response.refreshToken()).isNotBlank();
                });
    }

    @Test
    @DisplayName("When refreshing token with access token, return unauthorized exception")
    void refreshTokenUnauthorized() {
        UserRegistration user = new UserRegistration(
                null,
                "allan",
                "weber",
                "allanweber@mail.com",
                "dad231#$#4",
                "dad231#$#4",
                false
        );

        userService.createNewUser(user, null);

        Login login = new Login(user.getEmail(), user.getPassword());

        LoginResponse loginResponse = authenticationService.signIn(login);

        assert loginResponse != null;
        webTestClient
                .post()
                .uri("/auth/refresh-token")
                .header("refresh-token", loginResponse.accessToken())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response -> assertThat(response.get("error"))
                        .isEqualTo("Refresh token is invalid or is not a refresh token"));
    }

    @Test
    @DisplayName("When refreshing token with expired token, return unauthorized exception")
    void refreshTokenExpired() throws IOException {
        Key privateKey = privateKeyProvider.provide(this.properties.getPrivateKey());

        Date expiration = Date.from(LocalDateTime.now().minusSeconds(1L)
                .atZone(ZoneId.systemDefault())
                .toInstant());

        Date issuedAt = DateHelper.getUTCDatetimeAsDate();
        String refreshToken = Jwts.builder()
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .setHeaderParam(JwtConstants.HEADER_TYP, JwtConstants.TOKEN_TYPE)
                .setIssuer("trade-journal")
                .setAudience("https://tradejournal.biz")
                .setSubject("allan")
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .claim(JwtConstants.SCOPES, Collections.singletonList(JwtConstants.REFRESH_TOKEN))
                .compact();

        webTestClient
                .post()
                .uri("/auth/refresh-token")
                .header("refresh-token", refreshToken)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response -> assertThat(response.get("error"))
                        .isEqualTo("Refresh token is expired"));
    }
}
