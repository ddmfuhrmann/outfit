# Backlog — post-MVP ideas

Captures ideas and improvements that came up during development but are out of scope for the MVP defined in `docs/project-spec.md`. No priority or deadline commitment — the goal is to preserve the reasoning while it is fresh.

Add items freely. When an idea matures into a concrete requirement, migrate it to the spec or its own PRD.

---

## General / cross-cutting

- **API docs** — write comprehensive usage descriptions for all endpoints, including business and product context (e.g. when to call an endpoint, what preconditions apply, what downstream effects it triggers). Goal: a developer or integrator should understand the business intent from the OpenAPI spec alone, without reading source code.

## Party

<!-- improvements for customers, suppliers, sellers, addresses -->

## Catalog

- **Tax rate management** — analyze a simplified tax rate setup targeting small businesses. Current model ties tax directly to the product; evaluate whether a standalone tax rate entity (e.g. `TaxRate` with rate %, tax type, and optional fiscal code) would reduce repetition and make compliance updates easier without touching every product.

## Inventory

<!-- improvements for the inventory module -->

## Sales

<!-- improvements for the sales flow -->

## Purchasing

<!-- improvements for purchases and payables -->

## Finance

<!-- improvements for cash register and receivables -->

## Query / analytics

<!-- new projections, reports, dashboards -->

## Fiscal

<!-- improvements for NFC-e / NF-e emission -->

## Frontend

- **Tech stack** — frontend must use Claude Design system and Vue.js.
