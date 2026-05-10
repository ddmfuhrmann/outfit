package github.io.ddmfuhrmann.outfit.party.application.usecase;

import github.io.ddmfuhrmann.outfit.party.application.dto.AddAddressRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.AddContactRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.CreatePartyRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.PartyCreatedResponse;
import github.io.ddmfuhrmann.outfit.party.domain.model.Party;
import github.io.ddmfuhrmann.outfit.party.domain.repository.PartyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class CreatePartyUseCase {

    private final PartyRepository partyRepository;

    public CreatePartyUseCase(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @Transactional
    public PartyCreatedResponse execute(CreatePartyRequest request) {
        var party = Party.builder()
                .personType(request.personType())
                .cnpj(request.cnpj())
                .cpf(request.cpf())
                .legalName(request.legalName())
                .name(request.name())
                .customer(request.customer())
                .supplier(request.supplier())
                .salesperson(request.salesperson())
                .commissionPercent(request.commissionPercent())
                .build();

        partyRepository.save(party);

        // getId() is non-null after save() (IDENTITY strategy assigns ID on INSERT)
        addInitialAddresses(party, request.addresses());
        addInitialContacts(party, request.contacts());

        log.info("Party created: id={}, legalName={}", party.getId(), party.getLegalName());
        return new PartyCreatedResponse(party.getId());
    }

    private void addInitialAddresses(Party party, List<AddAddressRequest> addresses) {
        if (addresses == null || addresses.isEmpty()) return;
        for (var req : addresses) {
            party.addAddress(req.street(), req.neighborhood(), req.zipCode(),
                    req.number(), req.complement(), req.cityIbgeCode());
        }
    }

    private void addInitialContacts(Party party, List<AddContactRequest> contacts) {
        if (contacts == null || contacts.isEmpty()) return;
        for (var req : contacts) {
            party.addContact(req.classification(), req.description());
        }
    }
}
