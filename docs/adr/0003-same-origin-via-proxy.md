# 프론트와 백엔드를 한 오리진으로 합친다 — Cloudflare가 `/api/*`를 Railway로 넘긴다

프론트는 Cloudflare Pages(`molip-academy-lms2.pages.dev`), 백엔드는 Railway(`back-production-0533.up.railway.app`)에 배포한다. 두 곳은 **서로 다른 사이트**다.

[ADR 0001](./0001-jwt-in-httponly-cookies.md)은 그럴 때 `SameSite=None; Secure`로 바꾸면 된다고 적었다. **그 처방은 틀렸다.** `SameSite=None`은 쿠키를 서드파티 쿠키로 만드는데, Safari의 ITP는 서드파티 쿠키를 기본 차단하고 Firefox는 Total Cookie Protection으로 격리하며 Chrome도 단계적으로 없애는 중이다. 인증이 전적으로 쿠키에 의존하므로, 이는 **특정 브라우저 사용자가 로그인 자체를 못 하는** 상태를 뜻한다.

그래서 값을 바꾸는 대신 **브라우저가 보는 오리진을 하나로 만든다.** Cloudflare Pages Function이 `/api/*`를 Railway로 넘기고, 브라우저는 `pages.dev` 하나만 본다. 쿠키는 퍼스트파티가 되고 `SameSite=Lax`가 그대로 성립한다.

## Considered Options

- **`SameSite=None; Secure`로 크로스 사이트 유지**: 고친 곳은 적지만 Safari에서 로그인이 안 된다. 배포가 아니라 장애다.
- **공통 등록 도메인의 서브도메인** (`molip.example.com` / `api.molip.example.com`): 같은 사이트가 되어 `Lax`로 충분하다. 프록시도 필요 없고 지연도 없다. **도메인을 사면 이쪽이 더 낫다** — 그때 이 ADR을 다시 연다.
- **토큰을 `localStorage`로 옮기고 `Authorization` 헤더 사용**: 쿠키 문제는 사라지지만 XSS 한 번에 토큰이 털린다. [ADR 0001](./0001-jwt-in-httponly-cookies.md)이 이미 기각한 선택지다.

## Consequences

- **CORS와 `SameSite` 설정은 로컬 개발에만 쓰인다.** 실서버에서는 same-origin이라 발동하지 않는다. 프로퍼티는 그대로 두되, 값이 비어 있다고 실서버가 깨지는 게 아니라는 점을 알아야 한다.
- **프록시는 `Origin` 헤더를 지운다.** 그대로 넘기면 백엔드가 Host(railway)와 Origin(pages.dev)이 다르다고 판단해 `Invalid CORS request`로 막는다. 브라우저는 이미 same-origin으로 처리했으므로 그 Origin은 프록시가 만들어낸 유령이다. CSRF 방어는 토큰이 그대로 담당한다.
- **응답은 새 `Response`로 감싸지 않고 그대로 돌려준다.** 감싸면 `Set-Cookie`가 여러 개일 때 하나로 합쳐져 액세스·리프레시 쿠키 중 하나가 사라진다.
- 모든 API 요청이 Cloudflare를 한 번 더 거치므로 지연이 조금 늘고 Worker 요청 수를 소비한다.
- Railway 백엔드 URL은 공개돼 있어 직접 호출할 수 있다. 다만 그 경로로는 브라우저 쿠키가 실리지 않으므로 인증이 성립하지 않는다.
