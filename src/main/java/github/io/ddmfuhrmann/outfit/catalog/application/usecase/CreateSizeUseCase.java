package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.SizeRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.SizeResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.model.Size;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.SizeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CreateSizeUseCase {

    private final SizeRepository sizeRepository;

    public CreateSizeUseCase(SizeRepository sizeRepository) {
        this.sizeRepository = sizeRepository;
    }

    @Transactional
    public SizeResponse execute(SizeRequest request) {
        var size = Size.create(request.description());
        var response = SizeResponse.from(sizeRepository.save(size));
        log.info("Size created: id={}, description={}", response.id(), response.description());
        return response;
    }
}
