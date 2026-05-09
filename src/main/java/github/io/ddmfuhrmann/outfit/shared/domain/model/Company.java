package github.io.ddmfuhrmann.outfit.shared.domain.model;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "company")
public class Company extends BaseEntity {

    @Column(nullable = false, length = 14, unique = true)
    private String cnpj;

    @Column(nullable = false, length = 200)
    private String companyName;

    @Column(length = 200)
    private String tradeName;

    @Column(length = 300)
    private String street;

    @Column(length = 20)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    protected Company() {}

    public void update(String cnpj, String companyName, String tradeName,
                       String street, String phone, City city) {
        if (cnpj == null || cnpj.isBlank()) throw new IllegalArgumentException("cnpj is required");
        if (!cnpj.matches("\\d{14}")) throw new IllegalArgumentException("cnpj must be 14 digits");
        if (companyName == null || companyName.isBlank()) throw new IllegalArgumentException("companyName is required");
        this.cnpj = cnpj;
        this.companyName = companyName;
        this.tradeName = tradeName;
        this.street = street;
        this.phone = phone;
        this.city = city;
    }
}
