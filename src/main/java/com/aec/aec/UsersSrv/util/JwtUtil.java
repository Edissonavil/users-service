package com.aec.aec.UsersSrv.util;
import io.jsonwebtoken.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component 
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    public String extractUsername(String token) {
        return Jwts.parser().setSigningKey(secret)
                   .parseClaimsJws(token)
                   .getBody()
                   .getSubject();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = Jwts.parser().setSigningKey(secret)
                          .parseClaimsJws(token)
                          .getBody()
                          .getExpiration();
        return expiration.before(new Date());
    }

    public String generateToken(String username, long ttlMillis) {
        Date now = new Date();
        return Jwts.builder()
                   .setSubject(username)
                   .setIssuedAt(now)
                   .setExpiration(new Date(now.getTime() + ttlMillis))
                   .signWith(SignatureAlgorithm.HS256, secret)
                   .compact();
    }
}
