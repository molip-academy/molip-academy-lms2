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

# 이미지 저장소 설정 시작
# 이미지는 로컬에서 빌드해 여기 올리고 VM이 당겨 쓴다.
# VM에서 빌드하지 않는 이유: e2-small 에서 Gradle 빌드는 느리고 메모리도 빠듯해 배포할 때마다 서버가 멎는다.
resource "google_artifact_registry_repository" "registry_1" {
  location      = var.region
  repository_id = "${var.prefix}-1"
  format        = "DOCKER"
  description   = "몰입 아카데미 백엔드 이미지"
}
# 이미지 저장소 설정 끝

# VM 설정 시작

# VM 역할 생성
resource "google_service_account" "sa_1" {
  account_id   = "${var.prefix}-sa-1"
  display_name = "${var.prefix}-sa-1"
}

# 레지스트리 읽기 권한만 부착
resource "google_artifact_registry_repository_iam_member" "registry_reader_1" {
  location   = google_artifact_registry_repository.registry_1.location
  repository = google_artifact_registry_repository.registry_1.name
  role       = "roles/artifactregistry.reader"
  member     = "serviceAccount:${google_service_account.sa_1.email}"
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

# VM 의 서비스 계정으로 Artifact Registry 에 로그인한다. 키 파일이 없다.
gcloud auth configure-docker ${var.region}-docker.pkg.dev --quiet

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
# 첫 apply 때는 이미지가 아직 없다. 그때는 건너뛰고, deploy.sh 가 푸시 후 띄운다.
if [ -n '${var.app_1_image}' ]; then
  docker pull '${var.app_1_image}'
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
    '${var.app_1_image}'
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
  }

  allow_stopping_for_update = true
}
# VM 설정 끝

# 출력 설정 시작
output "ip_1" {
  description = "DNSZi 에 A 레코드로 넣을 주소"
  value       = google_compute_address.ip_1.address
}

output "registry_1" {
  description = "이미지를 푸시할 곳"
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.registry_1.repository_id}"
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
