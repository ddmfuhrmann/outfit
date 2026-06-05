package github.io.ddmfuhrmann.outfit.query;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import github.io.ddmfuhrmann.outfit.purchasing.application.dto.AddPayableRequest;
import github.io.ddmfuhrmann.outfit.purchasing.application.dto.PurchaseResponse;
import github.io.ddmfuhrmann.outfit.purchasing.domain.model.PurchaseStatus;
import github.io.ddmfuhrmann.outfit.query.application.dto.PurchaseDocument;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseProjectionIT extends AbstractPurchaseQueryIT {

    @Autowired
    ElasticsearchClient esClient;

    private static final LocalDate PURCHASE_DATE = LocalDate.of(2025, Month.JULY, 1);

    private PurchaseSetup setupPurchase(HttpHeaders headers, String suffix) {
        return setupPurchase(headers, suffix, PURCHASE_DATE);
    }

    private PurchaseDocument fetchFromEs(Long purchaseId) throws Exception {
        var response = esClient.get(
                g -> g.index(ElasticsearchIndexInitializer.INDEX_PURCHASES).id(purchaseId.toString()),
                PurchaseDocument.class);
        assertThat(response.found()).isTrue();
        return response.source();
    }

    @Test
    void purchaseOpened_createsEsDocumentWithStatusOpen() throws Exception {
        var headers = authHeaders(rest);
        var setup = setupPurchase(headers, "PO");

        var doc = fetchFromEs(setup.purchaseId());

        assertThat(doc.status()).isEqualTo("OPEN");
        assertThat(doc.brandId()).isEqualTo(setup.brandId());
        assertThat(doc.supplierId()).isEqualTo(setup.supplierId());
    }

    @Test
    void purchaseUpdated_fromAddLine_updatesEsDocumentWithLine() throws Exception {
        var headers = authHeaders(rest);
        var setup = setupPurchase(headers, "PAL");

        var doc = fetchFromEs(setup.purchaseId());

        assertThat(doc.lines()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(doc.lines().getFirst().productSkuId()).isNotNull();
        assertThat(doc.lines().getFirst().quantity()).isGreaterThan(0);
        assertThat(doc.lines().getFirst().unitCost()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void purchaseUpdated_fromAddPayable_updatesEsDocumentWithPayable() throws Exception {
        var headers = authHeaders(rest);
        var setup = setupPurchase(headers, "PAP");

        rest.exchange("/purchases/" + setup.purchaseId() + "/payables", HttpMethod.POST,
                new HttpEntity<>(new AddPayableRequest(LocalDate.of(2026, Month.AUGUST, 1), BigDecimal.valueOf(50.00)), headers),
                PurchaseResponse.class);

        var doc = fetchFromEs(setup.purchaseId());

        assertThat(doc.payables()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void purchaseConfirmed_updatesEsDocumentToConfirmed() throws Exception {
        var headers = authHeaders(rest);
        var setup = setupPurchase(headers, "PC");

        var purchase = purchaseRepository.findWithLinesByBrandIdAndPurchaseDateAndStatus(
                setup.brandId(), setup.purchaseDate(), PurchaseStatus.OPEN).get();
        BigDecimal total = purchase.getLines().stream()
                .map(l -> l.getUnitCost().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        rest.exchange("/purchases/" + setup.purchaseId() + "/payables", HttpMethod.POST,
                new HttpEntity<>(new AddPayableRequest(LocalDate.of(2026, Month.AUGUST, 1), total), headers),
                PurchaseResponse.class);
        rest.exchange("/purchases/" + setup.purchaseId() + "/confirm", HttpMethod.POST,
                new HttpEntity<>(headers), PurchaseResponse.class);

        var doc = fetchFromEs(setup.purchaseId());

        assertThat(doc.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void purchaseCancelled_updatesEsDocumentToCancelled() throws Exception {
        var headers = authHeaders(rest);
        var setup = setupPurchase(headers, "PCA");

        rest.exchange("/purchases/" + setup.purchaseId() + "/cancel", HttpMethod.POST,
                new HttpEntity<>(headers), PurchaseResponse.class);

        var doc = fetchFromEs(setup.purchaseId());

        assertThat(doc.status()).isEqualTo("CANCELLED");
    }

    @Test
    void purchaseUpdated_fromRemovePayable_updatesPayablesInEs() throws Exception {
        var headers = authHeaders(rest);
        var setup = setupPurchase(headers, "PRP");

        var addResp = rest.exchange("/purchases/" + setup.purchaseId() + "/payables", HttpMethod.POST,
                new HttpEntity<>(new AddPayableRequest(LocalDate.of(2026, Month.AUGUST, 1), BigDecimal.valueOf(50.00)), headers),
                PurchaseResponse.class);
        Long payableId = addResp.getBody().payables().getFirst().id();

        rest.exchange("/purchases/" + setup.purchaseId() + "/payables/" + payableId,
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        var doc = fetchFromEs(setup.purchaseId());

        assertThat(doc.payables()).isEmpty();
    }
}
