package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.ProductResponse;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.UpdateProductRequest;
import github.io.ddmfuhrmann.outfit.catalog.domain.model.*;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.*;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UpdateProductUseCase {

    private final ProductRepository productRepository;
    private final ColorRepository colorRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final SizeRepository sizeRepository;

    public UpdateProductUseCase(ProductRepository productRepository,
                                ColorRepository colorRepository,
                                BrandRepository brandRepository,
                                CategoryRepository categoryRepository,
                                SizeRepository sizeRepository) {
        this.productRepository = productRepository;
        this.colorRepository = colorRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.sizeRepository = sizeRepository;
    }

    @Transactional
    public ProductResponse execute(Long id, UpdateProductRequest request) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        if (request.colorId() != null && !colorRepository.existsById(request.colorId())) {
            throw new ResourceNotFoundException("Color not found: " + request.colorId());
        }
        if (!brandRepository.existsById(request.brandId())) {
            throw new ResourceNotFoundException("Brand not found: " + request.brandId());
        }
        if (!categoryRepository.existsById(request.categoryId())) {
            throw new ResourceNotFoundException("Category not found: " + request.categoryId());
        }

        product.updateDetails(request.description(), request.price(), request.cost(),
                request.purchaseDate(), request.colorId(), request.brandId(), request.categoryId());

        var saved = productRepository.save(product);
        log.info("Product updated: id={}", id);
        return buildResponse(saved);
    }

    private ProductResponse buildResponse(Product product) {
        String colorName = product.getColorId() != null
                ? colorRepository.findById(product.getColorId()).map(Color::getDescription).orElse(null)
                : null;
        String brandName = brandRepository.findById(product.getBrandId()).map(Brand::getDescription).orElse(null);
        String categoryName = categoryRepository.findById(product.getCategoryId()).map(Category::getDescription).orElse(null);

        var sizeIds = product.getSkus().stream().map(ProductSku::getSizeId).toList();
        Map<Long, String> sizeNames = sizeRepository.findAllById(sizeIds).stream()
                .collect(Collectors.toMap(BaseAggregate::getId, Size::getDescription));

        return ProductResponse.from(product, colorName, brandName, categoryName, sizeNames);
    }
}
