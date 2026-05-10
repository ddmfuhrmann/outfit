package github.io.ddmfuhrmann.outfit.catalog.api.rest;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandResponse;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.*;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/catalog/brands")
public class BrandController {

    private final CreateBrandUseCase createBrand;
    private final GetBrandUseCase getBrand;
    private final ListBrandsUseCase listBrands;
    private final RenameBrandUseCase renameBrand;
    private final DeleteBrandUseCase deleteBrand;

    public BrandController(CreateBrandUseCase createBrand, GetBrandUseCase getBrand,
                           ListBrandsUseCase listBrands, RenameBrandUseCase renameBrand,
                           DeleteBrandUseCase deleteBrand) {
        this.createBrand = createBrand;
        this.getBrand = getBrand;
        this.listBrands = listBrands;
        this.renameBrand = renameBrand;
        this.deleteBrand = deleteBrand;
    }

    @PostMapping
    ResponseEntity<BrandResponse> create(@RequestBody @Valid BrandRequest request) {
        BrandResponse created = createBrand.execute(request);
        return ResponseEntity.created(URI.create("/catalog/brands/" + created.id())).body(created);
    }

    @GetMapping
    ResponseEntity<PageResponse<BrandResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listBrands.execute(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    ResponseEntity<BrandResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(getBrand.execute(id));
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
}
