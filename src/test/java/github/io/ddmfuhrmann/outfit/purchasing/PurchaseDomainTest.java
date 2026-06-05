package github.io.ddmfuhrmann.outfit.purchasing;

import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchaseCancelled;
import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchaseConfirmed;
import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchaseOpened;
import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchaseUpdated;
import github.io.ddmfuhrmann.outfit.purchasing.domain.model.Purchase;
import github.io.ddmfuhrmann.outfit.purchasing.domain.model.PurchaseStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PurchaseDomainTest {

    private static final Long BRAND_ID = 1L;
    private static final Long SUPPLIER_ID = 2L;
    private static final LocalDate PURCHASE_DATE = LocalDate.of(2025, Month.JANUARY, 10);
    private static final Long SKU_ID = 10L;

    private Purchase openPurchase() {
        return Purchase.create(BRAND_ID, SUPPLIER_ID, PURCHASE_DATE, "obs");
    }

    // --- create ---

    @Test
    void createSetsStatusOpen() {
        var purchase = openPurchase();
        assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.OPEN);
    }

    // --- addLine ---

    @Test
    void addLine_onOpenPurchase_succeedsAndIncreasesSize() {
        var purchase = openPurchase();
        purchase.addLine(SKU_ID, 2, BigDecimal.valueOf(50.00));
        assertThat(purchase.getLines()).hasSize(1);
    }

    @Test
    void addLine_onConfirmedPurchase_throwsIllegalState() {
        var purchase = openPurchase();
        purchase.addLine(SKU_ID, 1, BigDecimal.valueOf(100.00));
        purchase.addPayable(LocalDate.of(2025, Month.FEBRUARY, 1), BigDecimal.valueOf(100.00));
        purchase.confirm();

        assertThatThrownBy(() -> purchase.addLine(SKU_ID, 1, BigDecimal.valueOf(100.00)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addLine_withZeroQuantity_throwsIllegalArgument() {
        var purchase = openPurchase();
        assertThatThrownBy(() -> purchase.addLine(SKU_ID, 0, BigDecimal.valueOf(50.00)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addLine_withNegativeUnitCost_throwsIllegalArgument() {
        var purchase = openPurchase();
        assertThatThrownBy(() -> purchase.addLine(SKU_ID, 1, BigDecimal.valueOf(-1.00)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- addPayable ---

    @Test
    void addPayable_onOpenPurchase_succeeds() {
        var purchase = openPurchase();
        purchase.addPayable(LocalDate.of(2025, Month.FEBRUARY, 1), BigDecimal.valueOf(200.00));
        assertThat(purchase.getPayables()).hasSize(1);
    }

    @Test
    void addPayable_onCancelledPurchase_throwsIllegalState() {
        var purchase = openPurchase();
        purchase.cancel();
        assertThatThrownBy(() -> purchase.addPayable(LocalDate.of(2025, Month.FEBRUARY, 1), BigDecimal.valueOf(100.00)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addPayable_withZeroAmount_throwsIllegalArgument() {
        var purchase = openPurchase();
        assertThatThrownBy(() -> purchase.addPayable(LocalDate.of(2025, Month.FEBRUARY, 1), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- removePayable ---

    @Test
    void removePayable_withUnknownId_throwsIllegalArgument() {
        var purchase = openPurchase();
        assertThatThrownBy(() -> purchase.removePayable(999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- confirm ---

    @Test
    void confirm_withNoLines_throwsIllegalState() {
        var purchase = openPurchase();
        assertThatThrownBy(purchase::confirm)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void confirm_withNoPayables_throwsIllegalState() {
        var purchase = openPurchase();
        purchase.addLine(SKU_ID, 1, BigDecimal.valueOf(100.00));
        assertThatThrownBy(purchase::confirm)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void confirm_withPayablesSumMismatch_throwsIllegalState() {
        var purchase = openPurchase();
        purchase.addLine(SKU_ID, 2, BigDecimal.valueOf(50.00));  // total = 100.00
        purchase.addPayable(LocalDate.of(2025, Month.FEBRUARY, 1), BigDecimal.valueOf(200.00)); // off by 100.00
        assertThatThrownBy(purchase::confirm)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void confirm_withMatchingPayablesTotal_setsStatusConfirmed() {
        var purchase = openPurchase();
        purchase.addLine(SKU_ID, 2, BigDecimal.valueOf(50.00));  // total = 100.00
        purchase.addPayable(LocalDate.of(2025, Month.FEBRUARY, 1), BigDecimal.valueOf(100.00));
        purchase.confirm();
        assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.CONFIRMED);
    }

    @Test
    void confirm_registersEventWithCorrectSnapshotData() {
        var purchase = openPurchase();
        purchase.addLine(SKU_ID, 3, BigDecimal.valueOf(40.00));  // total = 120.00
        purchase.addPayable(LocalDate.of(2025, Month.FEBRUARY, 1), BigDecimal.valueOf(120.00));
        purchase.confirm();

        var events = purchase.getRegisteredEvents().stream()
                .filter(PurchaseConfirmed.class::isInstance)
                .toList();
        assertThat(events).hasSize(1);

        var event = (PurchaseConfirmed) events.getFirst();
        assertThat(event.brandId()).isEqualTo(BRAND_ID);
        assertThat(event.supplierId()).isEqualTo(SUPPLIER_ID);
        assertThat(event.purchaseDate()).isEqualTo(PURCHASE_DATE);
        assertThat(event.lines()).hasSize(1);
        assertThat(event.lines().getFirst().productSkuId()).isEqualTo(SKU_ID);
        assertThat(event.lines().getFirst().quantity()).isEqualTo(3);
        assertThat(event.lines().getFirst().unitCost()).isEqualByComparingTo(BigDecimal.valueOf(40.00));
        assertThat(event.payables()).hasSize(1);
        assertThat(event.payables().getFirst().payableId()).isNotNull();
        assertThat(event.payables().getFirst().amount()).isEqualByComparingTo(BigDecimal.valueOf(120.00));
    }

    @Test
    void confirm_onAlreadyConfirmedPurchase_throwsIllegalState() {
        var purchase = openPurchase();
        purchase.addLine(SKU_ID, 1, BigDecimal.valueOf(100.00));
        purchase.addPayable(LocalDate.of(2025, Month.FEBRUARY, 1), BigDecimal.valueOf(100.00));
        purchase.confirm();
        assertThatThrownBy(purchase::confirm)
                .isInstanceOf(IllegalStateException.class);
    }

    // --- cancel ---

    @Test
    void cancel_onOpenPurchase_setsStatusCancelled() {
        var purchase = openPurchase();
        purchase.cancel();
        assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.CANCELLED);
    }

    @Test
    void cancel_onConfirmedPurchase_throwsIllegalState() {
        var purchase = openPurchase();
        purchase.addLine(SKU_ID, 1, BigDecimal.valueOf(100.00));
        purchase.addPayable(LocalDate.of(2025, Month.FEBRUARY, 1), BigDecimal.valueOf(100.00));
        purchase.confirm();
        assertThatThrownBy(purchase::cancel)
                .isInstanceOf(IllegalStateException.class);
    }

    // --- PurchaseOpened event ---

    @Test
    void create_registersOpenedEvent() {
        var purchase = Purchase.create(BRAND_ID, SUPPLIER_ID, PURCHASE_DATE, "initial obs");

        var events = purchase.getRegisteredEvents().stream()
                .filter(PurchaseOpened.class::isInstance)
                .toList();
        assertThat(events).hasSize(1);

        var event = (PurchaseOpened) events.getFirst();
        assertThat(event.purchaseId()).isEqualTo(purchase.getId());
        assertThat(event.brandId()).isEqualTo(BRAND_ID);
        assertThat(event.supplierId()).isEqualTo(SUPPLIER_ID);
        assertThat(event.purchaseDate()).isEqualTo(PURCHASE_DATE);
        assertThat(event.observations()).isEqualTo("initial obs");
    }

    // --- PurchaseUpdated event from addLine ---

    @Test
    void addLine_registersUpdatedEvent() {
        var purchase = openPurchase();
        purchase.addLine(SKU_ID, 3, BigDecimal.valueOf(25.00));

        var events = purchase.getRegisteredEvents().stream()
                .filter(PurchaseUpdated.class::isInstance)
                .toList();
        assertThat(events).hasSize(1);

        var event = (PurchaseUpdated) events.getFirst();
        assertThat(event.lines()).hasSize(1);
        assertThat(event.lines().getFirst().productSkuId()).isEqualTo(SKU_ID);
        assertThat(event.lines().getFirst().quantity()).isEqualTo(3);
        assertThat(event.lines().getFirst().unitCost()).isEqualByComparingTo(BigDecimal.valueOf(25.00));
    }

    // --- PurchaseUpdated event from addPayable ---

    @Test
    void addPayable_registersUpdatedEvent() {
        var purchase = openPurchase();
        purchase.addLine(SKU_ID, 1, BigDecimal.valueOf(100.00));
        purchase.addPayable(LocalDate.of(2025, Month.MARCH, 1), BigDecimal.valueOf(100.00));

        var events = purchase.getRegisteredEvents().stream()
                .filter(PurchaseUpdated.class::isInstance)
                .map(e -> (PurchaseUpdated) e)
                .toList();
        var last = events.getLast();
        assertThat(last.payables()).hasSize(1);
        assertThat(last.payables().getFirst().amount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    // --- PurchaseUpdated event from removePayable ---

    @Test
    void removePayable_registersUpdatedEvent() {
        var purchase = openPurchase();
        purchase.addLine(SKU_ID, 1, BigDecimal.valueOf(100.00));
        purchase.addPayable(LocalDate.of(2025, Month.MARCH, 1), BigDecimal.valueOf(100.00));

        var payableId = purchase.getPayables().getFirst().getId();
        purchase.removePayable(payableId);

        var events = purchase.getRegisteredEvents().stream()
                .filter(PurchaseUpdated.class::isInstance)
                .map(e -> (PurchaseUpdated) e)
                .toList();
        assertThat(events.getLast().payables()).isEmpty();
    }

    // --- PurchaseCancelled event ---

    @Test
    void cancel_registersCancelledEvent() {
        var purchase = openPurchase();
        purchase.cancel();

        var events = purchase.getRegisteredEvents().stream()
                .filter(PurchaseCancelled.class::isInstance)
                .toList();
        assertThat(events).hasSize(1);

        var event = (PurchaseCancelled) events.getFirst();
        assertThat(event.purchaseId()).isEqualTo(purchase.getId());
    }

    // --- updateObservations ---

    @Test
    void updateObservations_onOpenPurchase_updatesAndRegistersEvent() {
        var purchase = openPurchase();
        purchase.updateObservations("new obs");

        assertThat(purchase.getObservations()).isEqualTo("new obs");

        var events = purchase.getRegisteredEvents().stream()
                .filter(PurchaseUpdated.class::isInstance)
                .map(e -> (PurchaseUpdated) e)
                .toList();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().observations()).isEqualTo("new obs");
    }

    @Test
    void updateObservations_onConfirmedPurchase_throwsIllegalState() {
        var purchase = openPurchase();
        purchase.addLine(SKU_ID, 1, BigDecimal.valueOf(100.00));
        purchase.addPayable(LocalDate.of(2025, Month.FEBRUARY, 1), BigDecimal.valueOf(100.00));
        purchase.confirm();

        assertThatThrownBy(() -> purchase.updateObservations("late obs"))
                .isInstanceOf(IllegalStateException.class);
    }
}
