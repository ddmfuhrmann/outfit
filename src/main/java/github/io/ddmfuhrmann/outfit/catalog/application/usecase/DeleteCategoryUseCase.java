package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.domain.repository.CategoryRepository;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.ProductRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public DeleteCategoryUseCase(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void execute(Long id) {
        var category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        if (productRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("Category is in use by one or more products");
        }
        category.delete();
        categoryRepository.save(category);
        categoryRepository.delete(category);
        log.info("Category deleted: id={}", id);
    }
}
