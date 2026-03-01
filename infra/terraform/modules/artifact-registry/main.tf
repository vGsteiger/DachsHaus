# Artifact Registry Module - Placeholder
# Docker repository in europe-west6

resource "google_artifact_registry_repository" "main" {
  location      = var.region
  repository_id = "dachshaus"
  format        = "DOCKER"
}
