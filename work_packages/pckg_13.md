# PKG-13: CI/CD Pipelines

**Status:** Not Started
**Depends on:** PKG-01, PKG-11, PKG-12

## Goal

GitHub Actions workflows for CI (lint, test, plan) and CD (build, push, deploy).

## Produces

- `ci.yml` — on PR: lint + test Kotlin services (Gradle), lint + test TS (Turborepo)
- `terraform-plan.yml` — on PR: `terraform plan`, comment diff on PR
- `deploy.yml` — on merge to main: terraform apply → parallel docker build/push → kubectl apply → smoke test
- `dependabot.yml` — automated dependency updates
- `CODEOWNERS` — code review routing

## Interface Contracts

### CI Triggers
- `ci.yml`: on PR to main
- `terraform-plan.yml`: on PR to main (changes to `infra/**`)
- `deploy.yml`: on push to main

### Secrets Required
```
WIF_PROVIDER:    Workload Identity Federation provider
TERRAFORM_SA:    GCP SA for terraform (has broad IAM)
DEPLOY_SA:       GCP SA for build+deploy (Artifact Registry write, GKE deploy)
```

### Image Tagging
```
${REGION}-docker.pkg.dev/${PROJECT_ID}/dachshaus/${service}:${git_sha}
${REGION}-docker.pkg.dev/${PROJECT_ID}/dachshaus/${service}:latest
```

### Deploy Steps
1. `terraform apply` (infra changes)
2. `docker build + push` (parallel matrix: 6 Kotlin + 2 TS)
3. `kustomize set image` (updates all image refs to current SHA)
4. `kubectl apply -k overlays/${env}`
5. `kubectl rollout status` (wait for all deployments)
6. Smoke test: `curl gateway /health`

## Acceptance Criteria

- [ ] CI catches compilation errors and test failures before merge
- [ ] Terraform plan comments on PR with human-readable diff
- [ ] Deploy workflow completes in <15 minutes (parallel builds)
- [ ] Failed smoke test fails the workflow (prevents silent broken deploys)
- [ ] Rollback possible via `kubectl rollout undo` (tested)

## Files to Create

```
.github/workflows/ci.yml
.github/workflows/terraform-plan.yml
.github/workflows/deploy.yml
.github/dependabot.yml
.github/CODEOWNERS
```
