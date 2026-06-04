package github.io.ddmfuhrmann.outfit.party.application;

import github.io.ddmfuhrmann.outfit.party.domain.repository.PartyRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetSalespersonDetailsService {

    private final PartyRepository partyRepository;

    public GetSalespersonDetailsService(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    public SalespersonDetails execute(Long salespersonId) {
        var party = partyRepository.findById(salespersonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salesperson not found: " + salespersonId));
        String displayName = (party.getName() != null && !party.getName().isBlank())
                ? party.getName() : party.getLegalName();
        return new SalespersonDetails(salespersonId, displayName, party.getCommissionPercent());
    }
}
