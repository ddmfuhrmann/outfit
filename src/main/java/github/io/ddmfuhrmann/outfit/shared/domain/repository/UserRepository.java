package github.io.ddmfuhrmann.outfit.shared.domain.repository;

import github.io.ddmfuhrmann.outfit.shared.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
}
