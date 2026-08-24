# Loqal Product Requirements Document (PRD)

| | |
|---|---|
| **Product** | Loqal — Self-Hostable Commerce Backend Platform |
| **Version** | 2.0 (Modular Monolith era) |
| **Date** | 2026-08-24 |
| **Status** | Draft for implementation |
| **Related docs** | `docs/superpowers/specs/2026-08-24-monolith-migration-design.md`, `docs/superpowers/plans/2026-08-24-modular-monolith-migration.md` |

---

## Table of Contents

1. [Vision](#1-vision)
2. [Problem Statement](#2-problem-statement)
3. [Goals and Non-Goals](#3-goals-and-non-goals)
4. [Personas](#4-personas)
5. [Product Overview](#5-product-overview)
6. [Scope](#6-scope)
7. [Functional Requirements — v1 Modules](#7-functional-requirements--v1-modules)
8. [Functional Requirements — Phase 2 Modules](#8-functional-requirements--phase-2-modules)
9. [API Surface](#9-api-surface)
10. [Data Model Overview](#10-data-model-overview)
11. [Architecture Requirements](#11-architecture-requirements)
12. [Non-Functional Requirements](#12-non-functional-requirements)
13. [Explicitly Out of Scope](#13-explicitly-out-of-scope)
14. [Phased Roadmap](#14-phased-roadmap)
15. [Success Metrics](#15-success-metrics)
16. [Risks and Mitigations](#16-risks-and-mitigations)
17. [Decisions Log](#17-decisions-log)
18. [Glossary](#18-glossary)
19. [Appendix A — Pre-Migration Codebase Snapshot](#appendix-a--pre-migration-codebase-snapshot)
20. [Appendix B — Event Schemas (Kafka Payloads)](#appendix-b--event-schemas-kafka-payloads)
21. [Appendix C — Environment Variable Reference](#appendix-c--environment-variable-reference)
22. [Appendix D — Module API Contracts](#appendix-d--module-api-contracts)
23. [Appendix E — Order State Machine Formal Spec](#appendix-e--order-state-machine-formal-spec)
24. [Appendix F — Orientation for AI Assistants](#appendix-f--orientation-for-ai-assistants)

---

## 1. Vision

Loqal is a **self-hostable, multi-tenant commerce backend platform**. A merchant
downloads Loqal, runs one command (`docker compose up`), and has a complete,
production-grade online-store backend: catalog, inventory, ordering, payments,
and identity — exposed through a clean HTTP API behind an API gateway.

Loqal is deliberately **backend-only**. There is no first-party website or mobile
app. Any client — the merchant's own storefront, a freelance developer's build,
or a future first-party frontend — consumes the same documented API.

The backend is built as a **modular monolith**: one deployable application whose
internal modules have compile-enforced boundaries. When a single deployment hits
its scaling limits, any module can be extracted into its own service without a
rewrite, because every cross-module interaction already flows through published
interfaces and message-broker events.

> **One-line pitch:** "Self-host Shopify's backend, not Shopify."

---

## 2. Problem Statement

Small local businesses and startups face three compounding problems:

1. **Hosted SaaS platforms are expensive and rent-seeking.** Monthly fees scale
   with success, data lives in someone else's database, and customization is
   capped at what the vendor exposes.
2. **Building from scratch is out of reach.** A solo founder or small shop cannot
   stand up payments, inventory correctness, and order lifecycles themselves.
3. **Existing open-source commerce stacks are monoliths of the wrong kind** —
   tangled codebases where the only path to scale is a full rewrite.

Loqal occupies the gap: **open, self-hosted, own-your-data commerce backend**
that is simple to run as one container today, yet decomposable into services
tomorrow.

---

## 3. Goals and Non-Goals

### 3.1 Goals

| # | Goal |
|---|---|
| G1 | A merchant can self-host the entire platform with `docker compose up` and a `.env` file |
| G2 | All functionality is reachable through a versioned HTTP API gated by Kong |
| G3 | Order placement is safe under concurrency: no oversell, no double-charge, no duplicate order from retries |
| G4 | Module boundaries are compiler-enforced; extracting a module into a service requires no logic changes |
| G5 | Single deployable artifact; zero runtime coupling between modules except DB/broker/cache infrastructure |
| G6 | Every async flow survives process restarts (transactional outbox; consumer idempotency) |
| G7 | Auth keys persist across restarts — issued tokens remain valid |
| G8 | The repo builds green from scratch with `mvn clean verify` and has no manual setup steps beyond Docker + `.env` |

### 3.2 Non-Goals

| # | Non-Goal | Rationale |
|---|---|---|
| NG1 | First-party web storefront, dashboard UI, or mobile apps | Backend-only vision; frontends may come later |
| NG2 | Horizontal auto-scaling within one deployment | Modular monolith targets single-node self-hosting; scale path = module extraction |
| NG3 | Kubernetes-first operation | docker-compose is the supported v1 path; K8s manifests are a later concern |
| NG4 | Multi-region / geo-distributed deployments | Out of proportion for target users |
| NG5 | Plugin marketplace / third-party extension system | YAGNI until core loop is proven |
| NG6 | Real-time chat, calls, delivery tracking at v1 | Phase 2 modules; see §8, §14 |

---

## 4. Personas

### P1 — Merchant ("Priya", owns a neighborhood store)
Self-hosts Loqal on a $10 VPS or a box in her back office. Wants: products online,
customers ordering, money landing in her Razorpay account. Technical depth: can
edit a `.env` file and run `docker compose up`; will not tune JVM flags.
**Success:** store is taking online orders within one afternoon of setup.

### P2 — Developer-integrator ("Arjun", freelancer/agencies)
Builds storefronts and custom clients on top of platforms. Reads OpenAPI docs,
wants predictable REST semantics, JWT auth, webhook events, and sane error codes.
**Success:** integrates checkout against the API without reading source code.

### P3 — Shop staff / dispatcher (future, Phase 2)
Works at the counter, receives incoming orders, packs items, hands parcels to
delivery agents. Uses a client built on Loqal's dispatch APIs.
**Success:** processes an order from ping to handover without phone calls.

### P4 — Delivery agent (future, Phase 2)
Picks up assigned deliveries, navigates, marks delivered. Interacts through a
client using agent APIs.
**Success:** knows exactly what to pick up and where, minimal friction.

### P5 — Platform operator (future, Phase 2)
Runs a hosted instance of Loqal serving many merchants (the SaaS mode).
Onboards merchants, monitors health, manages global configuration.
**Success:** operates many tenants without direct DB access.

---

## 5. Product Overview

```
                        ┌──────────────────────────────┐
   Clients (any)        │         KONG (DB-less)       │
   storefronts, apps ───▶  routes, JWT verification    │
                        └──────────────┬───────────────┘
                                       │ :8000 → :8080
                        ┌──────────────▼───────────────┐
                        │      loqal-platform (app)    │
                        │  ┌────────────────────────┐  │
                        │  │ modules/identity       │  │
                        │  │ modules/catalog        │  │
                        │  │ modules/orders         │  │
                        │  │ modules/payments       │  │
                        │  └───┬───────────┬────────┘  │
                        └──────┼───────────┼───────────┘
                     Kafka topics │      │ Redis (idempotency)
                        ┌─────────▼──┐ ┌─▼───────┐
                        │  POSTGRES  │ │  REDIS  │
                        └────────────┘ └─────────┘
```

- **One JVM process** serves all REST APIs, consumes and produces Kafka events,
  and talks to Postgres (R2DBC) and Redis (reactive).
- **Kong** terminates external traffic, verifies JWT signatures (RS256), and
  routes by path prefix.
- **Kafka** carries inter-module async events (order saga), preserving the seam
  needed if modules become services later.
- **Redis** stores order idempotency keys.
- **Postgres** holds all module tables in one database, namespaced per module.

---

## 6. Scope

### 6.1 In Scope — v1 (this PRD's normative sections: §7, §9–§12)

| Area | Included |
|---|---|
| Identity | Email/password auth, Google OAuth, JWT RS256 issuance with persistent keys, refresh tokens, roles, profiles, addresses, tenant scoping |
| Catalog | Products, categories, per-merchant listings, stock levels, reservation/reversion |
| Orders | Full lifecycle state machine, concurrency-safe stock coordination via Kafka saga, transactional outbox, idempotent order creation |
| Payments | Razorpay integration (order creation, signed webhook capture, refunds), payment records |
| Platform plumbing | Kong routing + JWT verification, docker-compose deployment, OpenAPI docs, health endpoints, structured logging |
| Multi-tenancy | Tenant isolation enforced at API layer via token claims (`tenant_id`); all merchant-scoped queries filter by tenant |

### 6.2 In Scope — Phase 2 (requirements sketched in §8, specified later in their own PRDs)

Promotion engine · Delivery & fulfillment · Platform administration (merchant
onboarding, admin management, audit logs, stats) · Communication (notifications,
real-time chat).

### 6.3 Out of Scope — everything else

See §13 for the exhaustive, explicit list.

---

## 7. Functional Requirements — v1 Modules

Requirements use RFC-2119 language (**MUST / SHOULD / MAY**). Each requirement
has an ID for traceability.

### 7.1 Identity Module (`modules/identity`)

#### 7.1.1 Credentials & Login

- **ID-101** — The system MUST allow registration with email + password.
  Passwords MUST be hashed with BCrypt (cost ≥ 10). Email MUST be unique per
  instance (globally unique across tenants, since email identifies the account).
- **ID-102** — The system MUST reject password-less registration and passwords
  shorter than 8 characters.
- **ID-103** — Login with valid credentials MUST return: access token (JWT
  RS256, default TTL 1h), refresh token (opaque stored token, default TTL 7d),
  and the user's id, roles, and `tenant_id`.
- **ID-104** — Failed logins MUST return `401` with a generic error body that
  does not reveal whether the email exists.
- **ID-105** — Google OAuth MUST be supported for both server-web flow
  (authorization-code redirect) and mobile flow (code exchange endpoint). On
  first Google login the system MUST provision a user record automatically.
- **ID-106** — Access tokens MUST carry claims: `sub` (email), `user_id`,
  `tenant_id`, `roles`, `token_type=access`.
- **ID-107** — Signing keys MUST be loaded from configuration
  (`JWT_RSA_PRIVATE_KEY` / `JWT_RSA_PUBLIC_KEY`, base64 PKCS#8 / X.509 SPKI).
  The system MUST fail fast at startup if they are absent or unparseable. Keys
  MUST NOT be regenerated at runtime. *(Fixes the pre-migration ephemeral-key bug.)*
- **ID-108** — The JWKS endpoint `/.well-known/jwks.json` MUST expose the public
  key with a stable `kid`; Kong's pinned key MUST match it.
- **ID-109** — Refresh token rotation: exchanging a valid refresh token MUST
  issue a new access+refresh pair and revoke the used refresh token. Reuse of a
  revoked refresh token MUST return `401`.
- **ID-110** — Phone-number login SHOULD be reserved as a schema field and
  extension point in v1; SMS OTP verification itself is **out of scope v1** (see §13).

#### 7.1.2 Profiles, Roles, Addresses

- **ID-111** — Each user has exactly one profile: display name, phone (optional),
  email (immutable). Profile CRUD MUST be restricted to the authenticated owner.
- **ID-112** — Users MAY store multiple addresses; exactly one MAY be flagged
  default. Address fields: line1, line2, city, state, postal code, country,
  label, lat/lng (optional in v1).
- **ID-113** — Roles MUST include at minimum: `ROLE_USER`, `ROLE_MERCHANT`,
  `ROLE_ADMIN`. Role assignment rules:
  - New registrations get `ROLE_USER` + their own new tenant (see ID-120).
  - Merchant upgrade is triggered by the merchant-onboarding flow (§7.2.4 /
    Phase 2 platform module); v1 exposes the internal upgrade endpoint.
- **ID-114** — All identity endpoints MUST require authentication except:
  `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/google/**`,
  `/.well-known/jwks.json`.

#### 7.1.3 Tenancy

- **ID-120** — Every user belongs to exactly one **tenant**. A tenant represents
  a merchant business. When a user registers as a customer, the system MUST
  create a personal tenant for them (so any user can later become a merchant
  without migration).
- **ID-121** — `tenant_id` MUST be present in every access token and MUST be
  propagated internally to catalog/order queries for scoping.
- **ID-122** — Cross-tenant data access MUST be impossible via the public API:
  any query parameter attempting to read another tenant's resources MUST be
  ignored or rejected (`403`).

### 7.2 Catalog Module (`modules/catalog`)

#### 7.2.1 Products & Categories

- **CAT-101** — Merchants MUST be able to create products scoped to their
  `tenant_id`. Required fields: name, price (minor units integer, e.g., paise),
  currency (ISO-4217, defaults instance-wide to INR), category, SKU (unique per
  tenant). Optional: description, images (URL list), attributes (JSON), tax rate,
  active flag.
- **CAT-102** — Products MUST support soft delete (`active=false`) — never hard
  deletion once referenced by an order.
- **CAT-103** — Public browsing endpoints MUST expose only `active=true`
  products of the requested tenant; merchants MUST see their inactive products
  via merchant-scoped endpoints.
- **CAT-104** — Categories MUST be flat strings in v1 (no tree/hierarchy).
  Category CRUD is merchant-scoped; slugs unique per tenant.
- **CAT-105** — Price updates MUST NOT retroactively alter placed orders; orders
  snapshot unit price at purchase time (owned by the orders module).

#### 7.2.2 Inventory / Stock

- **CAT-110** — Each product has an integer `stock_quantity`. Negative stock is
  invalid.
- **CAT-111** — Stock reservation MUST be atomic and safe under concurrent
  orders: implemented via pessimistic row lock inside the reservation
  transaction. If available stock < requested, the whole reservation MUST fail
  (no partial reservations).
- **CAT-112** — Reservation requests MUST be idempotent: each carries an
  event/request id; duplicates MUST be detected via the `processed_events` table
  and acknowledged without double-decrementing.
- **CAT-113** — Successful/failed reservations MUST emit `stock-reservation-result`
  carrying: original request id, order id, per-item results, and outcome.
- **CAT-114** — Order cancellation events MUST trigger stock reversion
  (restore quantities) unless already reverted (idempotency rule CAT-112 applies).
- **CAT-115** — Poison messages (repeated processing failure) MUST land in the
  DLT topic after retry exhaustion; the service MUST stay healthy and log loudly.

#### 7.2.3 Merchant listing

- **CAT-120** — Merchant-facing endpoints MUST allow: list own products
  (paginated), update product, deactivate product, adjust stock directly.
  Direct stock adjustment MUST be audit-logged (timestamp, delta, actor) — log
  entry in v1, dedicated audit table in Phase 2 platform module.

### 7.3 Orders Module (`modules/orders`)

#### 7.3.1 Lifecycle

- **ORD-100** — Order states: `CREATED → PENDING_STOCK → STOCK_RESERVED →
  PAYMENT_PENDING → PAID → CONFIRMED`, with terminal/failure paths
  `CANCELLED`, `FAILED`, plus `PAYMENT_FAILED`, `STOCK_FAILED` as transient
  failure states feeding `FAILED` or retry. The exact machine:
  ```
  CREATED ──▶ PENDING_STOCK ──▶ STOCK_RESERVED ──▶ PAYMENT_PENDING ──▶ PAID ──▶ CONFIRMED
     │             │                    │                  │              │
     └─────────────┴──── CANCELLED ◀────┴──────────────────┘              │
                   (user cancel allowed before CONFIRMED)                 │
  STOCK_FAILED ─▶ FAILED          PAYMENT_FAILED ─▶ FAILED (or retry)
  ```
- **ORD-101** — Only transitions defined above MUST be accepted; any other
  transition attempt MUST raise a domain error and leave state unchanged.
- **ORD-102** — An order contains ≥ 1 item: product id, quantity, **snapshot
  unit price**, currency. Order totals are computed server-side; client-supplied
  totals MUST be ignored.

#### 7.3.2 Order creation flow

- **ORD-110** — Creating an order MUST: validate items exist & are active in the
  caller's tenant → persist order `CREATED` → write `order-creation-requested`
  to the **transactional outbox in the same DB transaction** → relay publishes
  to Kafka.
- **ORD-111** — Order creation MUST accept an optional `Idempotency-Key` header.
  If present, the key is checked/set in Redis (TTL 24h): replay returns the
  original response (cached) instead of creating a second order.
- **ORD-112** — Price lookup for item snapshots MUST call the catalog module
  through the in-process `ProductApi` interface (never HTTP).
- **ORD-113** — After `STOCK_RESERVED`, orders module MUST synchronously request
  payment creation from the payments module via `PaymentApi` and move the order
  to `PAYMENT_PENDING`.

#### 7.3.3 Saga reactions

- **ORD-120** — On `stock-reservation-result`: success → advance state;
  failure → mark `STOCK_FAILED` then `FAILED`, notify via event.
- **ORD-121** — On `payment-completed`: mark `PAID` → `CONFIRMED`.
- **ORD-122** — On user cancellation before `CONFIRMED`: publish `order-cancel`;
  payments emits refund if paid; catalog reverts stock. Cancel after `CONFIRMED`
  MUST be rejected in v1 (returns-as-refund flow is Phase 2).
- **ORD-123** — Every consumed event MUST be processed idempotently; replayed
  events MUST NOT double-advance state.

#### 7.3.4 Queries

- **ORD-130** — Customers list their own orders (paginated, newest first);
  merchants list orders containing their tenant's items; both filtered by
  `tenant_id` claim.
- **ORD-131** — Order detail includes items, status history timestamps, payment
  reference ids.

### 7.4 Payments Module (`modules/payments`)

- **PAY-101** — Payment provider for v1 is **Razorpay** exclusively. Provider
  abstraction SHOULD keep the gateway behind an interface so adding providers
  later does not touch callers.
- **PAY-102** — `PaymentApi.createPayment(orderId, amount, currency)` MUST
  create a Razorpay order and a local `payments` row (status `CREATED`),
  returning the Razorpay order id.
- **PAY-103** — Razorpay webhooks (`payment.captured`, `payment.failed`,
  `refund.processed`) MUST be verified by HMAC-SHA256 signature using
  `RAZORPAY_WEBHOOK_SECRET` before any state change; invalid signatures get
  `400` and MUST NOT mutate state.
- **PAY-104** — Verified `payment.captured` MUST set local status `COMPLETED`
  and publish `payment-completed` (order id, payment id, amount). Duplicate
  webhook deliveries MUST be idempotent (unique Razorpay entity id constraint).
- **PAY-105** — Refunds: consuming `refund-requested`, the module calls
  Razorpay refund, records a `refunds` row, and publishes `refund-completed`
  (order id, refund id, amount, outcome) so the orders module can surface
  refund status.
- **PAY-106** — Blocking Razorpay SDK calls MUST execute on virtual threads
  (`Executors.newVirtualThreadPerTaskExecutor()` bridged via
  `Schedulers.fromExecutorService`) with timeout operators; they MUST NOT block
  Netty event loops.
- **PAY-107** — Wallets: schema fields reserved (`wallet_ledger` design note),
  wallet operations are **out of scope v1** (§13).
- **PAY-108** — Payment credentials MUST come from env (`RAZORPAY_KEY_ID`,
  `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`); startup fails fast if missing.

### 7.5 Cross-cutting functional requirements

- **XC-101** — All list endpoints MUST paginate (`page`, `size`, max size 100,
  default 20) and return a stable envelope `{content, page, size, totalElements}`.
- **XC-102** — Error responses MUST follow one envelope:
  `{timestamp, status, error, message, path, traceId}` with correct HTTP codes
  (400 validation, 401 unauthenticated, 403 forbidden, 404 not found, 409
  conflict/state, 422 domain rule violation, 500 unexpected).
- **XC-103** — OpenAPI 3 documentation MUST be served (springdoc) aggregating
  all modules; Swagger UI enabled.
- **XC-104** — Every module boundary crossing uses either a published API
  interface (sync) or a Kafka topic (async). Direct entity/repository access
  across packages MUST NOT occur (enforced by Maven module structure).
- **XC-105** — Health/readiness endpoints (`/actuator/health`, `/health/liveness`,
  `/health/readiness`) MUST reflect DB, Redis, and Kafka connectivity.

---

## 8. Functional Requirements — Phase 2 Modules

These are **directional commitments**, not build-ready specs. Each gets its own
PRD + plan before implementation.

### 8.1 Promotion module

- Coupons (percent/fixed/free-shipping), validity windows, usage limits
  (global/per-user), min-order value, product/category applicability.
- Checkout-time validation via `PromotionApi.validateAndPrice(cart)`; orders
  snapshot applied discount.
- Campaign management endpoints for merchants; admin-level campaigns in
  platform module later.
- **Boundary note:** promotion *rules* live here; promotion *redemption records*
  link to orders (orders module stores applied-discount snapshot).

### 8.2 Delivery module

- Agent profiles, clock-in/out availability windows.
- Assignment engine: manual dispatch first, algorithmic (proximity/load) second;
  geofence-based order surfacing.
- Live location tracking (agent app pushes coordinates), delivery state machine
  (`ASSIGNED → PICKED_UP → IN_TRANSIT → DELIVERED / FAILED`), OTP handover
  verification between dispatcher and agent.
- Google Maps Platform integration for geocoding/routing.
- Extends the orders state machine post-`CONFIRMED` (delivery sub-states).

### 8.3 Platform module

- Merchant onboarding workflow (upgrade user→merchant, store profile, payout
  config), storefront metadata (name, logo, theme tokens for future frontends).
- Admin management: admin users, granular permissions, audit logs.
- Platform stats: GMV, order counts, merchant activity (aggregation queries;
  analytics warehouse is out of this module's scope).

### 8.4 Communication module

- Notification fan-out: email (SMTP/Resend), SMS, push — queue-driven (BullMQ
  experience informs design; likely Kafka consumers in the Java world).
- Template registry with variables; per-event notification preferences.
- Real-time chat (customer↔dispatcher, dispatcher↔agent) over WebSocket with
  presence; message persistence and history pagination.
- Digest buffering for burst-suppression of notifications.

---

## 9. API Surface

Base URL: `http://<host>:8000` (Kong). All bodies JSON. Auth: `Authorization:
Bearer <access-token>` unless noted public.

### 9.1 v1 route map

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | none | Register email/password |
| POST | `/auth/login` | none | Login → tokens |
| POST | `/auth/refresh` | none | Rotate refresh → new pair |
| GET | `/auth/google/callback` | none | OAuth web callback |
| POST | `/auth/oauth/mobile/google` | none | OAuth mobile code exchange |
| GET | `/.well-known/jwks.json` | none | Public signing keys |
| GET/PATCH | `/users/profile` | user | Read/update own profile |
| GET/POST/DELETE | `/users/addresses[/{id}]` | user | Address book |
| GET | `/users/{userId}/profile` | user/admin | Read profile by id (admin/self) |
| POST | `/users/{userId}/upgrade-merchant` | admin/internal | Grant merchant role |
| GET | `/products/public/merchant/{tenantId}` | none | Storefront listing |
| GET | `/products/public/category/{category}` | none | Browse by category |
| GET | `/api/products/{id}` | user | Product detail (tenant-scoped) |
| POST | `/products/merchant` | merchant | Create product |
| PUT/DELETE | `/products/merchant/{productId}` | merchant | Update/deactivate own product |
| GET | `/products/merchant/list` | merchant | List own products |
| PATCH | `/products/merchant/{id}/stock` | merchant | Adjust stock |
| POST | `/api/orders` | user | Create order (supports Idempotency-Key) |
| GET | `/api/orders` | user | Own order history |
| GET | `/api/orders/{id}` | user/merchant | Order detail |
| POST | `/api/orders/{id}/cancel` | user | Cancel (pre-confirmation) |
| GET | `/orders/merchant/{merchantId}` | merchant | Orders containing my items |
| POST | `/api/payments/order` | user | Create Razorpay order for pending payment |
| POST | `/api/payments/webhook` | none* | Razorpay webhook (*HMAC-verified) |
| GET | `/actuator/health` | none | Liveness/readiness |

*(Exact paths may shift ± during implementation; contract tests pin final shapes.)*

### 9.2 Conventions

- Versioning: path-prefix versioning starts when a breaking change ships
  (`/v2/...`); v1 routes initially unversioned for simplicity, frozen at GA.
- Money: integers in minor units + ISO-4217 code. Never floats.
- IDs: UUIDv4 strings everywhere externally.
- Timestamps: UTC ISO-8601.

---

## 10. Data Model Overview

One Postgres database (`loqaldb`). Tables grouped by owning module; modules
never query each other's tables in SQL — only via APIs/events.

| Module | Tables | Key columns |
|---|---|---|
| identity | `users`, `user_credentials`, `refresh_tokens`, `addresses` | users.id (uuid PK), users.tenant_id, credentials.password_hash, refresh_tokens.token_hash + expires_at + revoked |
| identity | `tenants` | id, name, type (personal/business), created_at |
| catalog | `categories`, `products`, `processed_events` | products.tenant_id, sku, price_minor, currency, stock_quantity, active; processed_events.event_id UNIQUE |
| orders | `orders`, `order_items`, `outbox_events` | orders.tenant_id, user_id, status, totals_minor; order_items.order_id FK, product_id, qty, unit_price_minor snapshot; outbox_events.published boolean |
| payments | `payments`, `refunds` | payments.order_id, razorpay_order_id UNIQUE, razorpay_payment_id UNIQUE, status, amount_minor; refunds.payment_id FK |

Indexes (minimum): products(tenant_id, active), orders(tenant_id, user_id,
status), order_items(order_id), payments(razorpay_order_id),
refresh_tokens(token_hash), outbox_events(published, created_at).

Schema evolution: Flyway migrations owned per module (`V<n>__<module>__desc.sql`)
— mandatory before GA; current codebase relies on `ddl-auto` during development
only.

---

## 11. Architecture Requirements

- **AR-1** Build: Maven multi-module, parent = spring-boot-starter-parent 3.5.3,
  Java 21 toolchain, single root Dockerfile (multi-stage, Corretto 21 runtime).
- **AR-2** Stack: WebFlux + R2DBC everywhere; no blocking calls on event loops
  (blocking SDK calls wrapped on virtual-thread executors).
- **AR-3** Module dependency direction: `app → {identity,catalog,orders,payments}
  → shared-contracts`. Enforced by Maven reactor structure.
- **AR-4** Async contract: Kafka topics `order-creation-requested`,
  `stock-reservation-result`, `order-cancel`, `order-cancel-dlt`,
  `payment-completed`, `refund-requested`, `refund-completed`. JSON payloads from
  `shared-contracts`. Topic names configurable via env; constants fix the
  legacy `ORDER_CREATTION_REQUESTED` typo.
- **AR-5** Reliability: transactional outbox for order→Kafka publishes; consumer
  idempotency tables; DLT after retries with exponential backoff.
- **AR-6** Gateway: Kong DB-less declarative config; RS256 JWT verification
  pinned to the persisted public key; rate limiting SHOULD be enabled per-route
  (defaults: 100 req/min/IP) — tuning deferred to deployment docs.
- **AR-7** Config: 12-factor — every environment-specific value via env vars;
  `.env.example` documents all; no secrets in git (the legacy committed RSA key
  in kong.yml is replaced by deployment-generated keys).

---

## 12. Non-Functional Requirements

| Category | Requirement |
|---|---|
| Performance | p95 API latency < 300ms for CRUD reads, < 800ms for order creation (excluding payment round-trip) on a 2 vCPU / 4GB host with embedded infra on separate containers |
| Capacity baseline | Single node comfortably handles 50 req/s mixed traffic and 10k products/tenant without tuning |
| Availability | Restart-safe: in-flight orders resume via outbox/event replay; no manual intervention after crash |
| Security | OWASP ASVS-lite: bcrypt hashes, generic auth errors, HMAC-verified webhooks, no secrets in repo, least-privilege DB user possible in production guide, input validation on all write endpoints |
| Observability | Structured JSON logs with traceId correlation across modules; actuator metrics exposed; Prometheus/Grafana dashboards are Phase 2 nice-to-have |
| Testability | Unit tests per module; Testcontainers integration test covering the full saga; contract stability pinned by API tests |
| Maintainability | No copy-pasted DTOs (single contracts module); package-per-module; public APIs javadoc'd |
| Portability | Runs on linux/amd64 and arm64 Docker hosts |

---

## 13. Explicitly Out of Scope

**Until further notice (not promised in any phase):**

- Any first-party frontend: website, admin dashboard UI, merchant dashboard UI,
  storefront renderer, iOS/Android apps
- Kubernetes manifests/Helm charts, multi-node HA, multi-region
- Plugin/third-party app ecosystem, public developer portal beyond OpenAPI
- Multi-currency pricing per tenant (single instance currency INR v1)
- Tax calculation engines beyond a stored tax-rate field (no jurisdiction logic)
- Internationalization/localization of API error messages (English only v1)
- Search relevance engines (Elasticsearch etc.) — SQL `ILIKE` filtering only
- Media/image upload pipeline — products reference image URLs; storage service TBD
- GDPR/compliance tooling beyond basic data deletion endpoint (Phase 2 candidate)
- Bug bounty/security audit program

**Deferred to Phase 2 (planned, not in v1):**

- Promotions/coupons/discounts (module planned §8.1)
- Delivery: agents, assignment, tracking, geofencing, OTP handover (§8.2)
- Admin/merchant management surfaces: onboarding workflows, permissions UI-APIs,
  audit tables, platform stats (§8.3)
- Notifications (email/SMS/push) and real-time chat/calls (§8.4)
- Phone/SMS OTP login (schema reserved only)
- Wallets and store credit (ledger schema reserved only)
- Post-confirmation returns/refund-request UX (engine-level refunds exist in v1)
- Analytics warehouse, Prometheus/Grafana stack, ELK
- Rate limiting dashboards/tuning guides

**Deleted permanently (decided):**

- Node.js notification-service code (superseded by future Java communication module)
- admin-service and merchant-service stubs (superseded by platform module)
- React frontend scaffold
- Microservice deployment topology for v1

---

## 14. Phased Roadmap

| Phase | Content | Exit criteria |
|---|---|---|
| **1 — Monolith migration (current)** | Tasks 0–10 of the migration plan: skeleton, ports, payments rewrite, app assembly, infra, cleanup, saga IT | `mvn clean verify` green; docker-compose smoke passes; saga IT passes; README rewritten |
| **2a — Hardening** | Flyway migrations replace ddl-auto; rate limiting; structured logging w/ traceIds; load test vs NFR targets; security pass | Load test report meets §12; no P0/P1 findings open |
| **2b — Promotion module** | Per §8.1 spec-to-be-written | Coupon end-to-end in checkout; tests |
| **2c — Delivery module** | Per §8.2 spec-to-be-written | Assignment + OTP handover demoable via API |
| **2d — Platform module** | Per §8.3 spec-to-be-written | Merchant onboarding via API; audit trail exists |
| **2e — Communication module** | Per §8.4 spec-to-be-written | Email fan-out works; chat demoable via WebSocket |
| **3 — Frontier** | K8s manifests, observability stack, additional payment providers, returns flow | Prioritized after Phase 2 learnings |

---

## 15. Success Metrics

| Metric | Target |
|---|---|
| Time-to-running-store (clone → orders flowing) | < 30 minutes incl. image pulls |
| `mvn clean verify` from clean clone | 100% pass, no flaky tests across 3 runs |
| Saga integrity | 0 oversells / 0 double-charges in 10k-order soak test with concurrent duplicates |
| Module extraction rehearsal | One module (catalog) extracted to a separate process behind its existing interfaces in ≤ 1 week spike — proving AR-3/G4 |
| API adoption ergonomics | An external dev completes checkout integration using only OpenAPI docs + `.env.example` (usability test) |

---

## 16. Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Reactive-stack complexity slows contributors | Dev velocity | Virtual threads isolate blocking SDKs; team style guide + examples; contracts module reduces cognitive load |
| Kafka/Zookeeper heavy for tiny self-hosters | Adoption friction | Document "lite profile" path: swap Kafka relay for in-process Spring events behind same interfaces (design keeps this cheap) — evaluated post-v1 |
| Single Postgres becomes bottleneck | Scale ceiling | Acceptable per vision; extraction path exists (AR-3); connection pooling tuned in ops guide |
| Key misconfiguration bricks auth | Downtime | Fail-fast startup with actionable error text; key generation script shipped |
| Razorpay SDK breaking changes | Payments outage | Gateway interface isolates SDK; pin version; contract tests around gateway |
| Scope creep toward frontend | Vision drift | This PRD's §13 is normative; new frontend ideas require amending this document |

---

## 17. Decisions Log

| # | Decision | Date | Rationale |
|---|---|---|---|
| D1 | Backend-only product; no first-party UI/apps | 2026-08-24 | Owner vision; frontends possibly later |
| D2 | Microservices → modular monolith | 2026-08-24 | Self-host simplicity; extraction seams kept (Kafka, interfaces) |
| D3 | WebFlux + R2DBC retained (not servlet/JPA) | 2026-08-24 | 90% of working code uses it; rewrite cost unjustified |
| D4 | Java 21 standard; virtual threads only for blocking SDK bridges | 2026-08-24 | Honest fit for reactive stack; Razorpay SDK benefit |
| D5 | Keep Kong/Kafka/Redis in deployment | 2026-08-24 | Preserve decomposition seams; matches future-scale story |
| D6 | Delete frontend/, backend/node/, admin-, merchant-service | 2026-08-24 | Java-only; stubs superseded |
| D7 | 6 functional modules v1 (identity merged auth+users; catalog merged product+inventory) | 2026-08-24 | Reduce module count while keeping domain coherence |
| D8 | Promotion→catalog-domain, Delivery→orders-domain, admin+merchant+notification+chat grouped | 2026-08-24 | Merge-by-domain-cohesion principle |
| D9 | Hand-written poms over Spring Initializr bootstrap | 2026-08-24 | Proven dependency versions from working services; multi-module from commit #1 |
| D10 | RSA keys persisted via env config | 2026-08-24 | Fixes ephemeral-key bug invalidating tokens & Kong trust |

---

## 18. Glossary

| Term | Meaning |
|---|---|
| **Tenant** | A merchant business (or a personal tenant for customers who haven't onboarded). Isolation boundary for catalog/orders data |
| **Module** | A Maven jar with enforced boundaries: own package root, own tables, published API interfaces |
| **Saga** | The distributed-transaction pattern coordinating order ↔ stock ↔ payment via Kafka events |
| **Outbox** | Table written in the same DB transaction as state change; relayed to Kafka by a poller — guarantees at-least-once publish |
| **Idempotency key** | Client-supplied header making order creation safely retryable |
| **Minor units** | Integer currency representation (₹10.00 → 1000) |
| **DLT** | Dead-letter topic — parking lot for poison messages |
| **JWKS** | JSON Web Key Set — published public keys for JWT verification |
| **Plan A / Plan B** | Migration plan (existing code → monolith) / greenfield Phase 2 module plans |

---

## Appendix A — Pre-Migration Codebase Snapshot

> **Purpose:** preserve complete knowledge of the original system so any
> engineer or AI model can reason about *why* the monolith looks the way it
> does, and where every ported line of code originated.

### A.1 Repository state (as of 2026-08-24)

```
DELETED (git history retains all):
├── frontend/                      # React 19 + Vite scaffold, empty components/pages
├── backend/node/notification-service/   # Express + Socket.io + BullMQ email service (~30 files)
├── backend/java/admin-service/    # 28 classes, servlet/JPA skeleton, shared auth DB
└── backend/java/merchant-service/ # 18 classes, blocking facade over other services' REST APIs

BEING MIGRATED (source → destination):
backend/java/auth-service/     → modules/identity  (...identity.auth)    # 21 classes
backend/java/user-service/     → modules/identity  (...identity.users)   # 12 classes
backend/java/product-service/  → modules/catalog                         # 29 classes
backend/java/order-service/    → modules/orders                          # 37 classes
backend/java/payment-service/  → modules/payments (REWRITTEN)            # 15 classes, was broken

ROOT FILES KEPT & MODIFIED:
docker-compose.yml             # 4 java services + kong/db/redis/kafka/zk → single app
kong.yml                       # routes: /auth /users /products /api/orders; pinned RSA key
README.md                      # rewritten for new vision
```

### A.2 Original service inventory

| Service | Package root | Stack | Tables owned | Kafka | Redis | Security | Notes |
|---|---|---|---|---|---|---|---|
| auth-service | `com.Loqal.authservice` | WebFlux+R2DBC | `user_credentials`, `refresh_tokens` | none | none | issues RS256 JWTs; oauth2-client | JWKS endpoint; Google OAuth web+mobile |
| user-service | `com.Loqal.userservice` | WebFlux+R2DBC | `users` | none | none | resource server | pure CRUD incl. `/internal/users/oauth-register` |
| product-service | `com.Loqal.productservice` | WebFlux+R2DBC | `products`, `processed_events` (+categories) | consumes `order-creation-requested`, `order-cancel`, `order-cancel-dlt`; produces `stock-reservation-result` | none | resource server (`SCOPE_internal-service` on internal paths) | pessimistic-lock stock reservation; circuit-breaker config |
| order-service | `com.Loqal.orderservice` | WebFlux+R2DBC | `orders` (+items), `outbox_events` | produces via outbox relay (`order-creation-requested`, `order-cancel`); consumes `payment-completed`, `stock-reservation-result`; produces `refund-requested` | idempotency keys `idempotency:<key>` TTL 24h | reactive resource server, SCOPE rules | saga orchestrator; Redis idempotency in controller |
| payment-service | `com.loqal.paymentservice` | servlet MVC + JPA/R2DBC mix (**broken**) | `payments`, `refunds` | produces `payment-completed`; consumes `refund-requested` | none | webhook permitAll; dead JWKS URI | Razorpay SDK 1.4.8; could not boot (missing config); TenantConfig commented out |

### A.3 Known defects at migration start (all addressed by this PRD)

1. **Ephemeral JWT keys**: `auth-service/utils/RSAKeyProvider.java` generated a fresh RSA-2048 pair in `@PostConstruct` — every restart invalidated all tokens and broke Kong's pinned public key in `kong.yml`. Fixed by ID-107.
2. **Payment service unbootable**: no kafka/topic/webhook-secret config in `application.yml`; `KafkaConfig.java` hardcoded `localhost:9092`. Superseded by the Task-6 rewrite.
3. **Kong/auth key mismatch**: static RSA key committed in `kong.yml` never matched runtime-generated keys. Fixed by persisted-key deployment flow (AR-7).
4. **Notification service used HS256 symmetric secrets** — incompatible with the RS256 realm. Deleted; future Java communication module will use the standard token infrastructure.
5. **Copy-pasted DTOs**: `OrderEvent`, `StockReservationResponse`, exception classes etc. duplicated across order/product/payment. Unified in `shared-contracts`.
6. **Topic-name typo**: compose env var `ORDER_CREATTION_REQUESTED`. Fixed in `Topics` constants (AR-4).

### A.4 Critical coupling points preserved by design

| Coupling | Old mechanism | New mechanism |
|---|---|---|
| order → product price lookup | `GET {PRODUCT_SERVICE_URL}/api/products/{id}` (OrderService.java ~line 88) | in-process `ProductApi.findPrice()` |
| order → payment creation | `POST {PAYMENT_SERVICE_URL}/api/payments/order` (~line 124) | in-process `PaymentApi.createPayment()` |
| auth → user sync on login/register | `POST {USER_SERVICE_URL}/internal/users/oauth-register` w/ service JWT (`UserServiceClient.java`) | in-process `UsersApi.registerOauthUser()` |
| order ⇄ product stock saga | Kafka topics (unchanged) | Kafka topics (unchanged) |
| order ⇄ payment events | Kafka topics (unchanged) | Kafka topics (unchanged) |
| admin → user/merchant calls | blocking WebClient `.block()` | deleted (recreated in Phase 2 platform module) |

---

## Appendix B — Event Schemas (Kafka Payloads)

All payloads are JSON, serialized from `shared-contracts` classes. Field naming:
lowerCamelCase. Every event carries `eventId` (UUIDv4), `occurredAt` (UTC ISO-8601),
and `tenantId` where applicable. Consumers MUST treat unknown fields as
non-fatal (forward compatibility) and process idempotently keyed on `eventId`.

### B.1 `order-creation-requested`
```json
{
  "eventId": "uuid", "occurredAt": "2026-08-24T10:00:00Z",
  "requestId": "uuid",              // dedup key = outbox event id
  "orderId": "uuid",
  "tenantId": "uuid",               // merchant tenant whose stock is reserved
  "userId": "uuid",                 // buyer
  "items": [
    { "productId": "uuid", "quantity": 2 }
  ]
}
```

### B.2 `stock-reservation-result`
```json
{
  "eventId": "uuid", "occurredAt": "...",
  "requestId": "uuid",              // echoes B.1.requestId
  "orderId": "uuid",
  "success": true,
  "items": [
    { "productId": "uuid", "requestedQty": 2, "reservedQty": 2 }
  ],
  "failureReason": null             // e.g. "INSUFFICIENT_STOCK" | "PRODUCT_INACTIVE"
}
```

### B.3 `order-cancel`
```json
{
  "eventId": "uuid", "occurredAt": "...",
  "orderId": "uuid",
  "tenantId": "uuid",
  "reason": "USER_REQUESTED"        // USER_REQUESTED | PAYMENT_TIMEOUT | SYSTEM
}
```

### B.4 `order-cancel-dlt`
Original `order-cancel` payload plus envelope:
```json
{ "originalPayload": { ... }, "errorClass": "...", "failedAt": "...", "attempts": 3 }
```

### B.5 `payment-completed`
```json
{
  "eventId": "uuid", "occurredAt": "...",
  "orderId": "uuid",
  "paymentId": "uuid",              // local payments.id
  "razorpayPaymentId": "pay_...",   // provider reference, uniqueness enforced
  "amountMinor": 250000,
  "currency": "INR"
}
```

### B.6 `refund-requested`
```json
{
  "eventId": "uuid", "occurredAt": "...",
  "orderId": "uuid",
  "paymentId": "uuid",
  "amountMinor": 250000,
  "reason": "ORDER_CANCELLED"
}
```

### B.7 `refund-completed`
```json
{
  "eventId": "uuid", "occurredAt": "...",
  "orderId": "uuid",
  "refundId": "uuid",
  "razorpayRefundId": "rfnd_...",
  "amountMinor": 250000,
  "status": "PROCESSED"             // PROCESSED | FAILED
}
```

---

## Appendix C — Environment Variable Reference

### C.1 v1 canonical set (documented in `.env.example`)

| Variable | Consumer | Example / format | Notes |
|---|---|---|---|
| `DB_URL` | app (r2dbc) | `r2dbc:postgresql://loqal-db:5432/loqaldb` | single DB, all modules |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | postgres container + app | string | |
| `APP_PORT` | app | `8080` | only exposed port behind Kong |
| `JWT_RSA_PRIVATE_KEY` | identity | base64 PKCS#8 private key | generate: `openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \| openssl pkcs8 -topk8 -nocrypt \| base64 -w0` |
| `JWT_RSA_PUBLIC_KEY` | identity + kong.yml | base64 X.509 SPKI public key | must match private key; same value pinned in kong.yml consumer credential |
| `ACCESS_TOKEN_EXPIRY` | identity | duration, default `1h` | |
| `REFRESH_TOKEN_EXPIRY` | identity | duration, default `168h` (7d) | |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` / `GOOGLE_REDIRECT_URI` | identity | strings | OAuth consent screen required |
| `KAFKA_BOOTSTRAP_SERVERS` | orders/catalog/payments | `kafka:9092` | |
| `TOPIC_ORDER_CREATION_REQUESTED` etc. | modules | topic names | defaults from `Topics` constants; override optional |
| `REDIS_HOST` / `REDIS_PORT` | orders | `redis` / `6379` | idempotency keys |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | payments | strings | Razorpay dashboard |
| `RAZORPAY_WEBHOOK_SECRET` | payments | string | HMAC-SHA256 verification |
| `DDL_AUTO_SETTING` | app (dev only) | `update` dev / unset prod | Flyway replaces before GA (Phase 2a) |

### C.2 Removed legacy variables (do not reintroduce)

`JWT_SECRET` (HS256 remnant) · `STATE_CHECKER_ENABLED` · `USER_SERVICE_URL` ·
`PRODUCT_SERVICE_URL` · `PAYMENT_SERVICE_URL` · per-service port vars
(`ADMIN_SERVER_PORT`, `USER_SERVER_PORT`, `PRODUCT_SERVER_PORT`,
`ORDER_SERVER_PORT`) · `ORDER_CREATTION_REQUESTED` (typo) ·
`STOCK_RESERVATION_RESULT`/`ORDER_CANCEL` as env (now constants with optional overrides).

---

## Appendix D — Module API Contracts

Sync cross-module calls use ONLY these interfaces. Implementations live inside
the owning module; consumers depend on the interface bean. Signatures below are
normative starting points (refine during implementation without breaking
consumers' semantics).

```java
// com.loqal.identity.users.api — implementation: identity module
public interface UsersApi {
    Mono<UserSnapshot> registerOauthUser(OauthRegistrationRequest request);
    Mono<UserSnapshot> findByEmail(String email);
    Mono<UserSnapshot> findById(UUID userId);
    Mono<Void> upgradeToMerchant(UUID userId, UUID tenantId);
}
// UserSnapshot(UUID userId, String email, String displayName,
//              List<String> roles, UUID tenantId)

// com.loqal.catalog.api — implementation: catalog module
public interface ProductApi {
    /** Active-product price snapshot; errors PRODUCT_NOT_FOUND / PRODUCT_INACTIVE. */
    Mono<ProductPrice> findPrice(UUID productId);
    /** Synchronous fallback path; primary path is the Kafka saga. */
    Mono<StockReservationResult> reserveStock(StockReservationRequest request);
    Mono<Void> revertStock(StockRevertRequest request);
}
// ProductPrice(UUID productId, long unitPriceMinor, String currency, boolean active)
// StockReservationRequest(String requestId, UUID orderId, UUID tenantId, List<Item> items)

// com.loqal.payments.api — implementation: payments module
public interface PaymentApi {
    /** Creates Razorpay order + local record; returns provider order id. */
    Mono<PaymentInitiation> createPayment(UUID orderId, UUID tenantId,
                                          long amountMinor, String currency);
}
// PaymentInitiation(UUID paymentId, String razorpayOrderId, long amountMinor, String currency)
```

Rules:
- No interface may leak entity types or R2DBC types across boundaries.
- New sync dependencies require a PRD amendment documenting the seam.
- Error signaling via typed domain exceptions defined in `shared-contracts`.

---

## Appendix E — Order State Machine Formal Spec

States: `CREATED, PENDING_STOCK, STOCK_RESERVED, PAYMENT_PENDING, PAID,
CONFIRMED, CANCELLED, STOCK_FAILED, PAYMENT_FAILED, FAILED`

Terminal: `CONFIRMED, CANCELLED, FAILED`.

| # | From | Trigger | Guard | Actions | To |
|---|---|---|---|---|---|
| T1 | — (none) | POST /api/orders valid | idempotency check passes | insert order + items (price snapshot); write outbox `order-creation-requested` | CREATED→PENDING_STOCK* |
| T2 | PENDING_STOCK | `stock-reservation-result(success)` | requestId matches | create payment via PaymentApi | STOCK_RESERVED→PAYMENT_PENDING |
| T3 | PENDING_STOCK | `stock-reservation-result(failure)` | — | log failureReason | STOCK_FAILED |
| T4 | STOCK_FAILED | timeout (24h job, Phase 2a candidate) | — | — | FAILED |
| T5 | PAYMENT_PENDING | `payment-completed` | razorpayPaymentId unseen | stamp paid_at | PAID→CONFIRMED |
| T6 | PAYMENT_PENDING | `payment.failed` webhook | signature valid | — | PAYMENT_FAILED→FAILED |
| T7 | PENDING_STOCK / STOCK_RESERVED / PAYMENT_PENDING | POST /cancel by owner | — | publish `order-cancel` (outbox); if past STOCK_RESERVED also publish `refund-requested` when paid flag set | CANCELLING→CANCELLED† |
| T8 | CONFIRMED | POST /cancel | — | rejected `409` (returns flow is Phase 2) | unchanged |
| T9 | any non-terminal | consumed duplicate event | eventId already processed | ack, no-op | unchanged |

\* T1 writes initial row with status `PENDING_STOCK` directly (CREATED exists in
the enum for API-response semantics only).
† `CANCELLING` is an internal await-state until `stock-reservation-result`(revert)
or `refund-completed` arrives; it is represented as `CANCELLED` with a
`settling=true` flag rather than a separate enum value to keep the public API simple.

Invariants:
- I1: sum(order_items.quantity × unit_price_minor) == orders.total_minor always.
- I2: an order has ≤ 1 active payment initiation at a time.
- I3: stock decrements occur only inside catalog reservation transactions keyed
  by requestId present in `processed_events`.
- I4: every state change appends to status history (timestamped).

---

## Appendix F — Orientation for AI Assistants

If you are an AI model working in this repository, anchor on these facts:

1. **What this is**: self-hostable commerce backend; modular monolith; Java 21;
   Spring Boot 3.5.3; WebFlux + R2DBC; NO first-party UI ever in scope (§13).
2. **Read first**: this PRD → spec doc (`docs/superpowers/specs/`) → current plan
   (`docs/superpowers/plans/`). The PRD §13 out-of-scope list and §17 decisions
   log are binding — do not relitigate settled decisions without owner approval.
3. **Non-negotiable invariants**:
   - Cross-module access only via Appendix D interfaces or Appendix B topics.
   - Money = integer minor units + currency code; never floats/doubles.
   - No blocking calls on Reactor event loops; wrap blocking SDKs on virtual threads.
   - All events processed idempotently; publishes go through the outbox.
   - Secrets only via env vars; never committed.
   - Every task: tests green before commit; conventional commit messages.
4. **Commands**: build `mvn clean verify` · single module `mvn -pl modules/<name> test`
   · full stack `docker compose up -d` (needs `.env` from `.env.example`) ·
   gateway entrypoint `http://localhost:8000`.
5. **Where things are** (post-migration): parent `pom.xml`;
   `shared-contracts/src/main/java/com/loqal/contracts/`; domain code under
   `modules/<module>/src/main/java/com/loqal/<module>/`; assembly in `app/`;
   gateway config `kong.yml`; orchestration `docker-compose.yml`.
6. **History**: pre-migration microservices live in git history under
   `backend/java/*-service` — use `git log --follow` to trace moved files;
   rationale for every structural choice is in §17 Decisions Log and §16 Risks.
