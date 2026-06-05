package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.CreateProductRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.ProductResponse;
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
public class CreateProductUseCase {

    private final ProductRepository productRepository;
    private final ColorRepository colorRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final SizeRepository sizeRepository;

    public CreateProductUseCase(ProductRepository productRepository,
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
    public ProductResponse execute(CreateProductRequest request) {
        Color color = request.colorId() != null
                ? colorRepository.findById(request.colorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Color not found: " + request.colorId()))
                : null;
        Brand brand = request.brandId() != null
                ? brandRepository.findById(request.brandId())
                        .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + request.brandId()))
                : null;
        Category category = request.categoryId() != null
                ? categoryRepository.findById(request.categoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()))
                : null;

        var product = Product.builder()
                .description(request.description())
                .price(request.price())
                .cost(request.cost())
                .purchaseDate(request.purchaseDate())
                .colorId(request.colorId())
                .brandId(request.brandId())
                .categoryId(request.categoryId())
                .build();

        var sizeIds = request.skus().stream().map(s -> s.sizeId()).toList();
        Map<Long, Size> sizesById = sizeRepository.findAllById(sizeIds).stream()
                .collect(Collectors.toMap(BaseAggregate::getId, s -> s));

        for (var skuReq : request.skus()) {
            if (!sizesById.containsKey(skuReq.sizeId())) {
                throw new ResourceNotFoundException("Size not found: " + skuReq.sizeId());
            }
            product.addSku(skuReq.barcode(), skuReq.sizeId(), skuReq.implantationQty());
        }

        var saved = productRepository.save(product);
        log.info("Product created: id={}", saved.getId());
        return buildResponse(saved, color, brand, category, sizesById);
    }

    private ProductResponse buildResponse(Product product, Color color, Brand brand, Category category,
                                          Map<Long, Size> sizesById) {
        String colorName = color != null ? color.getDescription() : null;
        String brandName = brand != null ? brand.getDescription() : null;
        String categoryName = category != null ? category.getDescription() : null;

        Map<Long, String> sizeNames = sizesById.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getDescription()));

        return ProductResponse.from(product, colorName, brandName, categoryName, sizeNames);
    }
}
