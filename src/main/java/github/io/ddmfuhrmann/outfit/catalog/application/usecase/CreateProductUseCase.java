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
        if (request.colorId() != null && !colorRepository.existsById(request.colorId())) {
            throw new ResourceNotFoundException("Color not found: " + request.colorId());
        }
        if (request.brandId() != null && !brandRepository.existsById(request.brandId())) {
            throw new ResourceNotFoundException("Brand not found: " + request.brandId());
        }
        if (request.categoryId() != null && !categoryRepository.existsById(request.categoryId())) {
            throw new ResourceNotFoundException("Category not found: " + request.categoryId());
        }

        var product = Product.builder()
                .description(request.description())
                .price(request.price())
                .cost(request.cost())
                .purchaseDate(request.purchaseDate())
                .colorId(request.colorId())
                .brandId(request.brandId())
                .categoryId(request.categoryId())
                .build();

        for (var skuReq : request.skus()) {
            if (!sizeRepository.existsById(skuReq.sizeId())) {
                throw new ResourceNotFoundException("Size not found: " + skuReq.sizeId());
            }
            product.addSku(skuReq.barcode(), skuReq.sizeId(), skuReq.implantationQty());
        }

        var saved = productRepository.save(product);
        log.info("Product created: id={}", saved.getId());
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
