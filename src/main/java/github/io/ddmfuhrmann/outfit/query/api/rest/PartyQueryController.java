package github.io.ddmfuhrmann.outfit.query.api.rest;

import github.io.ddmfuhrmann.outfit.query.application.dto.PartyDocument;
import github.io.ddmfuhrmann.outfit.query.application.usecase.GetPartyByIdUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.SearchPartiesUseCase;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/party")
public class PartyQueryController {

    private final SearchPartiesUseCase searchParties;
    private final GetPartyByIdUseCase getPartyById;

    public PartyQueryController(SearchPartiesUseCase searchParties, GetPartyByIdUseCase getPartyById) {
        this.searchParties = searchParties;
        this.getPartyById = getPartyById;
    }

    @GetMapping
    @Operation(summary = "Search parties",
            description = "Served from Elasticsearch. Reflects committed state synchronously — domain events are indexed before the write response is sent.")
    ResponseEntity<PageResponse<PartyDocument>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(searchParties.execute(q, role, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get party by ID",
            description = "Served from Elasticsearch. Reflects committed state synchronously — domain events are indexed before the write response is sent.")
    ResponseEntity<PartyDocument> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getPartyById.execute(id));
    }
}
