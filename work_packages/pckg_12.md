# PKG-12: Terraform Infrastructure

**Status:** Not Started
**Depends on:** Nothing (can be built in parallel with PKG-01)

## Goal

Full GCP infrastructure as code — VPC, GKE, Cloud SQL (×4), Memorystore, Secret Manager, KMS, Artifact Registry, DNS, monitoring.

## Produces

- `vpc` module: VPC, GKE subnet, secondary ranges, Cloud NAT, firewall rules
- `gke` module: private cluster, Workload Identity, etcd encryption, node pools (app + kafka), Istio
- `cloud-sql` module: PG 16, private IP, CMEK, backups, PITR, Query Insights, auto-password
- `memorystore` module: Redis 7.2, TLS, RDB persistence, private access
- `secret-manager` module: gateway HMAC, RSA JWT keypair, KMS key ring, IAM bindings
- `artifact-registry` module: Docker repository
- `dns` module: Cloud DNS zone + A record
- `monitoring` module: alert policies (consumer lag, DB CPU, Redis memory, pod restarts)
- Environment overrides: `dev.tfvars`, `prod.tfvars`

## Acceptance Criteria

- [ ] `terraform plan -var-file=environments/dev.tfvars` shows clean plan
- [ ] `terraform apply` provisions full stack from scratch in <20 minutes
- [ ] Cloud SQL instances have private IP only (no public)
- [ ] Cloud SQL encrypted with CMEK (KMS key auto-rotates every 90 days)
- [ ] GKE cluster is private (no public IPs on nodes)
- [ ] All secrets stored in Secret Manager (not in Terraform state values — use `sensitive = true`)
- [ ] Workload Identity bindings correct: each K8s SA maps to unique GCP SA
- [ ] Alert policies fire correctly
- [ ] `terraform destroy` cleans up everything except KMS keys (`lifecycle: prevent_destroy`)

## Files to Create

```
infra/terraform/main.tf
infra/terraform/variables.tf
infra/terraform/outputs.tf
infra/terraform/versions.tf
infra/terraform/modules/vpc/main.tf
infra/terraform/modules/vpc/variables.tf
infra/terraform/modules/vpc/outputs.tf
infra/terraform/modules/gke/main.tf
infra/terraform/modules/gke/node-pools.tf
infra/terraform/modules/gke/istio.tf
infra/terraform/modules/gke/variables.tf
infra/terraform/modules/gke/outputs.tf
infra/terraform/modules/cloud-sql/main.tf
infra/terraform/modules/cloud-sql/variables.tf
infra/terraform/modules/cloud-sql/outputs.tf
infra/terraform/modules/memorystore/main.tf
infra/terraform/modules/memorystore/variables.tf
infra/terraform/modules/memorystore/outputs.tf
infra/terraform/modules/secret-manager/main.tf
infra/terraform/modules/secret-manager/variables.tf
infra/terraform/modules/secret-manager/outputs.tf
infra/terraform/modules/artifact-registry/main.tf
infra/terraform/modules/artifact-registry/variables.tf
infra/terraform/modules/dns/main.tf
infra/terraform/modules/dns/variables.tf
infra/terraform/modules/monitoring/main.tf
infra/terraform/modules/monitoring/variables.tf
infra/terraform/environments/dev.tfvars
infra/terraform/environments/prod.tfvars
```
