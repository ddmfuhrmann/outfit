# Tasks — Phase 4d: Commission API

Implements the commission use cases, REST controllers, and wires `CreateCommissionsFromSaleUseCase` into `CreateSaleUseCase`. Also completes the `CreateSaleUseCase` placeholder injection that was deferred from phase 4b-2.

Depends on: docs/tasks/phase-04d-1-domain.md, docs/tasks/phase-04b-2-api.md

---

## 1. DTOs

In `sales/application/dto/`.

- [ ] `SellerCommissionResponse.java` — `record SellerCommissionResponse(Long id, Long saleId, Long salespersonId, BigDecimal sharePercent, BigDecimal commissionPercent, BigDecimal baseAmount, BigDecimal earnedAmount, BigDecimal pendingAmount, BigDecimal bonusAmount, BigDecimal totalAmount, String status)` with `static from(SellerCommission)`
- [ ] `CommissionBonusTierResponse.java` — `record CommissionBonusTierResponse(Long id, BigDecimal lowerBound, BigDecimal upperBound, BigDecimal bonusAmount, boolean active)` with `static from(CommissionBonusTier)`
- [ ] `CreateCommissionBonusTierRequest.java` — `record CreateCommissionBonusTierRequest(@NotNull @PositiveOrZero BigDecimal lowerBound, @NotNull @Positive BigDecimal upperBound, @NotNull @Positive BigDecimal bonusAmount) {}`
- [ ] `UpdateCommissionBonusTierRequest.java` — `record UpdateCommissionBonusTierRequest(@NotNull @PositiveOrZero BigDecimal lowerBound, @NotNull @Positive BigDecimal upperBound, @NotNull @Positive BigDecimal bonusAmount) {}`

---

## 2. Use cases

In `sales/application/usecase/`.

- [ ] `CreateCommissionsFromSaleUseCase.java` — `@Service @Transactional`
  - `void execute(Sale sale)`
  - Looks up `CommissionBonusTierRepository.findActiveMatchingTier(sale.getNetAmount())` → `bonusAmount = tier.getBonusAmount()` or `BigDecimal.ZERO` if absent
  - For each `SaleSeller` entry in `sale.getSellers()`:
    - Creates `SellerCommission.create(sale.getId(), seller.salespersonId(), seller.sharePercent(), seller.commissionPercent(), sale.getNetAmount(), installmentSnapshots, bonusAmount)`
    - Calls `sellerCommissionRepository.save(commission)`
  - `installmentSnapshots` are built from `sale.getInstallments()` as `SaleInstallmentSnapshot` records

- [ ] Wire `CreateCommissionsFromSaleUseCase` into `CreateSaleUseCase` (placeholder from phase 4b-2)
  - Add constructor injection of `CreateCommissionsFromSaleUseCase` to `CreateSaleUseCase`
  - Replace the placeholder call with `createCommissionsUseCase.execute(sale)`

- [ ] `GetCommissionUseCase.java` — `@Service` (no `@Transactional`)
  - Loads by ID; throws `EntityNotFoundException` if absent; returns `SellerCommissionResponse.from(commission)`

- [ ] `ListCommissionsUseCase.java` — `@Service` (no `@Transactional`)
  - Filters: `salespersonId`, `status` (parsed to `CommissionStatus`), `from` (sale date ≥, via JOIN on `sale`), `to`, `Pageable`
    - For date filtering: join `SellerCommission.saleId` → `Sale.saleDate` via `Specification`; alternatively add a `saleDate` denormalized field to `SellerCommission` at creation time for simpler filtering
  - Returns `PageResponse<SellerCommissionResponse>`

- [ ] `CreateCommissionBonusTierUseCase.java` — `@Service @Transactional`
  - Guard overlap: `CommissionBonusTierRepository.existsOverlappingActiveTier(request.lowerBound(), request.upperBound(), -1L)` → if true, `IllegalStateException("bonus tier range overlaps with an existing active tier")`
  - Creates `CommissionBonusTier.create(...)`, saves
  - Logs `INFO`
  - Returns `CommissionBonusTierResponse.from(tier)`

- [ ] `UpdateCommissionBonusTierUseCase.java` — `@Service @Transactional`
  - Loads tier (404 if absent); guard overlap excluding self: `existsOverlappingActiveTier(lower, upper, tier.getId())` → `IllegalStateException` if true
  - Calls `tier.update(...)` (JPA dirty checking handles persistence)
  - Returns `CommissionBonusTierResponse.from(tier)`

- [ ] `DeactivateCommissionBonusTierUseCase.java` — `@Service @Transactional`
  - Loads tier (404 if absent); calls `tier.deactivate()` (JPA dirty checking)
  - Logs `INFO`

- [ ] `ListCommissionBonusTiersUseCase.java` — `@Service` (no `@Transactional`)
  - Returns all tiers (active and inactive) as `List<CommissionBonusTierResponse>`

---

## 3. Controllers

In `sales/api/rest/`.

- [ ] `CommissionController.java` — `@RestController @RequestMapping("/commissions")`
  - `GET /commissions/{id}` → `GetCommissionUseCase` → `200 OK`
  - `GET /commissions` → `ListCommissionsUseCase` (query params: `salespersonId`, `status`, `from`, `to`, `Pageable`) → `200 OK`
  - OpenAPI `@Operation` on each method

- [ ] `CommissionBonusTierController.java` — `@RestController @RequestMapping("/commission-bonus-tiers")`
  - `POST /commission-bonus-tiers` → `CreateCommissionBonusTierUseCase` → `201 Created` + `Location` header
  - `GET /commission-bonus-tiers` → `ListCommissionBonusTiersUseCase` → `200 OK`
  - `PUT /commission-bonus-tiers/{id}` → `UpdateCommissionBonusTierUseCase` → `200 OK`
  - `DELETE /commission-bonus-tiers/{id}` → `DeactivateCommissionBonusTierUseCase` → `204 No Content`
  - OpenAPI `@Operation` on each method

---

## 4. Integration tests

In `src/test/java/.../outfit/sales/CommissionControllerIT.java` — extends `AbstractIT`.

- [ ] `cashSaleWithOneSellerCreatesEarnedCommission` — POST /sales (CASH, 1 seller) → GET /commissions?salespersonId=X → 1 record, `status = EARNED`, `earnedAmount = netAmount × commissionPercent / 100`, `pendingAmount = 0`
- [ ] `installmentOnlySaleCreatesPendingCommission` — POST /sales (INSTALLMENT-only) → `status = PENDING`, `earnedAmount = 0`
- [ ] `mixedPaymentSaleCreatesPartialCommission` — POST /sales (half CASH, half INSTALLMENT) → `status = PARTIAL`; `earnedAmount` and `pendingAmount` each ≈ half of commission base
- [ ] `saleWithTwoSellersCreatesTwoCommissionRecords` — POST /sales (2 sellers, CASH) → two `SellerCommission` records, each `sharePercent = 50`; each `baseAmount = netAmount × 0.5`
- [ ] `saleInBonusTierRangeAppliesBonusAmount` — create a bonus tier, POST /sales with netAmount in range → commission has correct `bonusAmount`
- [ ] `saleOutsideBonusTierRangeHasZeroBonus` — no tier matches → `bonusAmount = 0`
- [ ] `createOverlappingBonusTierReturns422` — create two tiers with overlapping ranges → second one returns 422
- [ ] `deactivateBonusTierReturns204` — DELETE /commission-bonus-tiers/{id} → 204; subsequent sale does not apply that tier

---

## 5. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass
- [ ] `ModularStructureTest` passes
- [ ] `GET /docs` — commission and bonus tier endpoints visible in OpenAPI
