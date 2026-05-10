# Workflow Spec

How features move from idea to implementation in this project.
Reference this when writing a PRD or breaking one into tasks.

---

## Document hierarchy

```
docs/project-spec.md          ← what the system is and why it exists
docs/prd/{phase}.md           ← what a phase delivers, in product language
docs/tasks/{phase}-{N}.md     ← how to implement it, scoped for one AI session
spec/architecture.md          ← implementation rules for new bounded contexts
spec/testing.md               ← integration test patterns
spec/workflow.md              ← this file
```

Each layer answers a different question. Never mix them — a PRD that contains
Java snippets or a task file that discusses business rationale is a signal the
layers have blurred.

---

## Layer 1 — Project spec

`docs/project-spec.md` is the single source of truth for the overall system:
modules, bounded contexts, phasing, and high-level decisions. It changes rarely.
When a PRD contradicts it, the spec wins — update the PRD.

---

## Layer 2 — PRD

One file per phase or feature. Audience: product owner, AI task writer.

### What belongs in a PRD

- **Goal** — one paragraph on the business outcome
- **Scope** — in scope / out of scope (explicit)
- **Domain model** — entities, fields, relationships in plain language or tables;
  mention types (`BigDecimal`, `LocalDate`) when they matter for correctness
- **REST API** — endpoints, request/response shape, HTTP status expectations
- **Domain events** — name, payload, when published, who consumes
- **Acceptance criteria** — testable, checkbox format; one criterion per observable behavior

### Language

Write for a product reader. Use terms from the business domain, not implementation
details. Technical hints are allowed when they prevent an important mistake
(e.g. "`implantationQty` is not persisted — it is carried only in the request and
forwarded via the domain event"), but implementation choices (class names, annotations,
query strategies) belong in the task file, not the PRD.

### What does not belong in a PRD

- Package names, class names, method signatures
- JPA annotations, Flyway SQL, Painless scripts
- Test code or test strategy
- Internal architecture decisions already covered by `spec/architecture.md`

---

## Layer 3 — Task files

One task file = one AI execution session. The agent reads the task file, the PRD
it references, CLAUDE.md, and the spec files. It should not need to read anything
else to complete the work.

### Size rule

A task file is the right size when an agent can complete it without losing track
of decisions made in earlier sections. In practice:

- **One Flyway migration** + its domain classes + repositories = one task
- **Write-side use cases + controllers + tests** for a module = one task
- **Query module** (Elasticsearch listeners, read use cases, read controllers) = one task

If a task file has more than ~8 sections or spans both a write side and a read side,
split it.

### Dependency declaration

Every task file must open with its dependency, if any:

```
Depends on: docs/tasks/phase-02c-1-domain.md
```

An agent must not start a task whose dependency is not complete.

### Naming

```
docs/tasks/{phase}-{sequence}-{slug}.md
```

Examples:
```
docs/tasks/phase-02c-1-domain.md
docs/tasks/phase-02c-2-catalog-api.md
docs/tasks/phase-02c-3-query-module.md
```

Sequence numbers make the execution order explicit. Phases with a single task
file skip the sequence number (`docs/tasks/phase-02a-reference-data.md`).

### Structure of a task file

```markdown
# Tasks — {Phase}: {Title}

{Two-sentence context — what this task builds and why it exists in the system.}

Depends on: {path or "none"}

---

## 1. {Layer or concern}

- [ ] {Concrete deliverable — one file, one class, one migration}
  - {Sub-bullet for non-obvious detail only}
```

Each checkbox is one deliverable: a file created, a method added, a migration written.
Sub-bullets are for invariants, guards, or decisions that an implementer might otherwise
get wrong — not for restating what the class name already says.

### What does not belong in a task file

- Business rationale (belongs in the PRD)
- Architecture rules already in `spec/architecture.md` (e.g. "use `@Getter` only",
  "no public setters") — do not repeat them; the agent reads the spec
- Copy-pasted acceptance criteria from the PRD — the task file is about implementation,
  not acceptance

### Verification section

Every task file ends with a verification checklist:

```markdown
## N. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass
- [ ] `ModularStructureTest` passes
- [ ] `GET /docs` — new endpoints visible in OpenAPI
```

Add phase-specific checks (e.g. "ProductSkuCreated event structure is stable") only
when there is a real cross-phase contract to protect.

---

## Splitting a PRD into task files

Cut along natural seams — boundaries where one piece of work can be completed and
verified before the next begins.

| Common seam | Example |
|---|---|
| Domain before API | Flyway + entities + events before use cases + controllers |
| Write side before read side | Catalog write endpoints before query module |
| Core before extensions | Base CRUD before guard updates in other modules |

Avoid splitting in the middle of a layer (e.g. half the use cases in one task,
half in the next) — it creates unclear ownership and makes verification harder.

When a task adds behaviour to a module created in a previous phase (e.g. adding
existence guards to reference-data controllers), put it at the end of the dependent
task file, not in a separate file. Only split when the added work is large enough
to risk overrunning the session.
