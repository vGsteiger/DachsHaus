# Monitoring Module - Placeholder
# Alert policies for consumer lag, DB CPU, Redis memory, pod restart rate

resource "google_monitoring_notification_channel" "email" {
  display_name = "Email Notification"
  type         = "email"
}
