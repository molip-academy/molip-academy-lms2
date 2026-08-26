variable "project_id" {
  description = "project_id"
  default     = "molip-academy-lms"
}

variable "region" {
  description = "region"
  default     = "asia-northeast3" // 서울
}

variable "zone" {
  description = "zone"
  default     = "asia-northeast3-a"
}

variable "prefix" {
  description = "Prefix for all resources"
  default     = "molip"
}

variable "machine_type" {
  description = "machine_type"
  default     = "e2-small" // 앱과 postgresql이 한 VM에 함께 뜬다. 1GB(e2-micro)로는 부족하다.
}

variable "app_1_domain" {
  description = "app_1 domain"
  default     = "api.molip.sik2.site"
}

variable "app_1_frontend_origin" {
  description = "app_1 frontend origin"
  default     = "https://molip.sik2.site" // CORS 허용 오리진
}
