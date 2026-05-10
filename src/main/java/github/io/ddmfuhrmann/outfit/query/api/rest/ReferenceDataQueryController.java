package github.io.ddmfuhrmann.outfit.query.api.rest;

import github.io.ddmfuhrmann.outfit.query.application.dto.RefDocument;
import github.io.ddmfuhrmann.outfit.query.application.usecase.*;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class ReferenceDataQueryController {

    private static final String ES_NOTE = "Served from Elasticsearch. Reflects committed state synchronously — domain events are indexed before the write response is sent.";

    private final GetBrandByIdUseCase getBrandById;
    private final SearchBrandsUseCase searchBrands;
    private final GetCategoryByIdUseCase getCategoryById;
    private final SearchCategoriesUseCase searchCategories;
    private final GetColorByIdUseCase getColorById;
    private final SearchColorsUseCase searchColors;
    private final GetSizeByIdUseCase getSizeById;
    private final SearchSizesUseCase searchSizes;

    @GetMapping("/catalog/brands")
    @Operation(summary = "Search brands", description = ES_NOTE)
    ResponseEntity<PageResponse<RefDocument>> searchBrands(@RequestParam(required = false) String q, Pageable pageable) {
        return ResponseEntity.ok(searchBrands.execute(q, pageable));
    }

    @GetMapping("/catalog/brands/{id}")
    @Operation(summary = "Get brand by ID", description = ES_NOTE)
    ResponseEntity<RefDocument> getBrandById(@PathVariable Long id) {
        return ResponseEntity.ok(getBrandById.execute(id));
    }

    @GetMapping("/catalog/categories")
    @Operation(summary = "Search categories", description = ES_NOTE)
    ResponseEntity<PageResponse<RefDocument>> searchCategories(@RequestParam(required = false) String q, Pageable pageable) {
        return ResponseEntity.ok(searchCategories.execute(q, pageable));
    }

    @GetMapping("/catalog/categories/{id}")
    @Operation(summary = "Get category by ID", description = ES_NOTE)
    ResponseEntity<RefDocument> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(getCategoryById.execute(id));
    }

    @GetMapping("/catalog/colors")
    @Operation(summary = "Search colors", description = ES_NOTE)
    ResponseEntity<PageResponse<RefDocument>> searchColors(@RequestParam(required = false) String q, Pageable pageable) {
        return ResponseEntity.ok(searchColors.execute(q, pageable));
    }

    @GetMapping("/catalog/colors/{id}")
    @Operation(summary = "Get color by ID", description = ES_NOTE)
    ResponseEntity<RefDocument> getColorById(@PathVariable Long id) {
        return ResponseEntity.ok(getColorById.execute(id));
    }

    @GetMapping("/catalog/sizes")
    @Operation(summary = "Search sizes", description = ES_NOTE)
    ResponseEntity<PageResponse<RefDocument>> searchSizes(@RequestParam(required = false) String q, Pageable pageable) {
        return ResponseEntity.ok(searchSizes.execute(q, pageable));
    }

    @GetMapping("/catalog/sizes/{id}")
    @Operation(summary = "Get size by ID", description = ES_NOTE)
    ResponseEntity<RefDocument> getSizeById(@PathVariable Long id) {
        return ResponseEntity.ok(getSizeById.execute(id));
    }
}
