package github.io.ddmfuhrmann.outfit.query;

import github.io.ddmfuhrmann.outfit.purchasing.application.dto.AddPayableRequest;
import github.io.ddmfuhrmann.outfit.purchasing.application.dto.PurchaseResponse;
import github.io.ddmfuhrmann.outfit.purchasing.domain.model.PurchaseStatus;
import github.io.ddmfuhrmann.outfit.query.application.dto.PurchaseDocument;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseQueryControllerIT extends AbstractPurchaseQueryIT {

    private static final LocalDate PURCHASE_DATE = LocalDate.of(2025, Month.AUGUST, 1);

    private PurchaseSetup setupPurchase(HttpHeaders headers, String suffix) {
        return setupPurchase(headers, suffix, PURCHASE_DATE);
    }

    @Test
    void getById_existingPurchase_returns200() {
        var headers = authHeaders(rest);
        var setup = setupPurchase(headers, "GBE");

        var resp = rest.exchange("/purchases/" + setup.purchaseId(), HttpMethod.GET,
                new HttpEntity<>(headers), PurchaseDocument.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().id()).isEqualTo(setup.purchaseId());
        assertThat(resp.getBody().status()).isEqualTo("OPEN");
        assertThat(resp.getBody().brandId()).isEqualTo(setup.brandId());
    }

    @Test
    void getById_unknownId_returns404() {
        var headers = authHeaders(rest);
        var resp = rest.exchange("/purchases/9999999999", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void search_byStatus_returnsFilteredResults() {
        var headers = authHeaders(rest);
        var setup = setupPurchase(headers, "SBS");

        var purchase = purchaseRepository.findWithLinesByBrandIdAndPurchaseDateAndStatus(
                setup.brandId(), setup.purchaseDate(), PurchaseStatus.OPEN).get();
        BigDecimal total = purchase.getLines().stream()
                .map(l -> l.getUnitCost().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        rest.exchange("/purchases/" + setup.purchaseId() + "/payables", HttpMethod.POST,
                new HttpEntity<>(new AddPayableRequest(LocalDate.of(2026, Month.SEPTEMBER, 1), total), headers),
                PurchaseResponse.class);
        rest.exchange("/purchases/" + setup.purchaseId() + "/confirm", HttpMethod.POST,
                new HttpEntity<>(headers), PurchaseResponse.class);

        var resp = rest.exchange("/purchases?status=CONFIRMED&size=100", HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<PurchaseDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().content()).anyMatch(d -> d.id().equals(setup.purchaseId()));
        assertThat(resp.getBody().content()).allMatch(d -> "CONFIRMED".equals(d.status()));
    }

    @Test
    void search_bySupplierId_returnsFilteredResults() {
        var headers = authHeaders(rest);
        var setup = setupPurchase(headers, "SSID");

        var resp = rest.exchange("/purchases?supplierId=" + setup.supplierId() + "&size=100", HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<PurchaseDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().content()).isNotEmpty();
        assertThat(resp.getBody().content()).allMatch(d -> setup.supplierId().equals(d.supplierId()));
    }

    @Test
    void search_byBrandId_returnsFilteredResults() {
        var headers = authHeaders(rest);
        var setup = setupPurchase(headers, "SBID");

        var resp = rest.exchange("/purchases?brandId=" + setup.brandId() + "&size=100", HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<PurchaseDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().content()).isNotEmpty();
    }

    @Test
    void search_noFilter_returnsPageResponse() {
        var headers = authHeaders(rest);
        setupPurchase(headers, "SNF");

        var resp = rest.exchange("/purchases?page=0&size=10", HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<PurchaseDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().content()).isNotNull();
        assertThat(resp.getBody().totalElements()).isGreaterThan(0);
        assertThat(resp.getBody().page()).isEqualTo(0);
        assertThat(resp.getBody().size()).isEqualTo(10);
        assertThat(resp.getBody().totalPages()).isGreaterThan(0);
    }
}
