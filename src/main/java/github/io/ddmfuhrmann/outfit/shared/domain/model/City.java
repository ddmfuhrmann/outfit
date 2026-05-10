package github.io.ddmfuhrmann.outfit.shared.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Entity
@Table(name = "city")
@EntityListeners(AuditingEntityListener.class)
public class City {

    @Id
    private Integer ibgeCityCode;

    @Column(nullable = false)
    private Integer ibgeStateCode;

    @Column(nullable = false, length = 100)
    private String cityName;

    @Column(nullable = false, length = 100)
    private String stateName;

    @Column(nullable = false, length = 2)
    private String stateAbbr;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected City() {}

    public static City of(Integer ibgeCityCode, Integer ibgeStateCode,
                          String cityName, String stateName, String stateAbbr) {
        if (ibgeCityCode == null) throw new IllegalArgumentException("ibgeCityCode is required");
        if (cityName == null || cityName.isBlank()) throw new IllegalArgumentException("cityName is required");
        if (stateAbbr == null || stateAbbr.length() != 2) throw new IllegalArgumentException("stateAbbr must be 2 characters");
        var city = new City();
        city.ibgeCityCode = ibgeCityCode;
        city.ibgeStateCode = ibgeStateCode;
        city.cityName = cityName;
        city.stateName = stateName;
        city.stateAbbr = stateAbbr;
        return city;
    }
}
