package com.trading.journal.authentication.api;

import com.trading.journal.authentication.MySqlTestContainerInitializer;
import com.trading.journal.authentication.authentication.Login;
import com.trading.journal.authentication.authentication.LoginResponse;
import com.trading.journal.authentication.authentication.service.AuthenticationService;
import com.trading.journal.authentication.authority.Authority;
import com.trading.journal.authentication.authority.AuthorityCategory;
import com.trading.journal.authentication.authority.AuthorityRepository;
import com.trading.journal.authentication.user.ApplicationUser;
import com.trading.journal.authentication.user.ApplicationUserRepository;
import com.trading.journal.authentication.userauthority.UserAuthority;
import com.trading.journal.authentication.userauthority.UserAuthorityRepository;
import com.trading.journal.authentication.userauthority.service.UserAuthorityService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = MySqlTestContainerInitializer.class)
class AuthoritiesControllerTest {

    private static String token;

    @Autowired
    AuthorityRepository authorityRepository;

    @Autowired
    UserAuthorityRepository userAuthorityRepository;

    @Autowired
    ApplicationUserRepository applicationUserRepository;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    public static void setUp(
            @Autowired ApplicationUserRepository applicationUserRepository,
            @Autowired PasswordEncoder encoder,
            @Autowired AuthenticationService authenticationService,
            @Autowired UserAuthorityService userAuthorityService
    ) {
        ApplicationUser user = ApplicationUser.builder()
                .userName("johnwick3")
                .password(encoder.encode("dad231#$#4"))
                .firstName("John")
                .lastName("Wick")
                .email("johnwick3@mail.com")
                .enabled(true)
                .verified(true)
                .createdAt(LocalDateTime.now())
                .build();
        ApplicationUser applicationUser = applicationUserRepository.save(user);
        userAuthorityService.saveAdminUserAuthorities(applicationUser);

        Login login = new Login("johnwick3@mail.com", "dad231#$#4");
        LoginResponse loginResponse = authenticationService.signIn(login);
        assertThat(loginResponse).isNotNull();
        token = loginResponse.accessToken();
    }

    @DisplayName("Get all authorities")
    @Test
    void getAll() {
        webTestClient
                .get()
                .uri("/admin/authorities")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<Authority>>() {
                })
                .value(response -> {
                    assertThat(response).hasSize(2);
                    assertThat(response).extracting(Authority::getName).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
                    assertThat(response).extracting(Authority::getCategory).containsExactlyInAnyOrder(AuthorityCategory.COMMON_USER, AuthorityCategory.ADMINISTRATOR);
                });
    }

    @DisplayName("Get all authorities category")
    @Test
    void getAllCategories() {
        webTestClient
                .get()
                .uri("/admin/authorities/categories")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<AuthorityCategory>>() {
                })
                .value(response -> {
                    assertThat(response).hasSize(2);
                    assertThat(response).containsExactlyInAnyOrder(AuthorityCategory.COMMON_USER, AuthorityCategory.ADMINISTRATOR);
                });
    }

    @DisplayName("Get authority by id")
    @Test
    void byId() {
        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/authorities/{id}")
                        .build(1L))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Authority.class)
                .value(response -> {
                    assertThat(response.getCategory()).isEqualTo(AuthorityCategory.COMMON_USER);
                    assertThat(response.getName()).isEqualTo("ROLE_USER");
                });

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/authorities/{id}")
                        .build(2L))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Authority.class)
                .value(response -> {
                    assertThat(response.getCategory()).isEqualTo(AuthorityCategory.ADMINISTRATOR);
                    assertThat(response.getName()).isEqualTo("ROLE_ADMIN");
                });

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/authorities/{id}")
                        .build(3L))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response ->
                        assertThat(response.get("error")).isEqualTo("Authority id not found")
                );
    }

    @DisplayName("Create a new authority")
    @Test
    void create() {
        webTestClient
                .post()
                .uri("/admin/authorities")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new Authority(AuthorityCategory.COMMON_USER, "ANOTHER_ROLE"))
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .exists("Location")
                .expectBody(Authority.class)
                .value(response -> {
                    assertThat(response.getCategory()).isEqualTo(AuthorityCategory.COMMON_USER);
                    assertThat(response.getName()).isEqualTo("ANOTHER_ROLE");
                });

        webTestClient
                .post()
                .uri("/admin/authorities")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new Authority(AuthorityCategory.COMMON_USER, "ANOTHER_ROLE"))
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response ->
                        assertThat(response.get("error")).isEqualTo("Authority name already exists")
                );

        authorityRepository.deleteById(3L);
    }

    @DisplayName("Update a authority")
    @Test
    void update() {
        webTestClient
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/authorities/{id}")
                        .build(1L))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new Authority(AuthorityCategory.COMMON_USER, "ROLE_USER_UPDATED"))
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Authority.class)
                .value(response -> {
                    assertThat(response.getCategory()).isEqualTo(AuthorityCategory.COMMON_USER);
                    assertThat(response.getName()).isEqualTo("ROLE_USER_UPDATED");
                });

        List<Authority> authorities = authorityRepository.findAll();
        assertThat(authorities).extracting(Authority::getName).containsExactlyInAnyOrder("ROLE_USER_UPDATED", "ROLE_ADMIN");

        webTestClient
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/authorities/{id}")
                        .build(1L))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new Authority(AuthorityCategory.COMMON_USER, "ROLE_ADMIN"))
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response ->
                        assertThat(response.get("error")).isEqualTo("Authority name already exists")
                );

        webTestClient
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/authorities/{id}")
                        .build(3L))
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new Authority(AuthorityCategory.COMMON_USER, "ROLE_ADMIN"))
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.NOT_FOUND)
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response ->
                        assertThat(response.get("error")).isEqualTo("Authority id not found")
                );

        authorityRepository.save(new Authority(1L, AuthorityCategory.COMMON_USER, "ROLE_USER"));
    }

    @DisplayName("Delete a authority")
    @Test
    void delete() {

        ApplicationUser applicationUser = applicationUserRepository.findByEmail("johnwick3@mail.com").orElse(null);
        assertThat(applicationUser).isNotNull();
        Authority anotherRole = authorityRepository.save(new Authority(AuthorityCategory.COMMON_USER, "ANOTHER_ROLE"));
        UserAuthority anotherRoleUserAuthority = userAuthorityRepository.save(
                UserAuthority.builder()
                        .authority(anotherRole)
                        .applicationUser(applicationUser)
                        .build()
        );

        webTestClient
                .delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/authorities/{id}")
                        .build(anotherRole.getId()))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response ->
                        assertThat(response.get("error")).isEqualTo("Authority is used by one or more user")
                );

        userAuthorityRepository.delete(anotherRoleUserAuthority);

        webTestClient
                .delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/authorities/{id}")
                        .build(anotherRole.getId()))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        webTestClient
                .delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/authorities/{id}")
                        .build(anotherRole.getId()))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.NOT_FOUND)
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response ->
                        assertThat(response.get("error")).isEqualTo("Authority id not found")
                );
    }
}