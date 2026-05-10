package github.io.ddmfuhrmann.outfit.party.application.usecase;

import github.io.ddmfuhrmann.outfit.party.domain.repository.PartyRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RemoveContactUseCase {

    private final PartyRepository partyRepository;

    public RemoveContactUseCase(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @Transactional
    public void execute(Long partyId, Long contactId) {
        var party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found: " + partyId));
        party.removeContact(contactId);
        log.info("Contact removed from party: partyId={}, contactId={}", partyId, contactId);
    }
}
