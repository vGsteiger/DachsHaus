# Monitoring Module - Placeholder
# Alert policies for consumer lag, DB CPU, Redis memory, pod restart rate

resource "google_monitoring_notification_channel" "email" {
  count        = var.alert_email != "" ? 1 : 0
  display_name = "Email Notification"
  type         = "email"
  labels = {
    email_address = var.alert_email
  }
}
