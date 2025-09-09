package com.content.springboot_rest_api.security;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // === Generate Token ===
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)             // simpan username
                .setIssuedAt(now)                 // waktu dibuat
                .setExpiration(expiryDate)        // waktu expired
                .signWith(SignatureAlgorithm.HS512, jwtSecret) // pakai secret biasa
                .compact();
    }

    // === Ambil username dari token ===
    public String getUsernameFromJWT(String token) {
        return Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)  // harus parseClaimsJws
                .getBody()
                .getSubject();
    }

    // === Validasi token ===
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            System.out.println("Invalid JWT signature: " + ex.getMessage());
        } catch (MalformedJwtException ex) {
            System.out.println("Invalid JWT token: " + ex.getMessage());
        } catch (ExpiredJwtException ex) {
            System.out.println("JWT token expired: " + ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            System.out.println("JWT token unsupported: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            System.out.println("JWT claims string is empty: " + ex.getMessage());
        }
        return false;
    }
}
