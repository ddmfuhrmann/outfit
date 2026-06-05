package github.io.ddmfuhrmann.outfit.purchasing.api.rest;

import github.io.ddmfuhrmann.outfit.purchasing.application.dto.AddPayableRequest;
import github.io.ddmfuhrmann.outfit.purchasing.application.dto.PurchaseResponse;
import github.io.ddmfuhrmann.outfit.purchasing.application.dto.UpdateObservationsRequest;
import github.io.ddmfuhrmann.outfit.purchasing.application.usecase.AddPayableUseCase;
import github.io.ddmfuhrmann.outfit.purchasing.application.usecase.CancelPurchaseUseCase;
import github.io.ddmfuhrmann.outfit.purchasing.application.usecase.ConfirmPurchaseUseCase;
import github.io.ddmfuhrmann.outfit.purchasing.application.usecase.RemovePayableUseCase;
import github.io.ddmfuhrmann.outfit.purchasing.application.usecase.UpdateObservationsUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchases")
public class PurchaseController {

    private final AddPayableUseCase addPayableUseCase;
    private final RemovePayableUseCase removePayableUseCase;
    private final ConfirmPurchaseUseCase confirmUseCase;
    private final CancelPurchaseUseCase cancelUseCase;
    private final UpdateObservationsUseCase updateObservationsUseCase;

    public PurchaseController(AddPayableUseCase addPayableUseCase,
                              RemovePayableUseCase removePayableUseCase,
                              ConfirmPurchaseUseCase confirmUseCase,
                              CancelPurchaseUseCase cancelUseCase,
                              UpdateObservationsUseCase updateObservationsUseCase) {
        this.addPayableUseCase = addPayableUseCase;
        this.removePayableUseCase = removePayableUseCase;
        this.confirmUseCase = confirmUseCase;
        this.cancelUseCase = cancelUseCase;
        this.updateObservationsUseCase = updateObservationsUseCase;
    }

    @PostMapping("/{id}/payables")
    ResponseEntity<PurchaseResponse> addPayable(@PathVariable Long id,
                                                @Valid @RequestBody AddPayableRequest req) {
        return ResponseEntity.ok(addPayableUseCase.execute(id, req.dueDate(), req.amount()));
    }

    @DeleteMapping("/{id}/payables/{payableId}")
    ResponseEntity<Void> removePayable(@PathVariable Long id, @PathVariable Long payableId) {
        removePayableUseCase.execute(id, payableId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm")
    ResponseEntity<PurchaseResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(confirmUseCase.execute(id));
    }

    @PostMapping("/{id}/cancel")
    ResponseEntity<PurchaseResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(cancelUseCase.execute(id));
    }

    @PutMapping("/{id}/observations")
    ResponseEntity<PurchaseResponse> updateObservations(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateObservationsRequest req) {
        return ResponseEntity.ok(updateObservationsUseCase.execute(id, req.observations()));
    }
}
