package github.io.ddmfuhrmann.outfit.shared.application.usecase;

import github.io.ddmfuhrmann.outfit.shared.application.dto.CreateUserRequest;
import github.io.ddmfuhrmann.outfit.shared.application.dto.UserResponse;
import github.io.ddmfuhrmann.outfit.shared.domain.model.User;
import github.io.ddmfuhrmann.outfit.shared.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CreateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CreateUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse execute(CreateUserRequest request) {
        var user = User.create(request.login(), passwordEncoder.encode(request.password()),
                request.name(), request.role());
        var response = UserResponse.from(userRepository.save(user));
        log.info("User created: login={}, role={}", response.login(), response.role());
        return response;
    }
}
