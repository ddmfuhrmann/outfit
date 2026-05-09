package github.io.ddmfuhrmann.outfit.shared.application.dto;

public record UpdateCompanyRequest(
        String cnpj,
        String companyName,
        String tradeName,
        String street,
        String phone,
        Long cityId
) {
}
