package github.io.ddmfuhrmann.outfit.party.domain.model;

import github.io.ddmfuhrmann.outfit.party.domain.event.*;
import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "party")
public class Party extends BaseAggregate<Party> {

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false, length = 20)
    private PersonType personType;

    @Embedded
    private Cnpj cnpj;

    @Embedded
    private Cpf cpf;

    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "customer", nullable = false)
    private boolean customer;

    @Column(name = "supplier", nullable = false)
    private boolean supplier;

    @Column(name = "salesperson", nullable = false)
    private boolean salesperson;

    @Column(name = "commission_percent", precision = 5, scale = 2)
    private BigDecimal commissionPercent;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "partyId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "partyId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Contact> contacts = new ArrayList<>();

    protected Party() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PersonType personType;
        private String cnpj;
        private String cpf;
        private String legalName;
        private String name;
        private boolean customer;
        private boolean supplier;
        private boolean salesperson;
        private BigDecimal commissionPercent;

        private Builder() {}

        public Builder personType(PersonType personType) { this.personType = personType; return this; }
        public Builder cnpj(String cnpj)                 { this.cnpj = cnpj;             return this; }
        public Builder cpf(String cpf)                   { this.cpf = cpf;               return this; }
        public Builder legalName(String legalName)       { this.legalName = legalName;   return this; }
        public Builder name(String name)                 { this.name = name;             return this; }
        public Builder customer(boolean customer)        { this.customer = customer;     return this; }
        public Builder supplier(boolean supplier)        { this.supplier = supplier;     return this; }
        public Builder salesperson(boolean salesperson)  { this.salesperson = salesperson; return this; }
        public Builder commissionPercent(BigDecimal v)   { this.commissionPercent = v;   return this; }

        public Party build() {
            if (personType == null) throw new IllegalArgumentException("personType is required");
            if (legalName == null || legalName.isBlank()) throw new IllegalArgumentException("legalName is required");
            if (!customer && !supplier && !salesperson)
                throw new IllegalArgumentException("at least one role must be true");

            var party = new Party();
            party.personType = personType;
            party.cnpj = personType == PersonType.LEGAL_ENTITY ? Cnpj.of(cnpj) : null;
            party.cpf  = personType == PersonType.INDIVIDUAL   ? Cpf.of(cpf)   : null;
            party.legalName = legalName.trim();
            party.name = name;
            party.customer = customer;
            party.supplier = supplier;
            party.salesperson = salesperson;
            party.commissionPercent = commissionPercent;
            party.active = true;
            party.registerEvent(new PartyCreated(party.getId(), party.toSnapshot()));
            return party;
        }
    }

    public void updateProfile(String legalName, String name,
                              BigDecimal commissionPercent) {
        if (legalName == null || legalName.isBlank()) throw new IllegalArgumentException("legalName is required");
        this.legalName = legalName.trim();
        this.name = name;
        this.commissionPercent = commissionPercent;
        registerEvent(new PartyUpdated(getId(), toSnapshot()));
    }

    public void deactivate() {
        if (!active) throw new IllegalStateException("party is already inactive");
        this.active = false;
        registerEvent(new PartyDeactivated(getId(), toSnapshot()));
    }

    public Address addAddress(String street, String neighborhood, String zipCode,
                              String number, String complement, Integer cityIbgeCode) {
        var address = Address.create(getId(), street, neighborhood, zipCode, number, complement, cityIbgeCode);
        addresses.add(address);
        registerEvent(new PartyAddressAdded(getId(), toSnapshot()));
        return address;
    }

    public void removeAddress(Long addressId) {
        var address = addresses.stream()
                .filter(a -> addressId.equals(a.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("address not found: " + addressId));
        addresses.remove(address);
        registerEvent(new PartyAddressRemoved(getId(), addressId, toSnapshot()));
    }

    public Contact addContact(ContactType classification, String description) {
        var contact = Contact.create(getId(), classification, description);
        contacts.add(contact);
        registerEvent(new PartyContactAdded(getId(), toSnapshot()));
        return contact;
    }

    public void removeContact(Long contactId) {
        var contact = contacts.stream()
                .filter(c -> contactId.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("contact not found: " + contactId));
        contacts.remove(contact);
        registerEvent(new PartyContactRemoved(getId(), contactId, toSnapshot()));
    }

    private PartySnapshot toSnapshot() {
        return new PartySnapshot(
                getId(),
                personType != null ? personType.name() : null,
                cnpj != null ? cnpj.value() : null,
                cpf != null ? cpf.value() : null,
                legalName,
                name,
                customer,
                supplier,
                salesperson,
                commissionPercent,
                active,
                getCreatedAt(),
                getUpdatedAt(),
                addresses.stream()
                        .map(address -> new PartyAddressSnapshot(
                                address.getId(),
                                address.getStreet(),
                                address.getNeighborhood(),
                                address.getZipCode(),
                                address.getNumber(),
                                address.getComplement(),
                                address.getCityIbgeCode()))
                        .toList(),
                contacts.stream()
                        .map(contact -> new PartyContactSnapshot(
                                contact.getId(),
                                contact.getClassification().name(),
                                contact.getDescription()))
                        .toList());
    }
}
