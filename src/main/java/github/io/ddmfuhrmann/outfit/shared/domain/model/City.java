package github.io.ddmfuhrmann.outfit.shared.domain.model;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "city")
public class City extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Integer ibgeCityCode;

    @Column(nullable = false)
    private Integer ibgeStateCode;

    @Column(nullable = false, length = 100)
    private String cityName;

    @Column(nullable = false, length = 100)
    private String stateName;

    @Column(nullable = false, length = 2)
    private String stateAbbr;

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
