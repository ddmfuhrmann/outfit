package github.io.ddmfuhrmann.outfit.sales.api.rest;

import github.io.ddmfuhrmann.outfit.sales.application.dto.CreateSaleRequest;
import github.io.ddmfuhrmann.outfit.sales.application.dto.SaleResponse;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.CreateSaleUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/sales")
public class SaleController {

    private final CreateSaleUseCase createSale;

    public SaleController(CreateSaleUseCase createSale) {
        this.createSale = createSale;
    }

    @PostMapping
    @Operation(summary = "Create a sale", description = "Creates a new direct sale or records the sale from a closed consignment.")
    ResponseEntity<SaleResponse> create(@RequestBody @Valid CreateSaleRequest request) {
        var response = createSale.execute(request);
        return ResponseEntity.created(URI.create("/sales/" + response.id())).body(response);
    }
}
