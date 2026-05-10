package github.io.ddmfuhrmann.outfit.catalog.api.rest;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.CategoryRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.CategoryResponse;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.*;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/catalog/categories")
public class CategoryController {

    private final CreateCategoryUseCase createCategory;
    private final GetCategoryUseCase getCategory;
    private final ListCategoriesUseCase listCategories;
    private final RenameCategoryUseCase renameCategory;
    private final DeleteCategoryUseCase deleteCategory;

    public CategoryController(CreateCategoryUseCase createCategory, GetCategoryUseCase getCategory,
                              ListCategoriesUseCase listCategories, RenameCategoryUseCase renameCategory,
                              DeleteCategoryUseCase deleteCategory) {
        this.createCategory = createCategory;
        this.getCategory = getCategory;
        this.listCategories = listCategories;
        this.renameCategory = renameCategory;
        this.deleteCategory = deleteCategory;
    }

    @PostMapping
    ResponseEntity<CategoryResponse> create(@RequestBody @Valid CategoryRequest request) {
        CategoryResponse created = createCategory.execute(request);
        return ResponseEntity.created(URI.create("/catalog/categories/" + created.id())).body(created);
    }

    @GetMapping
    ResponseEntity<PageResponse<CategoryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listCategories.execute(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    ResponseEntity<CategoryResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(getCategory.execute(id));
    }

    @PutMapping("/{id}")
    ResponseEntity<CategoryResponse> rename(@PathVariable Long id, @RequestBody @Valid CategoryRequest request) {
        return ResponseEntity.ok(renameCategory.execute(id, request));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteCategory.execute(id);
        return ResponseEntity.noContent().build();
    }
}
