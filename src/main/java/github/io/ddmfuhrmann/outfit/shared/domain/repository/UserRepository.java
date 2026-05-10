package github.io.ddmfuhrmann.outfit.shared.domain.repository;

import github.io.ddmfuhrmann.outfit.shared.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}
