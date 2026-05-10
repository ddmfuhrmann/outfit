package github.io.ddmfuhrmann.outfit.catalog.api.rest;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/catalog/products")
public class ProductController {

    private final CreateProductUseCase createProduct;
    private final UpdateProductUseCase updateProduct;
    private final DeactivateProductUseCase deactivateProduct;
    private final AddProductSkuUseCase addProductSku;
    private final DeactivateProductSkuUseCase deactivateProductSku;

    public ProductController(CreateProductUseCase createProduct,
                             UpdateProductUseCase updateProduct,
                             DeactivateProductUseCase deactivateProduct,
                             AddProductSkuUseCase addProductSku,
                             DeactivateProductSkuUseCase deactivateProductSku) {
        this.createProduct = createProduct;
        this.updateProduct = updateProduct;
        this.deactivateProduct = deactivateProduct;
        this.addProductSku = addProductSku;
        this.deactivateProductSku = deactivateProductSku;
    }

    @PostMapping
    ResponseEntity<ProductResponse> create(@RequestBody @Valid CreateProductRequest request) {
        var created = createProduct.execute(request);
        return ResponseEntity.created(URI.create("/catalog/products/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    ResponseEntity<ProductResponse> update(@PathVariable Long id, @RequestBody @Valid UpdateProductRequest request) {
        return ResponseEntity.ok(updateProduct.execute(id, request));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deactivate(@PathVariable Long id) {
        deactivateProduct.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/skus")
    ResponseEntity<ProductSkuResponse> addSku(@PathVariable Long id, @RequestBody @Valid AddSkuRequest request) {
        var created = addProductSku.execute(id, request);
        return ResponseEntity.created(URI.create("/catalog/products/" + id + "/skus/" + created.id())).body(created);
    }

    @DeleteMapping("/{id}/skus/{skuId}")
    ResponseEntity<Void> deactivateSku(@PathVariable Long id, @PathVariable Long skuId) {
        deactivateProductSku.execute(id, skuId);
        return ResponseEntity.noContent().build();
    }
}
