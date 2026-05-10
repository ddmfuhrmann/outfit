package github.io.ddmfuhrmann.outfit.party.application.usecase;

import github.io.ddmfuhrmann.outfit.party.application.dto.AddContactRequest;
import github.io.ddmfuhrmann.outfit.party.domain.repository.PartyRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AddContactUseCase {

    private final PartyRepository partyRepository;

    public AddContactUseCase(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @Transactional
    public void execute(Long partyId, AddContactRequest request) {
        var party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found: " + partyId));
        party.addContact(request.classification(), request.description());
        partyRepository.save(party);
        log.info("Contact added to party: partyId={}", partyId);
    }
}
