# PKG-11: Kubernetes Manifests

**Status:** Not Started
**Depends on:** PKG-01 (for container images)

## Goal

Production-ready Kubernetes deployment — namespaces, network policies, Istio mTLS, Strimzi Kafka, HPAs, Kustomize overlays for dev/prod.

## Produces

- Namespace definitions: `dachshaus-public`, `dachshaus-services`, `dachshaus-data`, `dachshaus-kafka`
- Network policies: gateway→services, services→data, deny all else
- Istio: `PeerAuthentication` (STRICT mTLS), `AuthorizationPolicy`, `VirtualService`
- Per-service: `Deployment`, `Service`, `HPA`, `ServiceAccount` (with WI annotation)
- Strimzi: `Kafka` cluster CRD, all `KafkaTopic` CRDs, all `KafkaUser` CRDs, metrics ConfigMap
- Redis: `StatefulSet` + `Service` (dev only, prod uses Memorystore)
- Ingress: Istio ingress gateway with TLS termination (integrates with `VirtualService` routing)
- Kustomize overlays: dev (low resources), prod (HA resources)
- External Secrets: `ExternalSecret` CRDs pulling from GCP Secret Manager
- Deploy scripts: `setup-cluster.sh`, `deploy.sh`

## Interface Contracts

### Namespaces
- `dachshaus-public`: gateway, storefront, ingress
- `dachshaus-services`: auth, catalog, order, customer, cart, streams
- `dachshaus-data`: redis (dev), cloud-sql-proxy sidecars
- `dachshaus-kafka`: strimzi operator, kafka brokers, entity operator

### Network Policy Rules
- `dachshaus-services` ingress ← `dachshaus-public`, `dachshaus-services`
- `dachshaus-data` ingress ← `dachshaus-services`
- `dachshaus-kafka` ingress ← `dachshaus-services`

### Service Ports
```
gateway:4000, auth:8084, catalog:8081, order:8082,
customer:8083, cart:8085, redis:6379
```

### HPA Ranges
| Service | Min | Max | CPU Target | Notes |
|---|---|---|---|---|
| gateway | 2 | 10 | 60% | |
| auth | 2 | 5 | 60% | |
| catalog | 2 | 10 | 65% | |
| order | 2 | 10 | 60% | |
| customer | 2 | 5 | 65% | |
| cart | 3 | 20 | 60% | + Redis p99 custom metric |
| storefront | 2 | 8 | 60% | |

## Acceptance Criteria

- [ ] `kubectl apply -k k8s/overlays/dev` deploys entire stack
- [ ] Network policies prevent direct access from public to services namespace
- [ ] Istio mTLS enforced (verified: `istioctl authn tls-check`)
- [ ] Strimzi creates 3-broker Kafka cluster with all topics and users
- [ ] HPAs scale cart service on custom Redis latency metric
- [ ] External Secrets pull from Secret Manager (no secrets in YAML)
- [ ] `kubectl rollout status` succeeds for all deployments within 5 minutes

## Files to Create

```
k8s/base/kustomization.yaml
k8s/base/namespaces.yaml
k8s/base/network-policies.yaml
k8s/base/istio/peer-authentication.yaml
k8s/base/istio/authorization-policies.yaml
k8s/base/istio/virtual-services.yaml
k8s/base/gateway/deployment.yaml
k8s/base/gateway/service.yaml
k8s/base/gateway/hpa.yaml
k8s/base/gateway/ingress.yaml
k8s/base/gateway/service-account.yaml
k8s/base/auth/deployment.yaml
k8s/base/auth/service.yaml
k8s/base/auth/hpa.yaml
k8s/base/auth/service-account.yaml
k8s/base/catalog/deployment.yaml
k8s/base/catalog/service.yaml
k8s/base/catalog/hpa.yaml
k8s/base/catalog/service-account.yaml
k8s/base/order/deployment.yaml
k8s/base/order/service.yaml
k8s/base/order/hpa.yaml
k8s/base/order/service-account.yaml
k8s/base/customer/deployment.yaml
k8s/base/customer/service.yaml
k8s/base/customer/hpa.yaml
k8s/base/customer/service-account.yaml
k8s/base/cart/deployment.yaml
k8s/base/cart/service.yaml
k8s/base/cart/hpa.yaml
k8s/base/cart/service-account.yaml
k8s/base/streams/deployment.yaml
k8s/base/streams/service.yaml
k8s/base/streams/service-account.yaml
k8s/base/storefront/deployment.yaml
k8s/base/storefront/service.yaml
k8s/base/storefront/hpa.yaml
k8s/base/kafka/kafka-cluster.yaml
k8s/base/kafka/topics.yaml
k8s/base/kafka/users.yaml
k8s/base/kafka/kafka-metrics-config.yaml
k8s/base/redis/statefulset.yaml
k8s/base/redis/service.yaml
k8s/overlays/dev/kustomization.yaml
k8s/overlays/dev/replicas-patch.yaml
k8s/overlays/dev/resource-patch.yaml
k8s/overlays/dev/external-secrets.yaml
k8s/overlays/prod/kustomization.yaml
k8s/overlays/prod/replicas-patch.yaml
k8s/overlays/prod/resource-patch.yaml
k8s/overlays/prod/external-secrets.yaml
k8s/scripts/setup-cluster.sh
k8s/scripts/deploy.sh
```
