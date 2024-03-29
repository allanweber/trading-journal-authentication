package com.trading.journal.authentication.api;

import com.trading.journal.authentication.PostgresTestContainerInitializer;
import com.trading.journal.authentication.authority.Authority;
import com.trading.journal.authentication.authority.AuthorityCategory;
import com.trading.journal.authentication.email.service.EmailSender;
import com.trading.journal.authentication.registration.UserRegistration;
import com.trading.journal.authentication.user.UserRepository;
import com.trading.journal.authentication.userauthority.UserAuthority;
import com.trading.journal.authentication.userauthority.UserAuthorityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = PostgresTestContainerInitializer.class)
public class AuthenticationControllerTest {

    @Autowired
    UserAuthorityRepository userAuthorityRepository;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    UserRepository userRepository;

    @MockBean
    EmailSender emailSender;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
        doNothing().when(emailSender).send(any());
    }

    @Test
    @DisplayName("When signUp as new user return success and the UserAuthority entity has an id to Authority entity")
    void signUp() {
        UserRegistration userRegistration = new UserRegistration(
                null,
                "firstName",
                "lastName",
                "mail5@mail.com",
                "dad231#$#4",
                "dad231#$#4",
                false
        );

        webTestClient
                .post()
                .uri("/auth/signup")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(userRegistration)
                .exchange()
                .expectStatus()
                .isOk();

        List<UserAuthority> userAuthorities = userAuthorityRepository.findAll();
        assert userAuthorities != null;
        userAuthorities.forEach(userAuthority -> assertThat(userAuthority.getAuthority()).isNotNull());
        assertThat(userAuthorities).extracting(UserAuthority::getAuthority).extracting(Authority::getCategory)
                .containsExactlyInAnyOrder(AuthorityCategory.ORGANISATION, AuthorityCategory.COMMON_USER);
    }
}
