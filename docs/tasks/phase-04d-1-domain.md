# Tasks — Phase 4d: Commission Domain

Builds the `SellerCommission` aggregate and the `CommissionBonusTier` reference entity. `SellerCommission` encapsulates all commission math: it partitions the sale's installments into immediate and deferred, computes earned and pending amounts, and applies the matching bonus tier.

Depends on: docs/tasks/phase-04b-1-domain.md

---

## 1. Flyway migration

- [ ] Create `V14__commission_schema.sql`:
  ```sql
  CREATE TABLE commission_bonus_tier (
      id              BIGINT          NOT NULL,
      lower_bound     NUMERIC(15,2)   NOT NULL,
      upper_bound     NUMERIC(15,2)   NOT NULL,
      bonus_amount    NUMERIC(15,2)   NOT NULL,
      active          BOOLEAN         NOT NULL DEFAULT TRUE,
      created_at      TIMESTAMPTZ     NOT NULL,
      updated_at      TIMESTAMPTZ     NOT NULL,
      CONSTRAINT pk_commission_bonus_tier PRIMARY KEY (id)
  );

  CREATE TABLE seller_commission (
      id                  BIGINT          NOT NULL,
      sale_id             BIGINT          NOT NULL,
      salesperson_id      BIGINT          NOT NULL,
      share_percent       NUMERIC(5,2)    NOT NULL,
      commission_percent  NUMERIC(5,2)    NOT NULL,
      base_amount         NUMERIC(15,2)   NOT NULL,
      earned_amount       NUMERIC(15,2)   NOT NULL,
      pending_amount      NUMERIC(15,2)   NOT NULL,
      bonus_amount        NUMERIC(15,2)   NOT NULL,
      total_amount        NUMERIC(15,2)   NOT NULL,
      status              VARCHAR(20)     NOT NULL,
      created_at          TIMESTAMPTZ     NOT NULL,
      updated_at          TIMESTAMPTZ     NOT NULL,
      CONSTRAINT pk_seller_commission PRIMARY KEY (id)
  );
  ```

---

## 2. Domain model

All in `sales/domain/model/`.

- [ ] `CommissionStatus.java` — enum `EARNED`, `PARTIAL`, `PENDING`

- [ ] `CommissionBonusTier.java` — extends `BaseAggregate<CommissionBonusTier>`
  - Fields: `lowerBound`, `upperBound`, `bonusAmount`, `active`
  - `static CommissionBonusTier.create(BigDecimal lowerBound, BigDecimal upperBound, BigDecimal bonusAmount)` — guards: `lowerBound` not null and not negative; `upperBound` not null; `lowerBound.compareTo(upperBound) >= 0` → `IllegalArgumentException("lowerBound must be less than upperBound")`; `bonusAmount` not null and not negative; sets `active = true`
  - `void update(BigDecimal lowerBound, BigDecimal upperBound, BigDecimal bonusAmount)` — same guards as create; JPA dirty checking handles persistence
  - `void deactivate()` — guard: `!active` → `IllegalStateException("commission bonus tier is already inactive")`; sets `active = false`
  - `boolean matches(BigDecimal amount)` — returns `active && lowerBound.compareTo(amount) <= 0 && upperBound.compareTo(amount) >= 0`

- [ ] `SellerCommission.java` — extends `BaseAggregate<SellerCommission>`
  - Fields: `saleId`, `salespersonId`, `sharePercent`, `commissionPercent`, `baseAmount`, `earnedAmount`, `pendingAmount`, `bonusAmount`, `totalAmount`, `status` (`@Enumerated(EnumType.STRING)`)
  - `static SellerCommission.create(Long saleId, Long salespersonId, BigDecimal sharePercent, BigDecimal commissionPercent, BigDecimal saleNetAmount, List<SaleInstallmentSnapshot> installments, BigDecimal bonusAmount)` — computes all amounts:
    1. `baseAmount = saleNetAmount.multiply(sharePercent).divide(BigDecimal.valueOf(100), 2, HALF_UP)`
    2. `immediateTotal` = sum of `installment.amount()` where `!PaymentModality.valueOf(installment.paymentModality()).isDeferred()`
    3. `deferredTotal` = sum of `installment.amount()` where `PaymentModality.valueOf(installment.paymentModality()).isDeferred()`
    4. `installmentTotal` = `immediateTotal + deferredTotal` (= `saleNetAmount`)
    5. `commissionBase = baseAmount.multiply(commissionPercent).divide(BigDecimal.valueOf(100), 2, HALF_UP)`
    6. `earnedAmount = commissionBase.multiply(immediateTotal).divide(installmentTotal, 2, HALF_UP)` (0 if `installmentTotal` is 0)
    7. `pendingAmount = commissionBase.multiply(deferredTotal).divide(installmentTotal, 2, HALF_UP)`
    8. `this.bonusAmount = bonusAmount` (0 if no tier matched)
    9. `totalAmount = earnedAmount.add(pendingAmount).add(bonusAmount)`
    10. `status`: all immediate → `EARNED`; all deferred → `PENDING`; mixed → `PARTIAL`

---

## 3. Repositories

In `sales/domain/repository/`.

- [ ] `CommissionBonusTierRepository.java` — `extends JpaRepository<CommissionBonusTier, Long>`
  - `List<CommissionBonusTier> findByActiveTrue()`
  - `@Query("SELECT t FROM CommissionBonusTier t WHERE t.active = true AND t.lowerBound <= :amount AND t.upperBound >= :amount") Optional<CommissionBonusTier> findActiveMatchingTier(@Param("amount") BigDecimal amount)`
  - `@Query("SELECT COUNT(t) > 0 FROM CommissionBonusTier t WHERE t.active = true AND t.id <> :excludeId AND t.lowerBound <= :upper AND t.upperBound >= :lower") boolean existsOverlappingActiveTier(@Param("lower") BigDecimal lower, @Param("upper") BigDecimal upper, @Param("excludeId") Long excludeId)`
    - For create (no existing ID to exclude), pass `excludeId = -1L` so the condition is never satisfied

- [ ] `SellerCommissionRepository.java` — `extends JpaRepository<SellerCommission, Long>`
  - `Page<SellerCommission> findAll(Specification<SellerCommission> spec, Pageable pageable)`

---

## 4. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass
- [ ] `ModularStructureTest` passes
