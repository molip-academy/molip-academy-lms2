#!/usr/bin/env bash
# 수동 재배포. 평소 배포는 GitHub Actions 가 한다(.github/workflows/deploy-backend.yml).
#
# 이 스크립트는 Actions 가 막혔을 때나 시작 스크립트를 고친 뒤 즉시 반영할 때 쓴다.
# 이미지를 빌드하지 않는다 — 빌드는 Actions 에서만 일어나고, 여기서는 VM 이
# 메타데이터의 app-image 를 다시 읽어 컨테이너를 갈아 끼우게 할 뿐이다.
set -euo pipefail

cd "$(dirname "$0")"

ZONE=$(terraform output -raw ssh_1 | sed -n 's/.*--zone \([^ ]*\).*/\1/p')
INSTANCE=$(terraform output -raw ssh_1 | awk '{print $4}')

if [ $# -gt 0 ]; then
  echo "==> 이미지 태그 지정: $1"
  gcloud compute instances add-metadata "$INSTANCE" --zone "$ZONE" --metadata "app-image=$1"
fi

echo "==> 현재 지정된 이미지"
gcloud compute instances describe "$INSTANCE" --zone "$ZONE" \
  --format="value(metadata.items.filter(key:app-image).extract(value))" | sed 's/^/    /'

echo "==> 시작 스크립트 재실행 (컨테이너 교체)"
# 시작 스크립트는 부팅 때만 자동 실행된다. 재부팅 없이 같은 코드를 다시 돌린다.
gcloud compute ssh "$INSTANCE" --zone "$ZONE" --tunnel-through-iap --command \
  "sudo google_metadata_script_runner startup && sudo docker ps --format 'table {{.Names}}\t{{.Status}}'"
