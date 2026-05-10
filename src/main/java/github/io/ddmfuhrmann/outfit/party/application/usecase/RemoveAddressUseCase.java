package github.io.ddmfuhrmann.outfit.party.application.usecase;

import github.io.ddmfuhrmann.outfit.party.domain.repository.PartyRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RemoveAddressUseCase {

    private final PartyRepository partyRepository;

    public RemoveAddressUseCase(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @Transactional
    public void execute(Long partyId, Long addressId) {
        var party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found: " + partyId));
        party.removeAddress(addressId);
        partyRepository.save(party);
        log.info("Address removed from party: partyId={}, addressId={}", partyId, addressId);
    }
}
