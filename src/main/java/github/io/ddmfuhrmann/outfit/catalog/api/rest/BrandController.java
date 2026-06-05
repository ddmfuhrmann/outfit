package github.io.ddmfuhrmann.outfit.catalog.api.rest;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandResponse;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.AddSupplierToBrandUseCase;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.CreateBrandUseCase;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.DeleteBrandUseCase;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.RemoveSupplierFromBrandUseCase;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.RenameBrandUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/catalog/brands")
public class BrandController {

    private final CreateBrandUseCase createBrand;
    private final RenameBrandUseCase renameBrand;
    private final DeleteBrandUseCase deleteBrand;
    private final AddSupplierToBrandUseCase addSupplierToBrand;
    private final RemoveSupplierFromBrandUseCase removeSupplierFromBrand;

    public BrandController(CreateBrandUseCase createBrand,
                           RenameBrandUseCase renameBrand,
                           DeleteBrandUseCase deleteBrand,
                           AddSupplierToBrandUseCase addSupplierToBrand,
                           RemoveSupplierFromBrandUseCase removeSupplierFromBrand) {
        this.createBrand = createBrand;
        this.renameBrand = renameBrand;
        this.deleteBrand = deleteBrand;
        this.addSupplierToBrand = addSupplierToBrand;
        this.removeSupplierFromBrand = removeSupplierFromBrand;
    }

    @PostMapping
    ResponseEntity<BrandResponse> create(@RequestBody @Valid BrandRequest request) {
        BrandResponse created = createBrand.execute(request);
        return ResponseEntity.created(URI.create("/catalog/brands/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    ResponseEntity<BrandResponse> rename(@PathVariable Long id, @RequestBody @Valid BrandRequest request) {
        return ResponseEntity.ok(renameBrand.execute(id, request));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteBrand.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/suppliers/{supplierId}")
    ResponseEntity<BrandResponse> addSupplier(@PathVariable Long id, @PathVariable Long supplierId) {
        return ResponseEntity.ok(addSupplierToBrand.execute(id, supplierId));
    }

    @DeleteMapping("/{id}/suppliers/{supplierId}")
    ResponseEntity<BrandResponse> removeSupplier(@PathVariable Long id, @PathVariable Long supplierId) {
        return ResponseEntity.ok(removeSupplierFromBrand.execute(id, supplierId));
    }
}
