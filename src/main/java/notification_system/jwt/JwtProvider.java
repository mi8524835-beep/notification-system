package notification_system.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtProvider {

    private static final String SECRET =
            "my-secret-key-my-secret-key-my-secret-key";

    private static final long TOKEN_VALID_TIME =
            1000 * 60 * 60;

    private final Key key =
            Keys.hmacShaKeyFor(
                    SECRET.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

    public String createToken(
            Authentication authentication
    ) {

        String authorities =
                authentication.getAuthorities()
                        .stream()
                        .map(
                                GrantedAuthority::getAuthority
                        )
                        .collect(
                                Collectors.joining(",")
                        );

        return Jwts.builder()

                .setSubject(
                        authentication.getName()
                )

                .claim(
                        "auth",
                        authorities
                )

                .setIssuedAt(
                        new Date()
                )

                .setExpiration(
                        new Date(
                                System.currentTimeMillis()
                                        + TOKEN_VALID_TIME
                        )
                )

                .signWith(
                        key,
                        SignatureAlgorithm.HS256
                )

                .compact();
    }

    public String getUsername(
            String token
    ) {

        return getClaims(
                token
        ).getSubject();
    }

    public String getAuthorities(
            String token
    ) {

        return getClaims(
                token
        ).get(
                "auth",
                String.class
        );
    }

    public boolean validateToken(
            String token
    ) {

        try {

            getClaims(
                    token
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    private Claims getClaims(
            String token
    ) {

        return Jwts.parserBuilder()

                .setSigningKey(key)

                .build()

                .parseClaimsJws(token)

                .getBody();
    }
}