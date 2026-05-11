package github.io.ddmfuhrmann.outfit.query.api.rest;

import github.io.ddmfuhrmann.outfit.query.application.dto.ProductDocument;
import github.io.ddmfuhrmann.outfit.query.application.usecase.GetProductByIdUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.SearchProductsUseCase;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/catalog/products")
public class ProductQueryController {

    private final SearchProductsUseCase searchProducts;
    private final GetProductByIdUseCase getProductById;

    public ProductQueryController(SearchProductsUseCase searchProducts, GetProductByIdUseCase getProductById) {
        this.searchProducts = searchProducts;
        this.getProductById = getProductById;
    }

    @GetMapping
    @Operation(summary = "Search products",
            description = "Served from Elasticsearch. Reflects committed state synchronously — domain events are indexed before the write response is sent.")
    ResponseEntity<PageResponse<ProductDocument>> search(
            @RequestParam(required = false) String q,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(searchProducts.execute(q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID",
            description = "Served from Elasticsearch. Reflects committed state synchronously — domain events are indexed before the write response is sent.")
    ResponseEntity<ProductDocument> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getProductById.execute(id));
    }
}
