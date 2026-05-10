package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.AddSkuRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.ProductSkuResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.ProductRepository;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.SizeRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AddProductSkuUseCase {

    private final ProductRepository productRepository;
    private final SizeRepository sizeRepository;

    public AddProductSkuUseCase(ProductRepository productRepository, SizeRepository sizeRepository) {
        this.productRepository = productRepository;
        this.sizeRepository = sizeRepository;
    }

    @Transactional
    public ProductSkuResponse execute(Long productId, AddSkuRequest request) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        var size = sizeRepository.findById(request.sizeId())
                .orElseThrow(() -> new ResourceNotFoundException("Size not found: " + request.sizeId()));

        var sku = product.addSku(request.barcode(), request.sizeId(), request.implantationQty());
        productRepository.save(product);
        log.info("SKU added: skuId={}, productId={}", sku.getId(), productId);

        return ProductSkuResponse.from(sku, size.getDescription());
    }
}
