package github.io.ddmfuhrmann.outfit.catalog.api.rest;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.ColorRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.ColorResponse;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.CreateColorUseCase;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.DeleteColorUseCase;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.RenameColorUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/catalog/colors")
public class ColorController {

    private final CreateColorUseCase createColor;
    private final RenameColorUseCase renameColor;
    private final DeleteColorUseCase deleteColor;

    public ColorController(CreateColorUseCase createColor, RenameColorUseCase renameColor,
                           DeleteColorUseCase deleteColor) {
        this.createColor = createColor;
        this.renameColor = renameColor;
        this.deleteColor = deleteColor;
    }

    @PostMapping
    ResponseEntity<ColorResponse> create(@RequestBody @Valid ColorRequest request) {
        ColorResponse created = createColor.execute(request);
        return ResponseEntity.created(URI.create("/catalog/colors/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    ResponseEntity<ColorResponse> rename(@PathVariable Long id, @RequestBody @Valid ColorRequest request) {
        return ResponseEntity.ok(renameColor.execute(id, request));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteColor.execute(id);
        return ResponseEntity.noContent().build();
    }
}
