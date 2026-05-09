package github.io.ddmfuhrmann.outfit.shared.application.usecase;

import github.io.ddmfuhrmann.outfit.shared.application.dto.CompanyResponse;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import github.io.ddmfuhrmann.outfit.shared.domain.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCompanyUseCase {

    private final CompanyRepository companyRepository;

    public GetCompanyUseCase(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public CompanyResponse execute() {
        return companyRepository.findById(1L)
                .map(CompanyResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }
}
