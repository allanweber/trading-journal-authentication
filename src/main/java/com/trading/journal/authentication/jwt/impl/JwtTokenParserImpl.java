package com.trading.journal.authentication.jwt.impl;

import com.trading.journal.authentication.ApplicationException;
import com.trading.journal.authentication.jwt.JwtException;
import com.trading.journal.authentication.jwt.JwtTokenParser;
import com.trading.journal.authentication.jwt.PublicKeyProvider;
import com.trading.journal.authentication.jwt.data.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.Key;

@Component
public class JwtTokenParserImpl implements JwtTokenParser {

    private final Key publicKey;

    public JwtTokenParserImpl(PublicKeyProvider publicKeyProvider, JwtProperties properties) {
        try {
            this.publicKey = publicKeyProvider.provide(properties.publicKey());
        } catch (IOException e) {
            throw new JwtException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("PMD")
    @Override
    public Jws<Claims> parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(this.publicKey)
                    .build()
                    .parseClaimsJws(token);
        } catch (ExpiredJwtException exception) {
            throw new ApplicationException(HttpStatus.UNAUTHORIZED,
                    String.format("Request to parse expired JWT : %s failed : %s", token, exception.getMessage()));
        } catch (UnsupportedJwtException exception) {
            throw new ApplicationException(HttpStatus.UNAUTHORIZED,
                    String.format("Request to parse unsupported JWT : %s failed : %s", token, exception.getMessage()));
        } catch (MalformedJwtException exception) {
            throw new ApplicationException(HttpStatus.UNAUTHORIZED,
                    String.format("Request to parse invalid JWT : %s failed : %s", token, exception.getMessage()));
        } catch (SignatureException exception) {
            throw new ApplicationException(HttpStatus.UNAUTHORIZED,
                    String.format("Request to parse JWT with invalid signature : %s failed : %s", token,
                            exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException(HttpStatus.UNAUTHORIZED,
                    String.format("Request to parse empty or null JWT : %s failed : %s", token,
                            exception.getMessage()));
        }
    }
}
