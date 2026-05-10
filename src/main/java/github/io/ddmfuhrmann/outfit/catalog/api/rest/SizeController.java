package github.io.ddmfuhrmann.outfit.catalog.api.rest;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.SizeRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.SizeResponse;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.*;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/catalog/sizes")
public class SizeController {

    private final CreateSizeUseCase createSize;
    private final GetSizeUseCase getSize;
    private final ListSizesUseCase listSizes;
    private final RenameSizeUseCase renameSize;
    private final DeleteSizeUseCase deleteSize;

    public SizeController(CreateSizeUseCase createSize, GetSizeUseCase getSize,
                          ListSizesUseCase listSizes, RenameSizeUseCase renameSize,
                          DeleteSizeUseCase deleteSize) {
        this.createSize = createSize;
        this.getSize = getSize;
        this.listSizes = listSizes;
        this.renameSize = renameSize;
        this.deleteSize = deleteSize;
    }

    @PostMapping
    ResponseEntity<SizeResponse> create(@RequestBody @Valid SizeRequest request) {
        SizeResponse created = createSize.execute(request);
        return ResponseEntity.created(URI.create("/catalog/sizes/" + created.id())).body(created);
    }

    @GetMapping
    ResponseEntity<PageResponse<SizeResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listSizes.execute(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    ResponseEntity<SizeResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(getSize.execute(id));
    }

    @PutMapping("/{id}")
    ResponseEntity<SizeResponse> rename(@PathVariable Long id, @RequestBody @Valid SizeRequest request) {
        return ResponseEntity.ok(renameSize.execute(id, request));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteSize.execute(id);
        return ResponseEntity.noContent().build();
    }
}
