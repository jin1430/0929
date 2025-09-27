package com.example.GoCafe.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    private final Key key;

    @Getter
    public final long jwtExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.expiration-ms:86400000}") long jwtExpirationMs // 1d
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generateToken(UserDetails user, long tokenVersion) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(user.getUsername())
                .addClaims(Map.of("ver", tokenVersion))
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> fn) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return fn.apply(claims);
    }
}
