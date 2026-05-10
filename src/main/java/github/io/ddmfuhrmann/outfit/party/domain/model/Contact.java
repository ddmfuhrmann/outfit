package github.io.ddmfuhrmann.outfit.party.domain.model;

import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "party_contact")
public class Contact extends BaseEntity {

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContactType classification;

    @Column(nullable = false, length = 200)
    private String description;

    protected Contact() {}

    public static Contact create(Long partyId, ContactType classification, String description) {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        var contact = new Contact();
        contact.partyId = partyId;
        contact.classification = classification;
        contact.description = description.trim();
        return contact;
    }
}
