# JWT를 httpOnly 쿠키에 담고, 리프레시 토큰은 DB에 저장한다

React SPA와 Spring API가 서로 다른 오리진에 있어 인증 수단을 골라야 했다. 액세스 토큰(30분)과 리프레시 토큰(14일)을 **둘 다 httpOnly 쿠키**로 내려, JS가 토큰을 읽을 수 없게 했다. 리프레시 토큰은 **DB에 저장**해 로그아웃과 강제 만료가 실제로 동작하게 한다.

## Considered Options

- **`Authorization: Bearer` + `localStorage`**: 프론트가 토큰을 직접 다루므로 XSS 한 번에 토큰이 그대로 털린다.
- **`Authorization: Bearer` + 일반(비 httpOnly) 쿠키**: `localStorage`보다 나쁘다. XSS 노출은 같은데 자동 전송까지 되어 CSRF 표면이 추가된다.
- **세션 쿠키**: 사실상 동등하게 안전하고 더 단순하다. JWT를 택한 이유는 액세스 토큰 검증에 저장소 조회가 없다는 점 하나다.

## Consequences

- 쿠키는 브라우저가 자동으로 붙이므로 **CSRF 방어가 필수**다. Spring Security의 `CookieCsrfTokenRepository.withHttpOnlyFalse()`를 써서 `XSRF-TOKEN` 쿠키 ↔ `X-XSRF-TOKEN` 헤더로 막는다.
- 리프레시 토큰을 저장하는 순간 **무상태(stateless)라는 JWT의 이점은 액세스 토큰 검증에만 남는다.** 이건 폐기 가능성과 맞바꾼 것이며, 의도된 것이다.
- 프론트에는 토큰을 보관하거나 헤더에 붙이는 코드가 아예 없다. `credentials: 'include'`만 있으면 된다.
- 로컬에서 `localhost:5173`과 `localhost:8080`은 **같은 사이트**(포트는 사이트 판정에 포함되지 않는다)라 `SameSite=Lax`로 충분하고 HTTPS가 필요 없다. 프론트와 백엔드가 진짜 다른 사이트에 배포되면 그때 `SameSite=None; Secure`로 바꿔야 하므로, `SameSite` 값과 CORS 허용 오리진은 프로퍼티로 빼 두었다.
