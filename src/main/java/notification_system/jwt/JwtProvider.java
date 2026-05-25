package notification_system.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtProvider {

    private static final String SECRET =
            "my-secret-key-my-secret-key-my-secret-key";

    private final Key key =
            Keys.hmacShaKeyFor(
                    SECRET.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

    public String createToken(
            String username
    ) {

        return Jwts.builder()

                .setSubject(username)

                .setIssuedAt(
                        new Date()
                )

                .setExpiration(
                        new Date(
                                System.currentTimeMillis()
                                        + 1000 * 60 * 60
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

        return Jwts.parserBuilder()

                .setSigningKey(key)

                .build()

                .parseClaimsJws(token)

                .getBody()

                .getSubject();
    }


    public boolean validateToken(
            String token
    ) {

        try {

            Jwts.parserBuilder()

                    .setSigningKey(key)

                    .build()

                    .parseClaimsJws(token);

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}