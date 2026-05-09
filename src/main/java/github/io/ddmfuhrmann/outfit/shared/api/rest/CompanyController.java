package github.io.ddmfuhrmann.outfit.shared.api.rest;

import github.io.ddmfuhrmann.outfit.shared.application.dto.CompanyResponse;
import github.io.ddmfuhrmann.outfit.shared.application.dto.UpdateCompanyRequest;
import github.io.ddmfuhrmann.outfit.shared.application.usecase.GetCompanyUseCase;
import github.io.ddmfuhrmann.outfit.shared.application.usecase.UpdateCompanyUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shared/company")
public class CompanyController {

    private final GetCompanyUseCase getCompany;
    private final UpdateCompanyUseCase updateCompany;

    public CompanyController(GetCompanyUseCase getCompany, UpdateCompanyUseCase updateCompany) {
        this.getCompany = getCompany;
        this.updateCompany = updateCompany;
    }

    @GetMapping
    ResponseEntity<CompanyResponse> get() {
        return ResponseEntity.ok(getCompany.execute());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<CompanyResponse> update(@RequestBody UpdateCompanyRequest request) {
        return ResponseEntity.ok(updateCompany.execute(request));
    }
}
