package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.sales.application.dto.SellerCommissionResponse;
import github.io.ddmfuhrmann.outfit.sales.domain.model.CommissionStatus;
import github.io.ddmfuhrmann.outfit.sales.domain.model.SellerCommission;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.SellerCommissionRepository;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class ListCommissionsUseCase {

    private final SellerCommissionRepository repository;

    public ListCommissionsUseCase(SellerCommissionRepository repository) {
        this.repository = repository;
    }

    public record ListCommissionsQuery(
            Long salespersonId,
            CommissionStatus status,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {}

    public PageResponse<SellerCommissionResponse> execute(ListCommissionsQuery query) {
        var spec = buildSpec(query);
        var pageable = PageRequest.of(query.page(), query.size());
        var page = repository.findAll(spec, pageable);
        return PageResponse.from(page.map(SellerCommissionResponse::from));
    }

    private Specification<SellerCommission> buildSpec(ListCommissionsQuery query) {
        var specs = new java.util.ArrayList<Specification<SellerCommission>>();

        if (query.salespersonId() != null)
            specs.add((root, cq, cb) -> cb.equal(root.get("salespersonId"), query.salespersonId()));
        if (query.status() != null)
            specs.add((root, cq, cb) -> cb.equal(root.get("status"), query.status()));
        if (query.from() != null)
            specs.add((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("saleDate"), query.from()));
        if (query.to() != null)
            specs.add((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("saleDate"), query.to()));

        return Specification.allOf(specs);
    }
}
