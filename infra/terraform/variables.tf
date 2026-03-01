variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "region" {
  description = "GCP Region"
  type        = string
  default     = "europe-west6"
}

variable "domain_name" {
  description = "Domain name for the application"
  type        = string
  default     = "dachshaus.com"
}

variable "environment" {
  description = "Environment name (dev, prod)"
  type        = string
}
