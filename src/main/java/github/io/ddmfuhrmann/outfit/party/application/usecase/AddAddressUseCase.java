package github.io.ddmfuhrmann.outfit.party.application.usecase;

import github.io.ddmfuhrmann.outfit.party.application.dto.AddAddressRequest;
import github.io.ddmfuhrmann.outfit.party.domain.repository.PartyRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AddAddressUseCase {

    private final PartyRepository partyRepository;

    public AddAddressUseCase(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @Transactional
    public void execute(Long partyId, AddAddressRequest request) {
        var party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found: " + partyId));
        party.addAddress(request.street(), request.neighborhood(), request.zipCode(),
                request.number(), request.complement(), request.cityId());
        partyRepository.saveAndFlush(party);
        party.onAddressAdded();
        partyRepository.save(party);
        log.info("Address added to party: partyId={}", partyId);
    }
}
