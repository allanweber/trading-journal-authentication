package com.trading.journal.authentication.authentication.service.impl;

import com.allanweber.jwttoken.data.AccessTokenInfo;
import com.allanweber.jwttoken.data.ContextUser;
import com.allanweber.jwttoken.data.TokenData;
import com.allanweber.jwttoken.service.JwtTokenProvider;
import com.allanweber.jwttoken.service.JwtTokenReader;
import com.trading.journal.authentication.ApplicationException;
import com.trading.journal.authentication.authentication.Login;
import com.trading.journal.authentication.authentication.LoginResponse;
import com.trading.journal.authentication.user.User;
import com.trading.journal.authentication.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class AuthenticationServiceImplTest {

    @Mock
    UserService userService;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    JwtTokenReader jwtTokenReader;

    @InjectMocks
    AuthenticationServiceImpl authenticationService;

    @Test
    @DisplayName("Successfully authenticate a user")
    void testAuthenticateUser() {
        Login login = new Login("mail@mail.com", "123456");

        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        ContextUser principal = new ContextUser("mail@mail.com", 1L, "username");
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, authorities);
        when(authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(login.email(), login.password())))
                .thenReturn(authentication);

        User user = User.builder()
                .id(1L)
                .password("12345679")
                .firstName("lastName")
                .lastName("Wick")
                .email("mail@mail.com")
                .enabled(true)
                .verified(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.getUserByEmail(login.email())).thenReturn(Optional.of(user));

        TokenData accessToken = new TokenData("token", LocalDateTime.now());
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn(accessToken);

        TokenData refreshToken = new TokenData("refreshToken", LocalDateTime.now());
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn(refreshToken);

        LoginResponse loginResponse = authenticationService.signIn(login);
        assertThat(loginResponse.accessToken()).isEqualTo("token");
        assertThat(loginResponse.refreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("When authentication fails throws an exception")
    void testAuthenticateFails() {
        Login login = new Login("mail@mail.com", "123456");
        when(authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(login.email(), login.password())))
                .thenThrow(new AuthenticationServiceException("Authentication failed"));

        assertThrows(AuthenticationServiceException.class, () -> authenticationService.signIn(login),
                "Authentication failed");

        verify(userService, never()).getUserByEmail(anyString());
        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Given a valid refresh token returns a new access token and keep the same refresh token")
    void refreshToken() {
        String refreshToken = UUID.randomUUID().toString();

        when(jwtTokenReader.isTokenValid(refreshToken)).thenReturn(true);

        AccessTokenInfo tokenInfo = new AccessTokenInfo("email@mail.com", 1L, null, Collections.singletonList("REFRESH_TOKEN"));
        when(jwtTokenReader.getTokenInfo(refreshToken)).thenReturn(tokenInfo);

        User user = User.builder()
                .id(1L)
                .password("12345679")
                .firstName("lastName")
                .lastName("Wick")
                .email("mail@mail.com")
                .enabled(true)
                .verified(true)
                .createdAt(LocalDateTime.now())
                .authorities(emptyList())
                .build();
        when(userService.getUserByEmail("email@mail.com")).thenReturn(Optional.of(user));

        TokenData tokenData = new TokenData("new_token", LocalDateTime.now());
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn(tokenData);

        LoginResponse loginResponse = authenticationService.refreshToken(refreshToken);
        assertThat(loginResponse.accessToken()).isEqualTo("new_token");
        assertThat(loginResponse.refreshToken()).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("Given a invalid refresh token returns a Unauthorized exception")
    void invalidRefreshToken() {
        String refreshToken = UUID.randomUUID().toString();

        when(jwtTokenReader.isTokenValid(refreshToken)).thenReturn(false);

        ApplicationException exception = assertThrows(ApplicationException.class, () -> authenticationService.refreshToken(refreshToken));
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getStatusText()).isEqualTo("Refresh token is expired");

        verify(jwtTokenReader, never()).getTokenInfo(anyString());
        verify(userService, never()).getUserByEmail(anyString());
        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Given a valid refresh token but the roles are not REFRESH_TOKEN returns a Unauthorized exception")
    void invalidRolesToken() {
        String refreshToken = UUID.randomUUID().toString();

        when(jwtTokenReader.isTokenValid(refreshToken)).thenReturn(true);

        AccessTokenInfo tokenInfo = new AccessTokenInfo("subject", 1L, null, Collections.singletonList("USER_ROLE"));
        when(jwtTokenReader.getTokenInfo(refreshToken)).thenReturn(tokenInfo);

        ApplicationException exception = assertThrows(ApplicationException.class, () -> authenticationService.refreshToken(refreshToken));
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getStatusText()).isEqualTo("Refresh token is invalid or is not a refresh token");

        verify(userService, never()).getUserByEmail(anyString());
        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }
}
