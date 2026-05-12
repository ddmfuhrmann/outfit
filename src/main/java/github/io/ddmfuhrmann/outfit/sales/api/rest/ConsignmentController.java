package github.io.ddmfuhrmann.outfit.sales.api.rest;

import github.io.ddmfuhrmann.outfit.sales.application.dto.CloseConsignmentRequest;
import github.io.ddmfuhrmann.outfit.sales.application.dto.ConsignmentResponse;
import github.io.ddmfuhrmann.outfit.sales.application.dto.IssueConsignmentRequest;
import github.io.ddmfuhrmann.outfit.sales.application.dto.ReturnItemsRequest;
import github.io.ddmfuhrmann.outfit.sales.application.dto.SaleResponse;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.CloseConsignmentUseCase;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.IssueConsignmentUseCase;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.ReturnConsignmentItemsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/consignments")
public class ConsignmentController {

    private final IssueConsignmentUseCase issueConsignment;
    private final ReturnConsignmentItemsUseCase returnConsignmentItems;
    private final CloseConsignmentUseCase closeConsignment;

    public ConsignmentController(IssueConsignmentUseCase issueConsignment,
                                 ReturnConsignmentItemsUseCase returnConsignmentItems,
                                 CloseConsignmentUseCase closeConsignment) {
        this.issueConsignment = issueConsignment;
        this.returnConsignmentItems = returnConsignmentItems;
        this.closeConsignment = closeConsignment;
    }

    @PostMapping
    @Operation(summary = "Issue a consignment", description = "Creates a new consignment and decrements stock for each item.")
    ResponseEntity<ConsignmentResponse> issue(@RequestBody @Valid IssueConsignmentRequest request) {
        var response = issueConsignment.execute(request);
        return ResponseEntity.created(URI.create("/consignments/" + response.id())).body(response);
    }

    @PostMapping("/{id}/return-items")
    @Operation(summary = "Return consignment items", description = "Records a partial or full return of items from an open consignment.")
    ResponseEntity<ConsignmentResponse> returnItems(@PathVariable Long id,
                                                    @RequestBody @Valid ReturnItemsRequest request) {
        return ResponseEntity.ok(returnConsignmentItems.execute(id, request));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a consignment", description = "Closes the consignment and creates the corresponding sale for sold items.")
    ResponseEntity<SaleResponse> close(@PathVariable Long id,
                                       @RequestBody @Valid CloseConsignmentRequest request) {
        var sale = closeConsignment.execute(id, request);
        return ResponseEntity.created(URI.create("/consignments/" + id)).body(sale);
    }
}
