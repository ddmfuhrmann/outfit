package github.io.ddmfuhrmann.outfit.query.application.listener;

import github.io.ddmfuhrmann.outfit.party.domain.event.*;
import github.io.ddmfuhrmann.outfit.query.application.usecase.IndexPartyUseCase;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class PartyIndexListener {

    private final IndexPartyUseCase indexParty;

    public PartyIndexListener(IndexPartyUseCase indexParty) {
        this.indexParty = indexParty;
    }

    @ApplicationModuleListener
    public void on(PartyCreated event) {
        indexParty.execute(event.snapshot());
    }

    @ApplicationModuleListener
    public void on(PartyUpdated event) {
        indexParty.execute(event.snapshot());
    }

    @ApplicationModuleListener
    public void on(PartyDeactivated event) {
        indexParty.execute(event.snapshot());
    }

    @ApplicationModuleListener
    public void on(PartyAddressAdded event) {
        indexParty.execute(event.snapshot());
    }

    @ApplicationModuleListener
    public void on(PartyAddressRemoved event) {
        indexParty.execute(event.snapshot());
    }

    @ApplicationModuleListener
    public void on(PartyContactAdded event) {
        indexParty.execute(event.snapshot());
    }

    @ApplicationModuleListener
    public void on(PartyContactRemoved event) {
        indexParty.execute(event.snapshot());
    }
}
