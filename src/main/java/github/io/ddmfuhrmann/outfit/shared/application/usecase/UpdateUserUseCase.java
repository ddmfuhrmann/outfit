package github.io.ddmfuhrmann.outfit.shared.application.usecase;

import github.io.ddmfuhrmann.outfit.shared.application.dto.UpdateUserRequest;
import github.io.ddmfuhrmann.outfit.shared.application.dto.UserResponse;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import github.io.ddmfuhrmann.outfit.shared.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UpdateUserUseCase {

    private final UserRepository userRepository;

    public UpdateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse execute(String login, UpdateUserRequest request) {
        var user = userRepository.findById(login)
                .orElseThrow(() -> new ResourceNotFoundException("User " + login + " not found"));
        if (request.name() != null) user.updateProfile(request.name());
        if (request.role() != null) user.changeRole(request.role());
        log.info("User updated: login={}", login);
        return UserResponse.from(user);
    }
}
