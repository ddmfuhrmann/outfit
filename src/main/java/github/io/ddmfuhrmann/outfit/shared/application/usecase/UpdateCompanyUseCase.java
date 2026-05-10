package github.io.ddmfuhrmann.outfit.shared.application.usecase;

import github.io.ddmfuhrmann.outfit.shared.application.dto.CompanyResponse;
import github.io.ddmfuhrmann.outfit.shared.application.dto.UpdateCompanyRequest;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import github.io.ddmfuhrmann.outfit.shared.domain.model.City;
import github.io.ddmfuhrmann.outfit.shared.domain.repository.CityRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.repository.CompanyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UpdateCompanyUseCase {

    private final CompanyRepository companyRepository;
    private final CityRepository cityRepository;

    public UpdateCompanyUseCase(CompanyRepository companyRepository, CityRepository cityRepository) {
        this.companyRepository = companyRepository;
        this.cityRepository = cityRepository;
    }

    @Transactional
    public CompanyResponse execute(UpdateCompanyRequest request) {
        var company = companyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        City city = request.cityIbgeCode() != null
                ? cityRepository.findById(request.cityIbgeCode())
                        .orElseThrow(() -> new ResourceNotFoundException("City " + request.cityIbgeCode() + " not found"))
                : null;
        company.update(request.cnpj(), request.companyName(), request.tradeName(),
                request.street(), request.phone(), city);
        log.info("Company updated: cnpj={}", company.getCnpj());
        return CompanyResponse.from(company);
    }
}
