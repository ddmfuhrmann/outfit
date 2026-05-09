package github.io.ddmfuhrmann.outfit.shared.api.rest;

import github.io.ddmfuhrmann.outfit.shared.application.dto.LoginRequest;
import github.io.ddmfuhrmann.outfit.shared.application.dto.LoginResponse;
import github.io.ddmfuhrmann.outfit.shared.application.usecase.LoginUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;

    public AuthController(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.execute(request));
    }
}
