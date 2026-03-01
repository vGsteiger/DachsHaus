variable "project_id" {
  type = string
}

variable "alert_email" {
  type        = string
  description = "Email address for monitoring alerts"
  default     = ""
}
