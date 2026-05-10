package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.domain.repository.ColorRepository;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.ProductRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DeleteColorUseCase {

    private final ColorRepository colorRepository;
    private final ProductRepository productRepository;

    public DeleteColorUseCase(ColorRepository colorRepository, ProductRepository productRepository) {
        this.colorRepository = colorRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void execute(Long id) {
        if (!colorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Color not found: " + id);
        }
        if (productRepository.existsByColorId(id)) {
            throw new IllegalStateException("Color is in use by one or more products");
        }
        colorRepository.deleteById(id);
        log.info("Color deleted: id={}", id);
    }
}
