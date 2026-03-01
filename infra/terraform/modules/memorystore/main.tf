# Memorystore Module - Placeholder
# Redis 7.2, TLS transit, RDB persistence, private service access

resource "google_redis_instance" "main" {
  name           = "dachshaus-redis"
  tier           = "BASIC"
  memory_size_gb = 1
  region         = var.region
}
