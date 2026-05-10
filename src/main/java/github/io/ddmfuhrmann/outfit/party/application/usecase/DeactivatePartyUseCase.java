package github.io.ddmfuhrmann.outfit.party.application.usecase;

import github.io.ddmfuhrmann.outfit.party.domain.repository.PartyRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DeactivatePartyUseCase {

    private final PartyRepository partyRepository;

    public DeactivatePartyUseCase(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @Transactional
    public void execute(Long id) {
        var party = partyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found: " + id));
        party.deactivate();
        partyRepository.save(party);
        log.info("Party deactivated: id={}", id);
    }
}
