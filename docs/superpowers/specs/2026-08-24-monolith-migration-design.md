# Loqal v2 — Modular Monolith Design

**Date:** 2026-08-24
**Status:** Approved

## Vision

Loqal is a **self-hostable backend platform for merchants**: a merchant takes Loqal,
self-hosts it (one `docker-compose up`), and gets a complete online-store backend.
There is no first-party frontend or mobile app; clients consume the HTTP API
(a frontend may come later).

The backend is a **modular monolith** — one deployable Spring Boot application whose
internal module boundaries are compile-enforced, so any module can later be extracted
into its own microservice when scale demands it.

## Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Architecture | Modular monolith (Maven multi-module) | Self-host simplicity; extraction-ready boundaries |
| Web/data stack | WebFlux + R2DBC | 4 core services already use it; avoids rewriting working code |
| Java version | 21 (toolchain + Dockerfile) | Standardize on 21 |
| Virtual threads | Only around blocking SDK calls (Razorpay) | Virtual threads don't benefit event-loop request handling; Razorpay Java SDK is blocking |
| Infra kept | Kong, Kafka/Zookeeper, Redis, Postgres | Preserve seams for future decomposition; saga topics unchanged |
| Frontend / Node services | Deleted | Java-only platform |
| Admin & merchant stubs | Deleted | Thin facades; rebuilt as `platform` module later |
| Greenfield modules | Phased (Plan B, after migration lands) | promotion, delivery, platform, communication |

## Module map (13 original README services → modules)

| Module | Absorbs | Owns (tables) | Responsibility |
|---|---|---|---|
| `shared-contracts` | — | — | Kafka events (`OrderCreationRequested`, `StockReservationResult`, `OrderCancel`, `PaymentCompleted`, `RefundRequested`), shared enums/errors/DTOs |
| `modules/identity` | auth-service + user-service | `user_credentials`, `refresh_tokens`, `users` | Login/register, JWT RS256 issuance with **persisted keys**, JWKS endpoint, refresh tokens, Google OAuth, profiles, addresses, roles, merchant upgrade |
| `modules/catalog` | product-service (+ inventory role) | `products`, categories, `processed_events` | Product/category CRUD, pessimistic-lock stock reservation/reversion, event dedup |
| `modules/orders` | order-service (+ delivery later) | `orders` + items, `outbox_events` | Order lifecycle saga, transactional outbox → Kafka, Redis idempotency keys |
| `modules/payments` | payment-service (**rewritten** in R2DBC/WebFlux) | `payments`, `refunds` | Razorpay orders (blocking SDK on virtual threads), webhook verification, refunds |
| `app` | — | — | Single `@SpringBootApplication`, component-scans all modules, one HTTP port behind Kong |

Future modules (Plan B): `promotion` (→ catalog pricing rules), `delivery`
(order fulfillment stage), `platform` (admin + merchant management),
`communication` (notification + chat over WebSocket).

## Module rules

1. Each module owns its tables and entities; no cross-module entity access.
2. Synchronous cross-module calls go through **published API interfaces only**
   (`UsersApi`, `ProductApi`, `PaymentApi`) implemented inside the owning module.
3. Asynchronous flows use **unchanged Kafka topics**: `order-creation-requested`,
   `stock-reservation-result`, `order-cancel`, `payment-completed`,
   `refund-requested` (+ DLT). The transactional outbox relay is preserved.
4. Dependency direction: `app` → all modules → `shared-contracts`.
5. Base package per module: `com.loqal.<module>`.

## Target structure

```
Loqal/
├── pom.xml                    # parent aggregator: Boot 3.5.3, Java 21, dependencyManagement
├── shared-contracts/          # com.loqal.contracts
├── modules/
│   ├── identity/              # com.loqal.identity.{auth,users}
│   ├── catalog/               # com.loqal.catalog
│   ├── orders/                # com.loqal.orders
│   └── payments/              # com.loqal.payments
├── app/                       # com.loqal.app
├── kong.yml                   # all routes → loqal-platform upstream
├── docker-compose.yml         # kong + loqal-platform + postgres + redis + kafka/zookeeper
├── Dockerfile                 # Corretto 21, root-built jar
├── .env.example
└── README.md                  # rewritten vision
```

Deleted: `frontend/`, `backend/node/`, `backend/java/admin-service`,
`backend/java/merchant-service`; entire `backend/` tree after ports complete.

## Targeted fixes included in scope

- **Ephemeral RSA keys bug**: auth currently regenerates its RSA keypair every boot,
  invalidating tokens and breaking Kong's pinned public key. Keys are now loaded from
  env config (`JWT_RSA_PRIVATE_KEY`, base64 PKCS#8).
- Payment-service config debt disappears with the rewrite (it previously could not boot:
  missing Kafka/topic/webhook-secret wiring, hardcoded localhost bootstrap servers).

## Testing strategy

- Per-module unit tests ported/new; context-load test per module.
- Saga integration test with Testcontainers (Postgres + Kafka):
  create order → stock reserved → payment completed → order confirmed;
  asserts topic flow and DB state.
- Auth flow test: login issues RS256 token validating against persisted JWKS key.
- Final smoke test via `docker compose up`: register → login → product → order through Kong :8000.

## Out of scope (separate specs/plans)

Promotion engine, delivery/geofencing, platform (admin+merchant) module,
communication (notification/chat). Frontends of any kind.
