#!/usr/bin/env bash
# 백엔드 이미지를 빌드해 Artifact Registry 에 올리고, VM 이 그걸 당겨 쓰게 한다.
#
# VM 에서 빌드하지 않는 이유: e2-small 에서 Gradle 빌드는 느리고 메모리도 빠듯해
# 배포할 때마다 서버가 몇 분씩 멎는다.
#
# 컨테이너를 띄우는 방법은 main.tf 의 startup-script 한 곳에만 적혀 있다.
# 여기서 docker run 을 다시 쓰면 환경변수가 두 곳으로 갈라져 반드시 어긋난다.
# 그래서 이미지 태그만 갱신하고 시작 스크립트를 다시 실행시킨다.
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f secrets.tf ]; then
  echo "secrets.tf 가 없습니다. secrets.tf.default 를 복사해 값을 채우세요." >&2
  exit 1
fi

REGISTRY=$(terraform output -raw registry_1)
ZONE=$(terraform output -raw ssh_1 | sed -n 's/.*--zone \([^ ]*\).*/\1/p')
INSTANCE=$(terraform output -raw ssh_1 | awk '{print $4}')
TAG="$(git rev-parse --short HEAD)"
IMAGE="$REGISTRY/app-1:$TAG"

echo "==> 빌드: $IMAGE"
# GCE 는 amd64 다. 애플 실리콘에서 빌드하므로 플랫폼을 명시한다.
docker build --platform linux/amd64 -t "$IMAGE" ../back

echo "==> 푸시"
gcloud auth configure-docker "${REGISTRY%%/*}" --quiet
docker push "$IMAGE"

echo "==> secrets.tf 의 app_1_image 갱신"
# VM 이 재생성돼도 같은 이미지로 뜨도록 코드에 남긴다.
sed -i.bak "s|^\( *default *= *\)\".*\" *// *Artifact Registry|\1\"$IMAGE\" // Artifact Registry|" secrets.tf
grep -q "$IMAGE" secrets.tf || {
  echo "secrets.tf 의 app_1_image 를 직접 아래 값으로 바꿔 주세요:" >&2
  echo "  default = \"$IMAGE\"" >&2
}
rm -f secrets.tf.bak

echo "==> 메타데이터 반영"
terraform apply -auto-approve

echo "==> 시작 스크립트 재실행 (컨테이너 교체)"
# 시작 스크립트는 부팅 때만 자동 실행된다. 재부팅 없이 같은 코드를 다시 돌린다.
gcloud compute ssh "$INSTANCE" --zone "$ZONE" --tunnel-through-iap --command \
  "sudo google_metadata_script_runner startup && sudo docker ps --format 'table {{.Names}}\t{{.Status}}'"

echo
echo "==> 완료: $IMAGE"
