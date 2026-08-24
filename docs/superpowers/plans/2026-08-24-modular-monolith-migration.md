# Loqal Modular Monolith Migration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the backend from 5 Spring microservices + 1 Node service into a single self-hostable Java 21 modular monolith (WebFlux/R2DBC), preserving Kong/Kafka/Redis seams, and rewrite the README for the new vision.

**Architecture:** Maven multi-module monolith: parent pom → `shared-contracts` + 4 domain modules (`identity`, `catalog`, `orders`, `payments`) + `app` assembly. Cross-module sync calls via published API interfaces; async flows via unchanged Kafka topics; transactional outbox preserved.

**Tech Stack:** Spring Boot 3.5.3, Java 21, WebFlux + R2DBC/PostgreSQL, spring-kafka/reactor-kafka, Redis reactive, Razorpay SDK 1.4.8 (virtual threads), Nimbus/jjwt RS256, Kong DB-less gateway.

**Spec:** `docs/superpowers/specs/2026-08-24-monolith-migration-design.md`

---

## Source-of-truth facts (from current code)

- Existing poms hang off `spring-boot-starter-parent` **3.5.3**; order-service uses Java 17 + spring-cloud 2025.0.0 BOM; auth/payment poms use Java 21.
- order-service pom deps (proven working set to lift): webflux, r2dbc (starter + r2dbc-postgresql + postgresql), security, oauth2-resource-server, oauth2-jose, spring-kafka, reactor-kafka, resilience4j-spring-boot2 2.0.2 + resilience4j-reactor 2.2.0, data-redis-reactive, lombok, validation, springdoc-openapi-starter-webmvc-ui 2.8.9, starter-test, reactor-test, spring-security-test.
- auth-service adds: oauth2-client, jjwt-api/impl/jackson **0.12.6**, h2 (test).
- payment-service adds: razorpay-java **1.4.8**.
- Config env vars today: `DB_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`, `STATE_CHECKER_ENABLED`, `ACCESS_TOKEN_EXPIRY`, `REFRESH_TOKEN_EXPIRY`, `USER_SERVICE_URL`, `DDL_AUTO_SETTING`, per-service port vars, `JWT_JWKS_URI`, `KAFKA_BOOTSTRAP_SERVERS`, topic vars (`ORDER_CREATTION_REQUESTED`, `STOCK_RESERVATION_RESULT`, `ORDER_CANCEL`, `ORDER_CANCEL_DLT` — note existing typo "CREATTION" in compose), `REDIS_HOST`, `REDIS_PORT`, `PRODUCT_SERVICE_URL`.
- Kafka topics in code: `order-creation-requested`, `stock-reservation-result`, `order-cancel`, `order-cancel-dlt`, `payment-completed`, `refund-requested`.
- All services share one Postgres (`loqal-db`, db `loqaldb`) already.

---

### Task 0: Delete admin & merchant stubs

**Files:** Delete `backend/java/admin-service/**`, `backend/java/merchant-service/**`.

- [ ] Step 1: Verify tracked & clean: `git ls-files backend/java/admin-service backend/java/merchant-service | wc -l`
- [ ] Step 2: `git rm -r -q backend/java/admin-service backend/java/merchant-service`
- [ ] Step 3: Commit: `git commit -m "chore: remove admin and merchant service stubs"`

### Task 1: Parent pom + module skeletons + shared-contracts

**Files:** Create root `pom.xml`; create `shared-contracts/pom.xml`, `modules/identity/pom.xml`, `modules/catalog/pom.xml`, `modules/orders/pom.xml`, `modules/payments/pom.xml`, `app/pom.xml`; create `shared-contracts/src/main/java/com/loqal/contracts/…`.

- [ ] Step 1: Root parent pom — groupId `com.loqal`, artifactId `loqal-parent`, parent = spring-boot-starter-parent 3.5.3, `<java.version>21</java.version>`, modules list, dependencyManagement lifting the proven dependency set from `backend/java/order-service/pom.xml` (+ jjwt 0.12.6 from auth-service pom). No spring-cloud BOM (unused at runtime).
- [ ] Step 2: Module poms — each declares parent `loqal-parent` and only the dependencies it needs:
  - shared-contracts: lombok, validation only
  - identity: webflux, data-r2dbc, r2dbc-postgresql, security, oauth2-client, oauth2-resource-server, oauth2-jose, jjwt 0.12.6, postgresql, springdoc, lombok, validation
  - catalog/orders: webflux, data-r2dbc, security+oauth2-resource-server+jose, spring-kafka, reactor-kafka, redis-reactive (orders), resilience4j, springdoc, lombok, validation
  - payments: webflux, data-r2dbc, security+oauth2-resource-server, spring-kafka, reactor-kafka, razorpay-java 1.4.8, lombok, validation
  - app: depends on all four modules; spring-boot-maven-plugin repackage
- [ ] Step 3: Move duplicated event/DTO classes into `com.loqal.contracts`: take canonical versions from order-service (`OrderEvent`, `OrderStatus`, `OrderStatusUpdate`, `OrderUpdate`, `StockReservationResponse`, `ProductOrderRequest`, exception types) and payment-service (`PaymentCompletedEvent`, refund event), plus topic-name constants class `Topics` (fix the `CREATTION` typo here: constant `ORDER_CREATION_REQUESTED = "order-creation-requested"`).
- [ ] Step 4: `mvn clean install -DskipTests` → BUILD SUCCESS expected.
- [ ] Step 5: Commit: `git commit -m "feat: add modular monolith skeleton with shared contracts"`

### Task 2: Port user-service → modules/identity (com.loqal.identity.users)

**Files:** `git mv backend/java/user-service/src/main/java/com/Loqal/userservice` → `modules/identity/src/main/java/com/loqal/identity/users`; same for test dir.

- [ ] Step 1: `git mv` main + test source trees; delete old service pom leftovers after move.
- [ ] Step 2: Package rename `com.Loqal.userservice` → `com.loqal.identity.users` across moved files; remove old `UserApplication` main class (app assembles later).
- [ ] Step 3: Create `com.loqal.identity.users.api.UsersApi` interface exposing: register OAuth user, get profile by id, fetch roles/tenantId by email, upgrade-to-merchant — implemented by existing `UserService`. Controllers keep HTTP routes.
- [ ] Step 4: Add `tenants` table support per PRD ID-120/§10 (`com.loqal.identity.users.entity.Tenant`, repository, tenant provisioning on registration).
- [ ] Step 5: Test: `mvn -pl modules/identity test` → context-load passes (add minimal `@SpringBootTest` config or replace with slice test since no main class yet — prefer plain JUnit on service logic).
- [ ] Step 6: Commit: `git commit -m "feat(identity): port user-service into identity module"`

### Task 3: Port auth-service → modules/identity (com.loqal.identity.auth) + RSA key persistence fix

**Files:** `git mv backend/java/auth-service/src/main/java/com/Loqal/authservice` → `modules/identity/src/main/java/com/loqal/identity/auth`; modify `utils/RSAKeyProvider.java`; delete `service/UserServiceClient.java`; modify `AuthService.java` login/register paths; modify `SpringSecurityConfig.java` merge.

- [ ] Step 1: `git mv` sources; package rename → `com.loqal.identity.auth`; drop old main class.
- [ ] Step 2: Rewrite `RSAKeyProvider` to load base64 PKCS#8 private key + SPKI public key from properties `jwt.rsa.private-key` / `jwt.rsa.public-key` (env `JWT_RSA_PRIVATE_KEY` / `JWT_RSA_PUBLIC_KEY`); fail fast if absent. JWKS endpoint serves the loaded key (stable kid).
- [ ] Step 3: Delete `UserServiceClient` + `services.user-service.url` config; inject `UsersApi` bean where roles/tenant sync happened (register + login paths in `AuthService`).
- [ ] Step 4: Remove dead config keys (`jwt.secret`, `state-checker`); update application.yml in module resources to new env names.
- [ ] Step 5: Test: unit test that a token signed by provider validates against its JWKS-derived public key; `mvn -pl modules/identity test`.
- [ ] Step 6: Commit: `git commit -m "feat(identity): port auth-service, persist RSA keys, inline users API"`

### Task 4: Port product-service → modules/catalog

**Files:** `git mv backend/java/product-service/src/main/java/com/Loqal/productservice` → `modules/catalog/src/main/java/com/loqal/catalog`.

- [ ] Step 1: `git mv`; package rename → `com.loqal.catalog`; drop old main class; delete vestigial WebClientConfig beans (product/user URLs no longer used).
- [ ] Step 2: Replace local copies of shared DTOs/events with imports from `shared-contracts`; producers/consumers use `Topics` constants.
- [ ] Step 3: Create `com.loqal.catalog.api.ProductApi` interface: find price by product id, reserve stock (pessimistic lock path), revert stock — implemented by existing `ProductService`.
- [ ] Step 4: Keep `processed_events` dedup + circuit-breaker config as-is.
- [ ] Step 5: Test: `mvn -pl modules/catalog test`; add unit test for stock reservation dedup using mocked repository.
- [ ] Step 6: Commit: `git commit -m "feat(catalog): port product-service into catalog module"`

### Task 5: Port order-service → modules/orders

**Files:** `git mv backend/java/order-service/src/main/java/com/Loqal/orderservice` → `modules/orders/src/main/java/com/loqal/orders`.

- [ ] Step 1: `git mv`; package rename → `com.loqal.orders`; drop old main class.
- [ ] Step 2: In `services/OrderService.java`: replace `GET {product}/api/products/{id}` WebClient call with `ProductApi.findPrice(...)`; keep Kafka publish/consume flow identical.
- [ ] Step 3: Replace shared DTO classes with contracts imports; outbox relay (`OutboxService`/`EventRelay`) unchanged.
- [ ] Step 4: Keep Redis idempotency in controller unchanged.
- [ ] Step 5: Test: `mvn -pl modules/orders test`; unit-test idempotency-key short-circuit logic.
- [ ] Step 6: Commit: `git commit -m "feat(orders): port order-service, swap REST hops for module APIs"`

### Task 6: Rewrite payment-service → modules/payments (R2DBC/WebFlux)

**Files:** New under `modules/payments/src/main/java/com/loqal/payments/`: entity/Payment.java, entity/Refund.java, repository/*.java, gateway/RazorpayGateway.java, service/PaymentService.java, controller/PaymentController.java, kafka/PaymentEventsProducer.java, kafka/RefundRequestConsumer.java, config/SecurityConfig.java, resources/application.yml.

- [ ] Step 1: Port domain model: R2DBC entities `payments`, `refunds` (reuse table/column names from old JPA entities so DB stays compatible); repositories extending `ReactiveCrudRepository`.
- [ ] Step 2: `RazorpayGateway`: wraps `com.razorpay.RazorpayClient` calls; executor = `Executors.newVirtualThreadPerTaskExecutor()` wrapped via `Schedulers.fromExecutorService`, bounded by timeout operators; methods: createOrder(amount, receipt, notes), verifyWebhookSignature(body, signature), fetchPayment(id), initiateRefund(paymentId, amount).
- [ ] Step 3: `PaymentService`: create payment record → call gateway → return Razorpay order id; webhook handler verifies HMAC then marks payment completed and emits `payment-completed` via producer (reactor-kafka); consumer of `refund-requested` triggers gateway refund + emits result.
- [ ] Step 4: SecurityConfig: `/api/payments/webhook` permitAll + signature-verified; rest JWT-protected via shared resource-server style used by other modules.
- [ ] Step 5: application.yml fully wired: r2dbc `${DB_URL}` etc., kafka bootstrap `${KAFKA_BOOTSTRAP_SERVERS}`, topics via `Topics` constants, `razorpay.key-id`/`razorpay.key-secret`/`razorpay.webhook-secret` env-backed.
- [ ] Step 6: Tests: unit test webhook signature verification (valid/invalid HMAC); unit test payment state transitions; `mvn -pl modules/payments test`.
- [ ] Step 7: Commit: `git commit -m "feat(payments): rewrite payment service as R2DBC module with virtual threads"`

### Task 7: app assembly

**Files:** `app/src/main/java/com/loqal/app/LoqalApplication.java`, `app/src/main/java/com/loqal/app/config/SecurityConfig.java`, `app/src/main/resources/application.yml`.

- [ ] Step 1: Main class `@SpringBootApplication(scanBasePackages = "com.loqal")` + `@EnableR2dbcRepositories` if needed.
- [ ] Step 2: Unified reactive `SecurityWebFilterChain`: permitAll `/auth/**`, `/.well-known/**`, `/api/payments/webhook`, swagger paths; everything else authenticated via `NimbusReactiveJwtDecoder.withPublicKey(...)` (local persisted key, no network JWKS round-trip internally).
- [ ] Step 3: Merged application.yml: single r2dbc block (`${DB_URL}`, `${POSTGRES_USER}`, `${POSTGRES_PASSWORD}`), kafka bootstrap/topics, redis host/port, google OAuth client, jwt expiry props, razorpay props, `server.port: ${APP_PORT:8080}`, `spring.jackson` defaults.
- [ ] Step 4: `mvn clean install` full build green; boot smoke: run app against empty Postgres → starts without errors.
- [ ] Step 5: Commit: `git commit -m "feat(app): assemble monolith application with unified security and config"`

### Task 8: Infra — Dockerfile, docker-compose, kong.yml

**Files:** Modify `docker-compose.yml`, `kong.yml`; create `Dockerfile`, `.env.example`.

- [ ] Step 1: Root multi-stage Dockerfile: maven build stage → Corretto 21 runtime copying app jar; expose 8080.
- [ ] Step 2: docker-compose: replace auth/user/product/order services with one `loqal-platform` (build `.`, env consolidated, depends_on db healthy + kafka + redis); kong depends_on loqal-platform; keep kong/db/redis/zookeeper/kafka blocks as-is.
- [ ] Step 3: kong.yml: all four route prefixes (`/auth`, `/users`, `/products`, `/api/orders`) → upstream `loqal-platform:8080`; JWT consumer key_credential set to the RSA **public** key matching `JWT_RSA_PUBLIC_KEY` env.
- [ ] Step 4: `.env.example` listing every var with placeholder values incl. `JWT_RSA_PRIVATE_KEY` generation hint (`openssl genpkey -algorithm RSA … | base64 -w0`).
- [ ] Step 5: Smoke: `docker compose up -d` → all containers healthy; `curl :8000/auth/login` reaches app through Kong.
- [ ] Step 6: Commit: `git commit -m "feat(infra): consolidate deployment to single loqal-platform container"`

### Task 9: Cleanup + README rewrite

**Files:** Delete `backend/**`; rewrite `README.md`.

- [ ] Step 1: `git rm -r -q backend` (all remaining old service trees now unused).
- [ ] Step 2: Rewrite README: self-hostable merchant platform vision; module map table; architecture diagram (client → kong → app → {kafka, redis, postgres}); how-to-run (docker-compose + .env); module rules; phased roadmap (Phase 2 modules: promotion, delivery, platform, communication).
- [ ] Step 3: Full `mvn clean verify` green.
- [ ] Step 4: Commit: `git commit -m "docs: rewrite README for modular monolith vision; remove legacy backend tree"`

### Task 10: Saga integration test

**Files:** `app/src/test/java/com/loqal/app/saga/OrderSagaIT.java`, `app/src/test/resources/application-test.yml`.

- [ ] Step 1: Add testcontainers deps (postgres, kafka) to app test scope.
- [ ] Step 2: Write integration test: seed catalog product via `ProductApi`-backed repo; POST order with idempotency key → assert stock reserved row + `stock-reservation-result` consumed; simulate payment-completed publish → assert order status CONFIRMED; cancel path asserts stock reverted.
- [ ] Step 3: Run: `mvn -pl app test -Dtest=OrderSagaIT` → PASS.
- [ ] Step 4: Commit: `git commit -m "test: add end-to-end order saga integration test"`
