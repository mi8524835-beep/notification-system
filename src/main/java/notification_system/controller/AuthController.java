package notification_system.controller;

import lombok.RequiredArgsConstructor;
import notification_system.jwt.JwtProvider;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtProvider jwtProvider;

    @PostMapping("/token")
    public String createToken(
            @RequestBody Map<String, String> request
    ) {

        String username =
                request.get("username");

        return jwtProvider.createToken(
                username
        );
    }
}