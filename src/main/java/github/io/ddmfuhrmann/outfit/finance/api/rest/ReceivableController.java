package github.io.ddmfuhrmann.outfit.finance.api.rest;

import github.io.ddmfuhrmann.outfit.finance.application.dto.PayReceivableRequest;
import github.io.ddmfuhrmann.outfit.finance.application.dto.ReceivableResponse;
import github.io.ddmfuhrmann.outfit.finance.application.usecase.PayReceivableUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/receivables")
public class ReceivableController {

    private final PayReceivableUseCase payReceivable;

    public ReceivableController(PayReceivableUseCase payReceivable) {
        this.payReceivable = payReceivable;
    }

    @PostMapping("/{id}/payments")
    ResponseEntity<ReceivableResponse> recordPayment(@PathVariable Long id,
                                                      @RequestBody PayReceivableRequest request) {
        return ResponseEntity.ok(payReceivable.execute(id, request.amount()));
    }
}
