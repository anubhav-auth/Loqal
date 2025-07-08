# Loqal

## 📁 Repository Structure
```plaintext
Loqal/
├── apps/
│   ├── android/
│   │   ├── customer-app/
│   │   ├── delivery-app/
│   │   └── merchant-app/
│   └── ios/
│       ├── customer-app/
│       ├── delivery-app/
│       └── merchant-app/
├── web/
│   ├── landing-pages/
│   │   ├── variant-a/
│   │   └── variant-b/
│   ├── dashboards/
│   │   ├── variant-a/
│   │   └── variant-b/
│   └── storefront/
├── backend/
│   ├── java/
│   │   ├── auth-service/
│   │   │   ├── src/
│   │   │   └── tests/
│   │   ├── user-service/
│   │   │   ├── src/
│   │   │   └── tests/
│   │   ├── product-service/
│   │   │   ├── src/
│   │   │   └── tests/
│   │   └── order-service/
│   │       ├── src/
│   │       └── tests/
│   ├── node/
│   │   ├── notification-service/
│   │   │   ├── src/
│   │   │   └── tests/
│   │   └── analytics-service/
│   │       ├── src/
│   │       └── tests/
│   └── database/
│       ├── migrations/
│       └── seeds/
├── infra/
│   ├── config/
│   │   ├── dev/
│   │   ├── staging/
│   │   └── prod/
│   ├── docker/
│   │   └── dockerfiles/
│   ├── nginx/
│   │   ├── api-gateway.conf
│   │   └── ssl/
│   ├── kong/
│   │   ├── declarative/
│   │   └── plugins/
│   └── k8s/
│       ├── manifests/
│       └── helm/
├── docs/
│   ├── architecture.md
│   ├── api-spec.yaml
│   └── setup.md
├── scripts/
│   ├── setup.sh
│   ├── deploy.sh
│   └── migrate.sh
├── tests/
│   ├── unit/
│   └── integration/
├── .github/
│   └── workflows/
│       ├── ci.yml
│       └── cd.yml
├── docker-compose.yml
├── .gitignore
├── README.md
└── package.json
```
