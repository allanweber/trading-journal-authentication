package com.trading.journal.authentication.api;

import com.trading.journal.authentication.MySqlTestContainerInitializer;
import com.trading.journal.authentication.TestLoader;
import com.trading.journal.authentication.authentication.Login;
import com.trading.journal.authentication.authentication.LoginResponse;
import com.trading.journal.authentication.authentication.service.AuthenticationService;
import com.trading.journal.authentication.user.ApplicationUser;
import com.trading.journal.authentication.user.ApplicationUserRepository;
import com.trading.journal.authentication.user.AuthoritiesChange;
import com.trading.journal.authentication.user.UserInfo;
import com.trading.journal.authentication.userauthority.UserAuthority;
import com.trading.journal.authentication.userauthority.UserAuthorityRepository;
import com.trading.journal.authentication.userauthority.service.UserAuthorityService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = MySqlTestContainerInitializer.class)
class UsersControllerTest {

    private static String token;

    @Autowired
    ApplicationUserRepository applicationUserRepository;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    public static void setUp(
            @Autowired ApplicationUserRepository applicationUserRepository,
            @Autowired UserAuthorityRepository userAuthorityRepository,
            @Autowired PasswordEncoder encoder,
            @Autowired AuthenticationService authenticationService,
            @Autowired UserAuthorityService userAuthorityService
    ) {
        TestLoader.load50Users(applicationUserRepository, userAuthorityRepository);

        ApplicationUser applicationUser = applicationUserRepository.save(new ApplicationUser(
                null,
                "johnwick",
                encoder.encode("dad231#$#4"),
                "John",
                "Wick",
                "johnwick@mail.com",
                true,
                true,
                emptyList(),
                LocalDateTime.now()));
        userAuthorityService.saveAdminUserAuthorities(applicationUser);

        Login login = new Login("johnwick@mail.com", "dad231#$#4");
        LoginResponse loginResponse = authenticationService.signIn(login);
        assertThat(loginResponse).isNotNull();
        token = loginResponse.accessToken();
    }

    @DisplayName("Get user by id")
    @Test
    void getUserById() {
        ApplicationUser applicationUser = applicationUserRepository.findByEmail("ermablack@email.com");
        assertThat(applicationUser).isNotNull();
        long userId = applicationUser.getId();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getFirstName()).isEqualTo("Erma");
                    assertThat(response.getLastName()).isEqualTo("Black");
                    assertThat(response.getEmail()).isEqualTo("ermablack@email.com");
                    assertThat(response.getUserName()).isEqualTo("ermablack");
                    assertThat(response.getVerified()).isEqualTo(true);
                    assertThat(response.getEnabled()).isEqualTo(true);
                });
    }

    @DisplayName("Get user by id not found")
    @Test
    void getUserByIdNotFound() {
        long userId = 1000L;
        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response ->
                        assertThat(response.get("error")).isEqualTo("User id not found")
                );
    }

    @DisplayName("Disable user by id")
    @Test
    void disableUserById() {
        ApplicationUser applicationUser = applicationUserRepository.findByEmail("ernestokim@email.com");
        assertThat(applicationUser).isNotNull();
        long userId = applicationUser.getId();

        webTestClient
                .patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}/disable")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getFirstName()).isEqualTo("Ernesto");
                    assertThat(response.getLastName()).isEqualTo("Kim");
                    assertThat(response.getEmail()).isEqualTo("ernestokim@email.com");
                    assertThat(response.getUserName()).isEqualTo("ernestokim");
                    assertThat(response.getVerified()).isEqualTo(true);
                    assertThat(response.getEnabled()).isEqualTo(false);
                });
    }

    @DisplayName("Disable user by id not found")
    @Test
    void disableUserByIdNotFound() {
        long userId = 1000L;
        webTestClient
                .patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}/disable")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response ->
                        assertThat(response.get("error")).isEqualTo("User id not found")
                );
    }

    @DisplayName("Enable user by id")
    @Test
    void enableUserById() {
        ApplicationUser applicationUser = applicationUserRepository.findByEmail("fanniehines@email.com");
        assertThat(applicationUser).isNotNull();
        long userId = applicationUser.getId();

        applicationUser.disable();
        applicationUser = applicationUserRepository.save(applicationUser);
        assertThat(applicationUser.getEnabled()).isFalse();

        webTestClient
                .patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}/enable")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getFirstName()).isEqualTo("Fannie");
                    assertThat(response.getLastName()).isEqualTo("Hines");
                    assertThat(response.getEmail()).isEqualTo("fanniehines@email.com");
                    assertThat(response.getUserName()).isEqualTo("fanniehines");
                    assertThat(response.getVerified()).isEqualTo(true);
                    assertThat(response.getEnabled()).isEqualTo(true);
                });
    }

    @DisplayName("Enable user by id not found")
    @Test
    void enableUserByIdNotFound() {
        long userId = 1000L;
        webTestClient
                .patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}/enable")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response ->
                        assertThat(response.get("error")).isEqualTo("User id not found")
                );
    }

    @DisplayName("Delete user by id")
    @Test
    void deleteUserById() {
        ApplicationUser applicationUser = applicationUserRepository.findByEmail("garylogan@email.com");
        assertThat(applicationUser).isNotNull();
        long userId = applicationUser.getId();

        webTestClient
                .delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        ApplicationUser byEmail = applicationUserRepository.findByEmail("garylogan@email.com");
        assertThat(byEmail).isNull();
    }

    @DisplayName("Delete user by id not found")
    @Test
    void DeleteUserByIdNotFound() {
        long userId = 1000L;
        webTestClient
                .delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response ->
                        assertThat(response.get("error")).isEqualTo("User id not found")
                );
    }

    @DisplayName("Delete user by id user recently deleted return not found")
    @Test
    void deleteUserByIdRecentlyDeleted() {
        ApplicationUser applicationUser = applicationUserRepository.findByEmail("laurieadams@email.com");
        assertThat(applicationUser).isNotNull();
        long userId = applicationUser.getId();

        webTestClient
                .delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        ApplicationUser byEmail = applicationUserRepository.findByEmail("laurieadams@email.com");
        assertThat(byEmail).isNull();

        webTestClient
                .delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response ->
                        assertThat(response.get("error")).isEqualTo("User id not found")
                );
    }

    @DisplayName("Add authorities to user")
    @Test
    void addAuthorities() {
        ApplicationUser applicationUser = applicationUserRepository.findByEmail("lorettastanley@email.com");
        assertThat(applicationUser).isNotNull();
        long userId = applicationUser.getId();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getAuthorities()).hasSize(1);
                    assertThat(response.getAuthorities()).containsExactlyInAnyOrder("ROLE_USER");
                });

        AuthoritiesChange authoritiesChange = new AuthoritiesChange(singletonList("ROLE_ADMIN"));
        webTestClient
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}/authorities")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(authoritiesChange)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getAuthorities()).hasSize(2);
                    assertThat(response.getAuthorities()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
                });
    }

    @DisplayName("Add same authorities that is already for the user do not add it again")
    @Test
    void addSameAuthorities() {
        ApplicationUser applicationUser = applicationUserRepository.findByEmail("natasharivera@email.com");
        assertThat(applicationUser).isNotNull();
        long userId = applicationUser.getId();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getAuthorities()).hasSize(1);
                    assertThat(response.getAuthorities()).containsExactlyInAnyOrder("ROLE_USER");
                });

        AuthoritiesChange authoritiesChange = new AuthoritiesChange(singletonList("ROLE_USER"));
        webTestClient
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}/authorities")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(authoritiesChange)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getAuthorities()).hasSize(1);
                    assertThat(response.getAuthorities()).containsExactlyInAnyOrder("ROLE_USER");
                });
    }

    @DisplayName("Add authorities with invalid name do not add it")
    @Test
    void addAuthoritiesInvalidName() {
        ApplicationUser applicationUser = applicationUserRepository.findByEmail("natasharivera@email.com");
        assertThat(applicationUser).isNotNull();
        long userId = applicationUser.getId();

        AuthoritiesChange authoritiesChange = new AuthoritiesChange(singletonList("ROLE_ANOTHER"));
        webTestClient
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}/authorities")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(authoritiesChange)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getAuthorities()).hasSize(1);
                    assertThat(response.getAuthorities()).containsExactlyInAnyOrder("ROLE_USER");
                });
    }

    @DisplayName("Add authorities user by id not found")
    @Test
    void addAuthoritiesUserNotFound() {
        long userId = 1000L;
        AuthoritiesChange authoritiesChange = new AuthoritiesChange(singletonList("ROLE_ANOTHER"));
        webTestClient
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}/authorities")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(authoritiesChange)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response ->
                        assertThat(response.get("error")).isEqualTo("User id not found")
                );
    }

    @DisplayName("Delete authority from user")
    @Test
    void deleteAuthority() {
        ApplicationUser applicationUser = applicationUserRepository.findByEmail("norawaters@email.com");
        assertThat(applicationUser).isNotNull();
        long userId = applicationUser.getId();

        AuthoritiesChange authoritiesChange = new AuthoritiesChange(singletonList("ROLE_ADMIN"));
        webTestClient
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}/authorities")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(authoritiesChange)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getAuthorities()).hasSize(2);
                    assertThat(response.getAuthorities()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
                });

        authoritiesChange = new AuthoritiesChange(singletonList("ROLE_USER"));
        webTestClient
                .method(HttpMethod.DELETE)
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}/authorities")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(authoritiesChange)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getAuthorities()).hasSize(1);
                    assertThat(response.getAuthorities()).containsExactlyInAnyOrder("ROLE_ADMIN");
                });
    }

    @DisplayName("Delete authority that does not exist for the user")
    @Test
    void deleteAuthorityNotThere() {
        ApplicationUser applicationUser = applicationUserRepository.findByEmail("pedrosullivan@email.com");
        assertThat(applicationUser).isNotNull();
        long userId = applicationUser.getId();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getAuthorities()).hasSize(1);
                    assertThat(response.getAuthorities()).containsExactlyInAnyOrder("ROLE_USER");
                });

        AuthoritiesChange authoritiesChange = new AuthoritiesChange(singletonList("ROLE_ADMIN"));
        webTestClient
                .method(HttpMethod.DELETE)
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}/authorities")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(authoritiesChange)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getAuthorities()).hasSize(1);
                    assertThat(response.getAuthorities()).containsExactlyInAnyOrder("ROLE_USER");
                });
    }

    @DisplayName("Delete authority that is invalid for the user")
    @Test
    void deleteAuthorityInvalid() {
        ApplicationUser applicationUser = applicationUserRepository.findByEmail("phyllisterry@email.com");
        assertThat(applicationUser).isNotNull();
        long userId = applicationUser.getId();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getAuthorities()).hasSize(1);
                    assertThat(response.getAuthorities()).containsExactlyInAnyOrder("ROLE_USER");
                });

        AuthoritiesChange authoritiesChange = new AuthoritiesChange(singletonList("ROLE_ANOTHER"));
        webTestClient
                .method(HttpMethod.DELETE)
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}/authorities")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(authoritiesChange)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getAuthorities()).hasSize(1);
                    assertThat(response.getAuthorities()).containsExactlyInAnyOrder("ROLE_USER");
                });
    }

    @DisplayName("Delete all authorities from user")
    @Test
    void deleteAllAuthorities() {
        ApplicationUser applicationUser = applicationUserRepository.findByEmail("sabrinagarcia@email.com");
        assertThat(applicationUser).isNotNull();
        long userId = applicationUser.getId();

        AuthoritiesChange authoritiesChange = new AuthoritiesChange(singletonList("ROLE_ADMIN"));
        webTestClient
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}/authorities")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(authoritiesChange)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getAuthorities()).hasSize(2);
                    assertThat(response.getAuthorities()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
                });

        authoritiesChange = new AuthoritiesChange(asList("ROLE_USER", "ROLE_ADMIN"));
        webTestClient
                .method(HttpMethod.DELETE)
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}/authorities")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(authoritiesChange)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInfo.class)
                .value(response -> {
                    assertThat(response.getAuthorities()).hasSize(0);
                });
    }

    @DisplayName("Delete authorities user by id not found")
    @Test
    void deleteAuthoritiesUserNotFound() {
        long userId = 1000L;
        AuthoritiesChange authoritiesChange = new AuthoritiesChange(singletonList("ROLE_ADMIN"));
        webTestClient
                .method(HttpMethod.DELETE)
                .uri(uriBuilder -> uriBuilder
                        .path("/users/{userId}/authorities")
                        .build(userId))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(authoritiesChange)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response ->
                        assertThat(response.get("error")).isEqualTo("User id not found")
                );
    }
}
