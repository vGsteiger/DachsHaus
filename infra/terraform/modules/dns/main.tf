# DNS Module - Placeholder
# Cloud DNS zone + A record

resource "google_dns_managed_zone" "main" {
  name     = "dachshaus-zone"
  dns_name = "${var.domain_name}."
}
