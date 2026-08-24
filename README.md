<img src="./banner.png" width=1000px>

# Loqal — Self-Hostable Commerce Backend Platform

**Loqal** is a self-hostable, multi-tenant commerce backend for local businesses
and startups. Run one command and you have a production-grade online-store
backend: identity, catalog, ordering, and payments — exposed through a clean
HTTP API behind the Kong gateway.

> **One-line pitch:** "Self-host Shopify's backend, not Shopify."

Loqal is deliberately **backend-only**: any client — your own storefront, a
freelance build, or a future first-party frontend — consumes the same documented API.

Full product requirements: [`docs/PRD.md`](docs/PRD.md)

---

## How It Works

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

## Modules

| Module | Package | Responsibility |
|---|---|---|
| `shared-contracts` | `com.loqal.contracts` | Kafka events, DTOs, enums shared across modules |
| `modules/identity` | `com.loqal.identity.*` | Auth (email/password + Google OAuth), RS256 JWT issuance with persisted keys, JWKS, refresh tokens, profiles, addresses, roles, tenancy |
| `modules/catalog` | `com.loqal.catalog` | Products, categories, concurrency-safe stock reservation/reversion |
| `modules/orders` | `com.loqal.orders` | Order lifecycle saga, transactional outbox → Kafka, idempotent order creation |
| `modules/payments` | `com.loqal.payments` | Razorpay orders (blocking SDK on virtual threads), signed webhooks, refunds |
| `app` | `com.loqal.app` | Single Spring Boot assembly: one process, one port |

**Module rules** (enforced by Maven structure):

1. Each module owns its tables; no cross-module entity access.
2. Sync cross-module calls only via published API interfaces (`UsersApi`,
   `ProductApi`, `PaymentApi`).
3. Async flows via Kafka topics (`order-creation-requested`,
   `stock-reservation-result`, `order-cancel`, `payment-completed`,
   `refund-requested`, …) with a transactional outbox guaranteeing publish.
4. Dependencies flow one way: `app → modules → shared-contracts`.

Because boundaries are compile-enforced and all async flows already use the
broker, **any module can later be extracted into its own microservice without
rewriting its logic**.

## Tech Stack

| Category | Technology |
| :--- | :--- |
| Language / Runtime | Java 21 (virtual threads for blocking SDK calls) |
| Framework | Spring Boot 3.5.x — WebFlux + R2DBC |
| Database | PostgreSQL 15 |
| Cache / Idempotency | Redis |
| Events | Apache Kafka |
| API Gateway | Kong (DB-less) |
| Payments | Razorpay SDK |
| Docs | OpenAPI / Swagger UI |

## Getting Started

### Prerequisites

- Docker & Docker Compose
- (for development) Java 21 + Maven 3.9+

### Run the full stack

```bash
cp .env.example .env        # fill in values (JWT keys required — see file)
docker compose up -d
```

Kong listens on **:8000** and routes everything to the platform container.

Generate JWT signing keys:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
  | openssl pkcs8 -topk8 -nocrypt | tr -d '\n'     # → JWT_RSA_PRIVATE_KEY
openssl pkey -in private.pem -pubout | tr -d '\n'  # → JWT_RSA_PUBLIC_KEY
```

Paste the public key PEM into `kong.yml` (`jwt_secrets.rsa_public_key`).

### Build & test locally

```bash
mvn clean verify                 # full build + unit tests
mvn -pl modules/orders test      # single module
java -jar app/target/*.jar       # run against your own Postgres/Kafka/Redis
```

Swagger UI: served by springdoc (`springdoc.swagger-ui.path`, default `/custom-ui.html`).

---

## Roadmap

| Phase | Content | Status |
|---|---|---|
| 1 | Modular monolith migration (identity, catalog, orders, payments) | ✅ In progress |
| 2a | Hardening: Flyway migrations, money-in-minor-units schema, rate limiting, observability | Planned |
| 2b | Promotion module (coupons, discounts, campaigns) | Planned |
| 2c | Delivery module (agents, assignment, tracking, geofencing, OTP handover) | Planned |
| 2d | Platform module (merchant onboarding, admin management, audit logs) | Planned |
| 2e | Communication module (notifications + real-time chat) | Planned |

See the [PRD](docs/PRD.md) for scope details, out-of-scope list, and decision log.
