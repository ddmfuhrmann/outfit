package github.io.ddmfuhrmann.outfit.party.domain.model;

import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "party_address")
public class Address extends BaseEntity {

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(length = 300)
    private String street;

    @Column(length = 200)
    private String neighborhood;

    @Column(name = "zip_code", length = 8)
    private String zipCode;

    @Column(length = 20)
    private String number;

    @Column(length = 100)
    private String complement;

    @Column(name = "city_ibge_code")
    private Integer cityIbgeCode;

    protected Address() {}

    public static Address create(Long partyId, String street, String neighborhood,
                                 String zipCode, String number, String complement, Integer cityIbgeCode) {
        var address = new Address();
        address.partyId = partyId;
        address.street = street;
        address.neighborhood = neighborhood;
        address.zipCode = zipCode;
        address.number = number;
        address.complement = complement;
        address.cityIbgeCode = cityIbgeCode;
        return address;
    }
}
