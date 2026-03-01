# Cloud SQL Module - Placeholder
# PostgreSQL 16, private IP, CMEK, TLS enforced, auto-backups, PITR

resource "google_sql_database_instance" "main" {
  name             = "dachshaus-db"
  database_version = "POSTGRES_16"
  region           = var.region

  settings {
    tier = "db-f1-micro"
  }
}
