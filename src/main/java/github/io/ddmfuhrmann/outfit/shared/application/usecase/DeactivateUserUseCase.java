package github.io.ddmfuhrmann.outfit.shared.application.usecase;

import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import github.io.ddmfuhrmann.outfit.shared.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DeactivateUserUseCase {

    private final UserRepository userRepository;

    public DeactivateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void execute(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
        user.deactivate();
        log.info("User deactivated: id={}", id);
    }
}
