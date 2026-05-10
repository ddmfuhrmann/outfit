package github.io.ddmfuhrmann.outfit.catalog.api.rest;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.ColorRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.ColorResponse;
import github.io.ddmfuhrmann.outfit.catalog.application.usecase.*;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/catalog/colors")
public class ColorController {

    private final CreateColorUseCase createColor;
    private final GetColorUseCase getColor;
    private final ListColorsUseCase listColors;
    private final RenameColorUseCase renameColor;
    private final DeleteColorUseCase deleteColor;

    public ColorController(CreateColorUseCase createColor, GetColorUseCase getColor,
                           ListColorsUseCase listColors, RenameColorUseCase renameColor,
                           DeleteColorUseCase deleteColor) {
        this.createColor = createColor;
        this.getColor = getColor;
        this.listColors = listColors;
        this.renameColor = renameColor;
        this.deleteColor = deleteColor;
    }

    @PostMapping
    ResponseEntity<ColorResponse> create(@RequestBody @Valid ColorRequest request) {
        ColorResponse created = createColor.execute(request);
        return ResponseEntity.created(URI.create("/catalog/colors/" + created.id())).body(created);
    }

    @GetMapping
    ResponseEntity<PageResponse<ColorResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listColors.execute(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    ResponseEntity<ColorResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(getColor.execute(id));
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
