# Infrastructure & Deployment Verification Guide

This document captures verification steps that require real external systems and cannot be covered by unit tests. These are deployment-time checks to run after `docker compose up` or Kubernetes deployment.

---

## 1. Docker Compose Smoke Test

Prerequisites: Docker running, `.env` file filled from `.env.example`.

```bash
docker compose up -d
```

### 1.1 JWKS Endpoint

```bash
curl -s http://localhost:8000/.well-known/jwks.json | jq .
# Expected: { "keys": [{ "kid": "...", "kty": "RSA", ... }] }
```

### 1.2 Register

```bash
curl -s -X POST http://localhost:8000/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"secret123","fullName":"Test User","phoneNumber":"+911234567890"}'
# Expected: 201 { "message": "User registered successfully" }
```

### 1.3 Login

```bash
TOKEN=$(curl -s -X POST http://localhost:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"secret123"}' | jq -r '.accessToken')
echo $TOKEN
# Expected: eyJhbGciOiJSUzI1NiIs...
```

### 1.4 Authenticated Request

```bash
curl -s http://localhost:8000/users/profile/<user-id> \
  -H "Authorization: Bearer $TOKEN"
# Expected: 200 { "userId": "...", "fullName": "Test User", ... }
```

### 1.5 Create Product (as merchant)

```bash
curl -s -X POST http://localhost:8000/products/<merchant-tenant-id> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Widget","description":"A widget","category":{"category_name":"General","category_description":"Stuff"},"priceMinor":999,"quantity":100,"image_urls":[]}'
# Expected: 201 { "id": "...", "name": "Widget", ... }
```

### 1.6 Create Order

```bash
curl -s -X POST http://localhost:8000/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: order-001" \
  -d '{"items":[{"productId":"<product-id>","quantity":2}],"merchantId":"<merchant-tenant-id>"}'
# Expected: 201 { "orderId": "...", "razorpayOrderId": "order_xxx" }
```

### 1.7 Kafka Topics Created

```bash
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
# Expected: order-creation-requested, stock-reservation-result, payment-completed, refund-requested, refund-completed, order-cancel, order-cancel-dlt, chat-messages
```

### 1.8 Database Migrated

```bash
docker compose exec postgres psql -U loqal -d loqaldb -c "\dt"
# Expected: tables for users, products, orders, payments, refunds, coupons, coupon_redemptions, delivery_agents, deliveries, audit_logs, merchant_profiles, notifications, chat_messages, outbox_events, processed_events
```

### 1.9 Redis Connected

```bash
docker compose exec redis redis-cli ping
# Expected: PONG
```

---

## 2. Payment Provider Verification

### 2.1 Mock Provider (CI/Local)

Set `payments.provider=mock` in `application.yml` or env. No external calls needed.

```bash
# Webhook accepts "mock-signature"
curl -X POST http://localhost:8000/payments/webhook \
  -H "Content-Type: application/json" \
  -H "X-Razorpay-Signature: mock-signature" \
  -d '{"payload":{"payment":{"entity":{"id":"mock_pay_1","order_id":"order_xxx","amount":999}}}}'
# Expected: 200
```

### 2.2 Razorpay Provider (Production)

Requires live Razorpay test-mode credentials in `.env`:

```
RAZORPAY_KEY_ID=rzp_test_...
RAZORPAY_KEY_SECRET=...
RAZORPAY_WEBHOOK_SECRET=...
```

1. Create a test order via the API
2. Complete payment on Razorpay test dashboard
3. Razorpay sends webhook → verify `payment.captured` event processes correctly
4. Verify `orders` table shows `ORDER_CONFIRMED` after stock reservation

### 2.3 Refund Flow

```bash
# Return an order (requires order in CONFIRMED/DELIVERED state)
curl -X POST http://localhost:8000/api/orders/<order-id>/return \
  -H "Authorization: Bearer $TOKEN"
# Expected: 202 Accepted

# Verify: order status → ORDER_CANCELLED_PENDING
# After refund webhook: order status → ORDER_RETURNED
# Check refunds table has a row
```

---

## 3. Google OAuth Verification

### 3.1 Setup

In Google Cloud Console, create OAuth 2.0 credentials:
- Authorized redirect URI: `http://localhost:8000/auth/google/callback`

Fill in `.env`:
```
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GOOGLE_REDIRECT_URI=http://localhost:8000/auth/google/callback
```

### 3.2 Web Flow

1. Redirect user to:
   ```
   https://accounts.google.com/o/oauth2/v2/auth?client_id=...&redirect_uri=...&response_type=code&scope=openid email profile
   ```
2. After Google consent, browser redirects to `/auth/google/callback?code=...`
3. Backend exchanges code for tokens, fetches user info, creates/finds user, returns JWT pair

### 3.3 Mobile Flow

```bash
curl -X POST http://localhost:8000/auth/oauth/mobile/google \
  -H "Content-Type: application/json" \
  -d '{"idToken":"<google-id-token>"}'
# Expected: 200 { "accessToken": "...", "refreshToken": "..." }
```

---

## 4. WebSocket Chat Verification

```bash
# Using websocat (install: cargo install websocat)
websocat "ws://localhost:8000/communication/chat/ws"

# Send a chat frame:
>{"roomId":"order:test-123","senderId":"<user-uuid>","senderRole":"CUSTOMER","content":"Hello!"}

# Echo received (same shape back)
```

For multi-instance fan-out:
1. Start 2 platform instances
2. Connect a WebSocket client to each
3. Send a message from client A → both A and B should receive the echo
4. Verify via Kafka: `docker compose exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic chat-messages`

---

## 5. Kafka Topic Verification

```bash
# List topics
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Expected topics:
# - order-creation-requested
# - order-creation-requested-dlt
# - order-cancel
# - order-cancel-dlt
# - stock-reservation-result
# - payment-completed
# - refund-requested
# - refund-completed
# - chat-messages

# Consume from a topic
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-completed \
  --from-beginning
```

---

## 6. Kong Gateway Verification

### 6.1 Rate Limiting

```bash
# Rapid requests (should get 429 after 100/min)
for i in $(seq 1 105); do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8000/.well-known/jwks.json
done | sort | uniq -c
# Expected: ~100x 200, ~5x 429
```

### 6.2 JWT Enforcement

```bash
# Protected endpoint without token → 401
curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/users/profile/<id>
# Expected: 401

# Protected endpoint with valid token → 200
curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/users/profile/<id> \
  -H "Authorization: Bearer $TOKEN"
# Expected: 200
```

### 6.3 Public Endpoints (no auth required)

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/.well-known/jwks.json
# Expected: 200

curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8000/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"new@example.com","password":"pass","fullName":"New"}'
# Expected: 201
```

---

## 7. Kubernetes Verification

After deploying to a cluster:

```bash
# Check pods running
kubectl get pods -n loqal

# Check logs
kubectl logs -n loqal deployment/loqal-platform --tail=50

# Check health endpoints
kubectl port-forward -n loqal svc/loqal-platform 8080:8080
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus | head -20

# Check Kong proxy
kubectl port-forward -n loqal svc/kong-proxy 8000:8000
curl http://localhost:8000/.well-known/jwks.json
```

### 7.1 Flyway Migration

Flyway runs at startup. Check logs for:
```
Successfully applied 5 migrations to schema "public"
```

If migration fails, the pod will crash-loop. Check:
```bash
kubectl logs -n loqal deployment/loqal-platform | grep -i "flyway\|migration\|error"
```

---

## 8. Observability Verification

### 8.1 Prometheus

```bash
# Port-forward Prometheus
kubectl port-forward svc/prometheus 9090:9090

# Open http://localhost:9090
# Query: http_server_requests_seconds_count
# Query: jvm_memory_used_bytes
```

### 8.2 Grafana

```bash
# Port-forward Grafana
kubectl port-forward svc/grafana 3001:3000
# Login: admin / <GRAFANA_ADMIN_PASSWORD>

# Prometheus datasource should be auto-configured
# Import dashboards: JVM (7724), Spring Boot Statistics (12900)
```

---

## 9. Known Limitations & Future Work

| Item | Status | Notes |
|---|---|---|
| Kong smoke test via Docker | Not automated | Requires `.env` with RSA keys filled in |
| Google OAuth E2E | Not automated | Requires real Google credentials |
| WebSocket handshake E2E | Not automated | Requires running server |
| Multi-instance chat fan-out | Not tested with real Kafka | Unit-tested with mocks |
| K8s deployment | Manifests provided, not tested on live cluster | Requires cloud provider |
| Razorpay production flow | Not tested with real Razorpay | Requires live test-mode keys |
| Load testing | Not done | Consider k6 or Gatling for Phase 4 |
| Security audit | Not done | Consider OWASP ZAP scan before production |
| Database backup/restore | Not documented | Add to ops runbook |

---

## 10. Running Full Verification Locally

One-shot command to verify everything unit-testable:

```bash
mvn clean verify    # 177 tests, all modules
```

Then for integration-level:

```bash
docker compose up -d
sleep 30    # wait for Flyway + Kafka readiness

# Run the smoke test script above (sections 1.1–1.7)
# Verify Kafka topics exist (section 5)
# Verify Kong rate limiting (section 6.1)
# Verify actuator endpoints (section 8)
```
