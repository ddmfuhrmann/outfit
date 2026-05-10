package github.io.ddmfuhrmann.outfit.catalog.api.rest;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.SizeRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.SizeResponse;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.CreateSizeUseCase;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.DeleteSizeUseCase;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.RenameSizeUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/catalog/sizes")
public class SizeController {

    private final CreateSizeUseCase createSize;
    private final RenameSizeUseCase renameSize;
    private final DeleteSizeUseCase deleteSize;

    public SizeController(CreateSizeUseCase createSize, RenameSizeUseCase renameSize,
                          DeleteSizeUseCase deleteSize) {
        this.createSize = createSize;
        this.renameSize = renameSize;
        this.deleteSize = deleteSize;
    }

    @PostMapping
    ResponseEntity<SizeResponse> create(@RequestBody @Valid SizeRequest request) {
        SizeResponse created = createSize.execute(request);
        return ResponseEntity.created(URI.create("/catalog/sizes/" + created.id())).body(created);
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
