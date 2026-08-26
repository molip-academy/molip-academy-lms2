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

variable "github_repo_1" {
  description = "github_repo_1"
  default     = "molip-academy/molip-academy-lms2" // 이 저장소에서 온 Actions 토큰만 GCP 에 접근할 수 있다
}

variable "github_branch_1" {
  description = "github_branch_1"
  default     = "main" // 이 브랜치에서만 배포한다
}
