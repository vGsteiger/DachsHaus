# Secret Manager Module - Placeholder
# Gateway HMAC secret, RSA key pair for JWT, KMS keys, IAM bindings

resource "random_password" "hmac_secret" {
  length  = 64
  special = true
}
