package github.io.ddmfuhrmann.outfit.party.api.rest;

import github.io.ddmfuhrmann.outfit.party.application.dto.*;
import github.io.ddmfuhrmann.outfit.party.application.usecase.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/party")
public class PartyController {

    private final CreatePartyUseCase createParty;
    private final UpdatePartyUseCase updateParty;
    private final DeactivatePartyUseCase deactivateParty;
    private final AddAddressUseCase addAddress;
    private final RemoveAddressUseCase removeAddress;
    private final AddContactUseCase addContact;
    private final RemoveContactUseCase removeContact;

    public PartyController(CreatePartyUseCase createParty, UpdatePartyUseCase updateParty,
                           DeactivatePartyUseCase deactivateParty, AddAddressUseCase addAddress,
                           RemoveAddressUseCase removeAddress, AddContactUseCase addContact,
                           RemoveContactUseCase removeContact) {
        this.createParty = createParty;
        this.updateParty = updateParty;
        this.deactivateParty = deactivateParty;
        this.addAddress = addAddress;
        this.removeAddress = removeAddress;
        this.addContact = addContact;
        this.removeContact = removeContact;
    }

    @PostMapping
    ResponseEntity<PartyCreatedResponse> create(@RequestBody CreatePartyRequest request) {
        PartyCreatedResponse response = createParty.execute(request);
        return ResponseEntity.created(URI.create("/party/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    ResponseEntity<Void> update(@PathVariable Long id, @RequestBody UpdatePartyRequest request) {
        updateParty.execute(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deactivate(@PathVariable Long id) {
        deactivateParty.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/addresses")
    ResponseEntity<Void> addAddress(@PathVariable Long id, @RequestBody AddAddressRequest request) {
        addAddress.execute(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/addresses/{addressId}")
    ResponseEntity<Void> removeAddress(@PathVariable Long id, @PathVariable Long addressId) {
        removeAddress.execute(id, addressId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/contacts")
    ResponseEntity<Void> addContact(@PathVariable Long id, @RequestBody AddContactRequest request) {
        addContact.execute(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    ResponseEntity<Void> removeContact(@PathVariable Long id, @PathVariable Long contactId) {
        removeContact.execute(id, contactId);
        return ResponseEntity.noContent().build();
    }
}
