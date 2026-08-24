# Loqal on Kubernetes

Manifests for the Phase 3 "Frontier" deployment target (PRD §14).

## What's here

| File | Purpose |
|---|---|
| `00-namespace.yaml` | `loqal` namespace |
| `10-configmap.yaml` | Non-secret runtime configuration |
| `20-secret.template.yaml` | Template for secrets — **copy to `20-secret.yaml`, fill in, never commit** |
| `30-loqal-platform.yaml` | Platform Deployment (2 replicas) + Service; probes hit actuator health groups |
| `40-kong-config.yaml` | Kong DB-less declarative config as a ConfigMap |
| `50-kong.yaml` | Kong proxy Deployment + LoadBalancer Service |

## Assumptions

- **Postgres / Kafka / Redis** are provisioned separately (managed services or the
  Bitnami Helm charts). Point `10-configmap.yaml` at your endpoints:
  ```bash
  helm install postgres bitnami/postgresql -n loqal
  helm install kafka bitnami/kafka -n loqal
  helm install redis bitnami/redis -n loqal
  ```
- The platform image must be published to a registry your cluster can pull from.
- Flyway runs inside the app at startup against the JDBC URL in the ConfigMap;
  exactly one replica performs migration automatically (Flyway locking).

## Deploy

```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/10-configmap.yaml
cp k8s/20-secret.template.yaml k8s/20-secret.yaml   # fill in values first!
kubectl apply -f k8s/20-secret.yaml
kubectl create configmap kong-config -n loqal --from-file=kong.yml=./kong.yml --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f k8s/30-loqal-platform.yaml
kubectl apply -f k8s/50-kong.yaml
```

## Scaling story

The modular monolith scales horizontally behind Kong for stateless traffic.
When a single deployment genuinely saturates (per PRD G4), extract a module:
its published API interfaces and Kafka seams mean it lifts out into this same
namespace as its own Deployment + Service with no logic changes.
