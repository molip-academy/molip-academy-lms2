terraform {
  // google 라이브러리 불러옴
  required_providers {
    google = {
      source = "hashicorp/google"
    }
  }
}

# GCP 설정 시작
provider "google" {
  project = var.project_id
  region  = var.region
  zone    = var.zone
}
# GCP 설정 끝

# 네트워크 설정 시작
resource "google_compute_network" "vpc_1" {
  name                    = "${var.prefix}-vpc-1"
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "subnet_1" {
  name          = "${var.prefix}-subnet-1"
  ip_cidr_range = "10.0.0.0/24"
  region        = var.region
  network       = google_compute_network.vpc_1.id
}

# 인터넷에 여는 문은 80과 443 둘뿐이다.
resource "google_compute_firewall" "firewall_1" {
  name    = "${var.prefix}-firewall-1"
  network = google_compute_network.vpc_1.name

  allow {
    protocol = "tcp"
    ports    = ["80", "443"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["${var.prefix}-app-1"]
}

# SSH(22)는 인터넷에 열지 않는다. IAP 터널로만 들어간다.
# 22번을 공개하면 온종일 무차별 대입 시도를 받는다. 35.235.240.0/20 은 Google이 고정해 둔 IAP 대역이다.
resource "google_compute_firewall" "firewall_2" {
  name    = "${var.prefix}-firewall-2"
  network = google_compute_network.vpc_1.name

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = ["35.235.240.0/20"]
  target_tags   = ["${var.prefix}-app-1"]
}

# DNS A 레코드가 이 주소를 가리켜야 하므로 고정이어야 한다. VM을 다시 만들어도 주소는 남는다.
resource "google_compute_address" "ip_1" {
  name   = "${var.prefix}-ip-1"
  region = var.region
}
# 네트워크 설정 끝

# GitHub Actions 연동 설정 시작
# 이미지는 GitHub Actions 가 빌드해 ghcr.io 에 올리고, VM 이 당겨 쓴다.
# 로컬이나 VM 에서 빌드하지 않는 이유: 로컬 Docker 에 의존하지 않기 위해서고,
# e2-small 에서 Gradle 빌드는 느려 배포할 때마다 서버가 멎기 때문이다.

# Actions 가 키 파일 없이 GCP 에 들어오는 통로.
# 서비스 계정 키를 만들어 GitHub Secrets 에 넣는 방식은 그 키가 유출되면 끝이고 만료도 없다.
resource "google_iam_workload_identity_pool" "pool_1" {
  workload_identity_pool_id = "${var.prefix}-github-pool-1"
  display_name              = "${var.prefix}-github-pool-1"
}

resource "google_iam_workload_identity_pool_provider" "provider_1" {
  workload_identity_pool_id          = google_iam_workload_identity_pool.pool_1.workload_identity_pool_id
  workload_identity_pool_provider_id = "${var.prefix}-github-provider-1"

  attribute_mapping = {
    "google.subject"       = "assertion.sub"
    "attribute.repository" = "assertion.repository"
  }

  # 이 저장소에서 온 토큰만 받는다. 없으면 GitHub 의 어떤 저장소든 이 프로젝트에 들어올 수 있다.
  attribute_condition = "assertion.repository == '${var.github_repo_1}'"

  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com"
  }
}

# Actions 가 빌려 쓰는 신원
resource "google_service_account" "sa_deploy_1" {
  account_id   = "${var.prefix}-sa-deploy-1"
  display_name = "${var.prefix}-sa-deploy-1"
}

resource "google_service_account_iam_member" "wif_binding_1" {
  service_account_id = google_service_account.sa_deploy_1.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.pool_1.name}/attribute.repository/${var.github_repo_1}"
}

# 배포에 필요한 최소 권한: 메타데이터 갱신, IAP 터널, sudo 가능한 SSH
resource "google_project_iam_member" "deploy_instance_admin_1" {
  project = var.project_id
  role    = "roles/compute.instanceAdmin.v1"
  member  = "serviceAccount:${google_service_account.sa_deploy_1.email}"
}

resource "google_project_iam_member" "deploy_iap_1" {
  project = var.project_id
  role    = "roles/iap.tunnelResourceAccessor"
  member  = "serviceAccount:${google_service_account.sa_deploy_1.email}"
}

resource "google_project_iam_member" "deploy_os_admin_1" {
  project = var.project_id
  role    = "roles/compute.osAdminLogin"
  member  = "serviceAccount:${google_service_account.sa_deploy_1.email}"
}

# VM 의 서비스 계정으로 행세할 수 있어야 SSH 가 성립한다.
resource "google_service_account_iam_member" "deploy_act_as_1" {
  service_account_id = google_service_account.sa_1.name
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${google_service_account.sa_deploy_1.email}"
}
# GitHub Actions 연동 설정 끝

# VM 설정 시작

# VM 역할 생성
resource "google_service_account" "sa_1" {
  account_id   = "${var.prefix}-sa-1"
  display_name = "${var.prefix}-sa-1"
}

# 부팅 로그를 Cloud Logging 에서 볼 수 있게 한다. 기동이 실패하면 여기서 원인을 찾는다.
resource "google_project_iam_member" "log_writer_1" {
  project = var.project_id
  role    = "roles/logging.logWriter"
  member  = "serviceAccount:${google_service_account.sa_1.email}"
}

locals {
  vm_user_data_base = <<-END_OF_FILE
#!/bin/bash
set -euo pipefail

# 가상 메모리 4GB 설정
# e2-small 은 2GB 뿐인데 JVM 과 postgresql 이 함께 산다. 스왑이 없으면 OOM 으로 죽는다.
if [ ! -f /swapfile ]; then
  dd if=/dev/zero of=/swapfile bs=128M count=32
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  sh -c 'echo "/swapfile swap swap defaults 0 0" >> /etc/fstab'
  echo 'vm.swappiness = 10' >> /etc/sysctl.conf
  sysctl -p
fi

# 타임존 설정
timedatectl set-timezone Asia/Seoul

# 환경변수 세팅(/etc/environment)
echo 'APP_1_DOMAIN=${var.app_1_domain}' >> /etc/environment
echo 'APP_1_DB_NAME=${var.app_1_db_name}' >> /etc/environment
source /etc/environment

# 도커 설치 및 실행/활성화
if ! command -v docker >/dev/null 2>&1; then
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -y
  apt-get install -y ca-certificates curl gnupg
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
  echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" > /etc/apt/sources.list.d/docker.list
  apt-get update -y
  apt-get install -y docker-ce docker-ce-cli containerd.io
  systemctl enable docker
  systemctl start docker
fi

# 도커 네트워크 생성
docker network create common || true

# ghcr 로그인. 패키지가 비공개라 토큰이 필요하다.
# 토큰이 아직 없으면 건너뛴다 — 여기서 실패하면 postgresql 과 caddy 까지 못 뜬다.
if [ -n '${var.github_token_1}' ]; then
  echo '${var.github_token_1}' | docker login ghcr.io -u '${var.github_owner_1}' --password-stdin
else
  echo 'github_token_1 이 비어 있어 ghcr 로그인을 건너뛴다. 비공개 이미지는 당길 수 없다.'
fi

# postgresql 설치
docker rm -f pg_1 || true
docker run -d \
  --name pg_1 \
  --restart unless-stopped \
  --network common \
  -v /dockerProjects/pg_1/volumes/var/lib/postgresql/data:/var/lib/postgresql/data \
  -e 'POSTGRES_DB=${var.app_1_db_name}' \
  -e 'POSTGRES_USER=${var.app_1_db_user}' \
  -e 'POSTGRES_PASSWORD=${var.password_1}' \
  -e 'TZ=Asia/Seoul' \
  postgres:17-alpine

# caddy 설치 (TLS 종료 + 리버스 프록시)
# ${var.app_1_domain} 이 이 VM 을 가리켜야 Let's Encrypt 인증서가 발급된다.
# DNS A 레코드가 아직 없으면 caddy 는 계속 재시도한다 — 정상이다.
mkdir -p /dockerProjects/caddy_1/volumes/etc/caddy
cat > /dockerProjects/caddy_1/volumes/etc/caddy/Caddyfile <<'CADDYFILE'
{
  email ${var.acme_email_1}
}

${var.app_1_domain} {
  encode gzip
  reverse_proxy app_1:8080
}
CADDYFILE

docker rm -f caddy_1 || true
docker run -d \
  --name caddy_1 \
  --restart unless-stopped \
  --network common \
  -p 80:80 \
  -p 443:443 \
  -e TZ=Asia/Seoul \
  -v /dockerProjects/caddy_1/volumes/etc/caddy/Caddyfile:/etc/caddy/Caddyfile:ro \
  -v /dockerProjects/caddy_1/volumes/data:/data \
  -v /dockerProjects/caddy_1/volumes/config:/config \
  caddy:2-alpine

# app 설치
# 어떤 이미지를 띄울지는 인스턴스 메타데이터의 app-image 가 정한다.
# Terraform 변수로 두면 배포할 때마다 코드를 고쳐 apply 해야 하고, GitHub Actions 가
# 태그를 갱신할 방법이 없다. 메타데이터라 커밋 SHA 태그를 그대로 쓸 수 있어 추적도 된다.
APP_IMAGE=$(curl -fsS -H 'Metadata-Flavor: Google' \
  http://metadata.google.internal/computeMetadata/v1/instance/attributes/app-image 2>/dev/null || echo '')

if [ -n "$APP_IMAGE" ]; then
  docker pull "$APP_IMAGE"
  docker rm -f app_1 || true
  docker run -d \
    --name app_1 \
    --restart unless-stopped \
    --network common \
    -e 'SPRING_PROFILES_ACTIVE=prod' \
    -e 'SPRING_DATASOURCE_URL=jdbc:postgresql://pg_1:5432/${var.app_1_db_name}' \
    -e 'SPRING_DATASOURCE_USERNAME=${var.app_1_db_user}' \
    -e 'SPRING_DATASOURCE_PASSWORD=${var.password_1}' \
    -e 'JWT_SECRET=${var.jwt_secret_1}' \
    -e 'COOKIE_SAME_SITE=Lax' \
    -e 'COOKIE_SECURE=true' \
    -e 'CORS_ALLOWED_ORIGINS=${var.app_1_frontend_origin}' \
    -e 'TZ=Asia/Seoul' \
    -e 'JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=45' \
    "$APP_IMAGE"
else
  echo 'app-image 메타데이터가 비어 있어 앱을 띄우지 않는다. Actions 가 배포하면 채워진다.'
fi

END_OF_FILE
}

resource "google_compute_instance" "instance_1" {
  name         = "${var.prefix}-instance-1"
  machine_type = var.machine_type
  zone         = var.zone
  tags         = ["${var.prefix}-app-1"]

  boot_disk {
    initialize_params {
      image = "ubuntu-os-cloud/ubuntu-2404-lts-amd64"
      size  = 20
      type  = "pd-balanced"
    }
  }

  # postgresql 데이터가 이 부팅 디스크 위에 있다.
  # VM 을 다시 만들거나 terraform destroy 하면 회원 일지가 함께 사라진다.
  # 데이터를 지키려면 별도 영구 디스크로 옮기고 prevent_destroy 를 걸어야 한다.

  network_interface {
    subnetwork = google_compute_subnetwork.subnet_1.id

    access_config {
      nat_ip = google_compute_address.ip_1.address
    }
  }

  service_account {
    email  = google_service_account.sa_1.email
    scopes = ["cloud-platform"]
  }

  metadata = {
    # OS Login 을 켜면 IAM 으로 SSH 접근을 관리한다. 키를 VM 에 심지 않는다.
    enable-oslogin = "TRUE"
    startup-script = local.vm_user_data_base

    # GitHub Actions 가 배포할 때마다 이 값을 커밋 SHA 태그로 갱신한다.
    app-image = ""
  }

  allow_stopping_for_update = true

  lifecycle {
    # 배포는 Actions 가 이 값을 바꾸는 방식이다. Terraform 이 매번 ""로 되돌리면 안 된다.
    ignore_changes = [metadata["app-image"]]
  }
}
# VM 설정 끝

# 출력 설정 시작
output "ip_1" {
  description = "DNSZi 에 A 레코드로 넣을 주소"
  value       = google_compute_address.ip_1.address
}

output "registry_1" {
  description = "이미지를 푸시할 곳"
  value       = "ghcr.io/${var.github_repo_1}"
}

# 아래 둘은 GitHub Actions 워크플로에 그대로 들어간다. 비밀이 아니다.
output "wif_provider_1" {
  description = "Actions 의 google-github-actions/auth 에 넣을 workload_identity_provider"
  value       = google_iam_workload_identity_pool_provider.provider_1.name
}

output "wif_service_account_1" {
  description = "Actions 가 빌려 쓸 서비스 계정"
  value       = google_service_account.sa_deploy_1.email
}

output "ssh_1" {
  description = "22번을 공개하지 않으므로 IAP 터널로 들어간다"
  value       = "gcloud compute ssh ${var.prefix}-instance-1 --zone ${var.zone} --tunnel-through-iap"
}

output "next_steps" {
  description = "apply 이후 사람이 해야 하는 일"
  value       = <<-EOT

    1) DNSZi 에 A 레코드를 넣는다
         ${var.app_1_domain}  A  ${google_compute_address.ip_1.address}

    2) 이미지를 빌드해 푸시하고 앱을 띄운다
         ./deploy.sh

    3) 인증서가 발급됐는지 확인한다 (DNS 전파 후 몇 분)
         curl -I https://${var.app_1_domain}/api/v1/csrf

    DNS 가 이 IP 를 가리키기 전에는 caddy 가 인증서를 받지 못하고 계속 재시도한다.
  EOT
}
# 출력 설정 끝
