package github.io.ddmfuhrmann.outfit.party.application;

import github.io.ddmfuhrmann.outfit.party.domain.repository.PartyRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SupplierVerificationService {

    private final PartyRepository partyRepository;

    public SupplierVerificationService(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    public void verifyIsSupplier(Long partyId) {
        var party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found: " + partyId));
        if (!party.isSupplier()) {
            throw new IllegalArgumentException("Party is not a supplier: " + partyId);
        }
    }
}
