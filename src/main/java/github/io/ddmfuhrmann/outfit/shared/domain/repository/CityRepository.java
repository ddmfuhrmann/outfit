package github.io.ddmfuhrmann.outfit.shared.domain.repository;

import github.io.ddmfuhrmann.outfit.shared.domain.model.City;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Integer> {
    Page<City> findByCityNameContainingIgnoreCase(String name, Pageable pageable);
}
