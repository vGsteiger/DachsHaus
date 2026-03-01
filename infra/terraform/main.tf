# DachsHaus Infrastructure - Main Terraform Configuration

terraform {
  required_version = ">= 1.6.0"
}

# VPC Module
module "vpc" {
  source = "./modules/vpc"

  project_id = var.project_id
  region     = var.region
}

# GKE Module
module "gke" {
  source = "./modules/gke"

  project_id = var.project_id
  region     = var.region
  network_id = module.vpc.network_id
  subnet_id  = module.vpc.subnet_id
}

# Cloud SQL Module
module "cloud_sql" {
  source = "./modules/cloud-sql"

  project_id = var.project_id
  region     = var.region
  network_id = module.vpc.network_id
}

# Memorystore (Redis) Module
module "memorystore" {
  source = "./modules/memorystore"

  project_id = var.project_id
  region     = var.region
  network_id = module.vpc.network_id
}

# Kafka Module (Strimzi Operator)
module "kafka" {
  source = "./modules/kafka"

  depends_on = [module.gke]
}

# Secret Manager Module
module "secret_manager" {
  source = "./modules/secret-manager"

  project_id = var.project_id
}

# Artifact Registry Module
module "artifact_registry" {
  source = "./modules/artifact-registry"

  project_id = var.project_id
  region     = var.region
}

# DNS Module
module "dns" {
  source = "./modules/dns"

  project_id  = var.project_id
  domain_name = var.domain_name
}

# Monitoring Module
module "monitoring" {
  source = "./modules/monitoring"

  project_id = var.project_id
}
