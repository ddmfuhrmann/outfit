package github.io.ddmfuhrmann.outfit.shared.application.dto;

import github.io.ddmfuhrmann.outfit.shared.domain.model.Company;

import java.time.Instant;

public record CompanyResponse(
        String cnpj,
        String companyName,
        String tradeName,
        String street,
        String phone,
        Integer cityIbgeCode,
        Instant createdAt,
        Instant updatedAt
) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getCnpj(),
                company.getCompanyName(),
                company.getTradeName(),
                company.getStreet(),
                company.getPhone(),
                company.getCity() != null ? company.getCity().getIbgeCityCode() : null,
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}
