package notification_system.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.
        UsernamePasswordAuthenticationToken;

import org.springframework.security.core.context.
        SecurityContextHolder;

import org.springframework.security.core.userdetails.
        User;

import org.springframework.security.web.authentication.
        WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.
        OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;


    @Override
    protected void doFilterInternal(

            HttpServletRequest request,

            HttpServletResponse response,

            FilterChain filterChain

    )

            throws ServletException,
            IOException {

        String header =
                request.getHeader(
                        "Authorization"
                );

        if (header != null &&
                header.startsWith(
                        "Bearer "
                )) {

            String token =
                    header.substring(
                            7
                    );

            if (jwtProvider.validateToken(
                    token
            )) {

                String username =
                        jwtProvider.getUsername(
                                token
                        );

                UsernamePasswordAuthenticationToken auth =

                        new UsernamePasswordAuthenticationToken(

                                new User(
                                        username,
                                        "",
                                        Collections.emptyList()
                                ),

                                null,

                                Collections.emptyList()
                        );

                auth.setDetails(

                        new WebAuthenticationDetailsSource()

                                .buildDetails(
                                        request
                                )
                );

                SecurityContextHolder
                        .getContext()

                        .setAuthentication(
                                auth
                        );
            }
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}
