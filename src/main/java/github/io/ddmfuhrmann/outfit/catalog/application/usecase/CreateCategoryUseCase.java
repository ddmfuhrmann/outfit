package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.CategoryRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.CategoryResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.model.Category;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public CreateCategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse execute(CategoryRequest request) {
        var category = Category.create(request.description(), request.ncmCode());
        var response = CategoryResponse.from(categoryRepository.save(category));
        log.info("Category created: id={}, description={}", response.id(), response.description());
        return response;
    }
}
