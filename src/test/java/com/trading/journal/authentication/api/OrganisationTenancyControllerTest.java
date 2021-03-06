package com.trading.journal.authentication.api;

import com.trading.journal.authentication.ApplicationException;
import com.trading.journal.authentication.MySqlTestContainerInitializer;
import com.trading.journal.authentication.WithCustomMockUser;
import com.trading.journal.authentication.jwt.data.AccessTokenInfo;
import com.trading.journal.authentication.jwt.service.JwtResolveToken;
import com.trading.journal.authentication.jwt.service.JwtTokenReader;
import com.trading.journal.authentication.tenancy.Tenancy;
import com.trading.journal.authentication.tenancy.service.TenancyService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = MySqlTestContainerInitializer.class)
@WithCustomMockUser(authorities = {"ROLE_ADMIN"})
class OrganisationTenancyControllerTest {

    public static final String PATH_WITH_ID = "/organisation/tenancy/{id}";

    @MockBean
    TenancyService tenancyService;

    @MockBean
    JwtTokenReader tokenReader;

    @MockBean
    JwtResolveToken resolveToken;

    private static WebTestClient webTestClient;

    @BeforeAll
    public static void setUp(@Autowired WebApplicationContext applicationContext) {
        webTestClient = MockMvcWebTestClient.bindToApplicationContext(applicationContext).build();
    }

    @BeforeEach
    public void mockAccessTokenInfo() {
        when(resolveToken.resolve(any())).thenReturn("token");
        when(tokenReader.getAccessTokenInfo(anyString()))
                .thenReturn(new AccessTokenInfo("user", 1L, "admin-user", singletonList("TENANCY_ADMIN")));
    }

    @DisplayName("Get tenancy info")
    @Test
    void getTenancy() {
        Tenancy tenancy = Tenancy.builder().id(1L).name("admin-user").userLimit(10).userUsage(0).enabled(true).build();

        when(tenancyService.getById(1L)).thenReturn(tenancy);

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(PATH_WITH_ID)
                        .build(tenancy.getId()))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Tenancy.class)
                .value(response -> {
                    assertThat(response.getName()).isEqualTo("admin-user");
                });
    }

    @DisplayName("Get tenancy info not found")
    @Test
    void getTenancyNotFound() {
        Tenancy tenancy = Tenancy.builder().id(1L).name("admin-user").userLimit(10).userUsage(0).enabled(true).build();

        when(tenancyService.getById(1L)).thenThrow(new ApplicationException(HttpStatus.NOT_FOUND, "Tenancy id not found"));

        webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(PATH_WITH_ID)
                        .build(tenancy.getId()))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(response ->
                        assertThat(response.get("error")).isEqualTo("Tenancy id not found")
                );
    }
}