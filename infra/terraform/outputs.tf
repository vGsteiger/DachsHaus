output "cluster_endpoint" {
  description = "GKE cluster endpoint"
  value       = module.gke.cluster_endpoint
  sensitive   = true
}

output "cluster_ca_certificate" {
  description = "GKE cluster CA certificate"
  value       = module.gke.cluster_ca_certificate
  sensitive   = true
}

output "database_connection_name" {
  description = "Cloud SQL connection name"
  value       = module.cloud_sql.connection_name
}

output "redis_host" {
  description = "Redis host"
  value       = module.memorystore.host
}

output "artifact_registry_url" {
  description = "Artifact Registry URL"
  value       = module.artifact_registry.registry_url
}
