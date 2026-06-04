package github.io.ddmfuhrmann.outfit.sales.api.rest;

import github.io.ddmfuhrmann.outfit.sales.application.dto.CommissionBonusTierResponse;
import github.io.ddmfuhrmann.outfit.sales.application.dto.CreateCommissionBonusTierRequest;
import github.io.ddmfuhrmann.outfit.sales.application.dto.UpdateCommissionBonusTierRequest;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.CreateCommissionBonusTierUseCase;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.DeactivateCommissionBonusTierUseCase;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.ListCommissionBonusTiersUseCase;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.UpdateCommissionBonusTierUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/commission-bonus-tiers")
public class CommissionBonusTierController {

    private final CreateCommissionBonusTierUseCase createTier;
    private final UpdateCommissionBonusTierUseCase updateTier;
    private final DeactivateCommissionBonusTierUseCase deactivateTier;
    private final ListCommissionBonusTiersUseCase listTiers;

    public CommissionBonusTierController(CreateCommissionBonusTierUseCase createTier,
                                         UpdateCommissionBonusTierUseCase updateTier,
                                         DeactivateCommissionBonusTierUseCase deactivateTier,
                                         ListCommissionBonusTiersUseCase listTiers) {
        this.createTier = createTier;
        this.updateTier = updateTier;
        this.deactivateTier = deactivateTier;
        this.listTiers = listTiers;
    }

    @PostMapping
    @Operation(summary = "Create a commission bonus tier")
    ResponseEntity<CommissionBonusTierResponse> create(@RequestBody @Valid CreateCommissionBonusTierRequest request) {
        var response = createTier.execute(request);
        return ResponseEntity.created(URI.create("/commission-bonus-tiers/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "List active commission bonus tiers")
    ResponseEntity<List<CommissionBonusTierResponse>> list() {
        return ResponseEntity.ok(listTiers.execute());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a commission bonus tier")
    ResponseEntity<CommissionBonusTierResponse> update(@PathVariable Long id,
                                                        @RequestBody @Valid UpdateCommissionBonusTierRequest request) {
        return ResponseEntity.ok(updateTier.execute(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a commission bonus tier")
    ResponseEntity<Void> deactivate(@PathVariable Long id) {
        deactivateTier.execute(id);
        return ResponseEntity.noContent().build();
    }
}
