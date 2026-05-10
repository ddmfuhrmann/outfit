package github.io.ddmfuhrmann.outfit.party.domain.repository;

import github.io.ddmfuhrmann.outfit.party.domain.model.Party;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyRepository extends JpaRepository<Party, Long> {}
