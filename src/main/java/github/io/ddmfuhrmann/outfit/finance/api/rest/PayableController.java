package github.io.ddmfuhrmann.outfit.finance.api.rest;

import github.io.ddmfuhrmann.outfit.finance.application.dto.PayPayableRequest;
import github.io.ddmfuhrmann.outfit.finance.application.dto.PayableResponse;
import github.io.ddmfuhrmann.outfit.finance.application.usecase.PayPayableUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payables")
public class PayableController {

    private final PayPayableUseCase payPayable;

    public PayableController(PayPayableUseCase payPayable) {
        this.payPayable = payPayable;
    }

    @PostMapping("/{id}/payments")
    ResponseEntity<PayableResponse> recordPayment(@PathVariable Long id,
                                                   @RequestBody PayPayableRequest request) {
        return ResponseEntity.ok(payPayable.execute(id, request.amount()));
    }
}
