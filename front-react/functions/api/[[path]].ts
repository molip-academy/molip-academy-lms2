/**
 * `/api/*` 를 Railway 백엔드로 넘기는 리버스 프록시.
 *
 * 이게 없으면 프론트(*.pages.dev)와 백엔드(*.up.railway.app)가 서로 다른 사이트가 되어
 * 인증 쿠키가 **서드파티 쿠키**가 된다. Safari는 그런 쿠키를 기본 차단하므로 로그인이
 * 아예 되지 않는다. `SameSite=None; Secure`로 바꿔도 마찬가지다.
 *
 * 브라우저가 보는 오리진을 하나로 만들어 그 문제를 통째로 없앤다. 쿠키는 pages.dev의
 * 퍼스트파티 쿠키가 되고, `SameSite=Lax`가 그대로 성립한다.
 */

interface Env {
	/** Railway 백엔드의 오리진. 예: https://molip-api.up.railway.app */
	BACKEND_ORIGIN: string;
}

export const onRequest: PagesFunction<Env> = async ({ request, env }) => {
	if (!env.BACKEND_ORIGIN) {
		return new Response("BACKEND_ORIGIN is not configured", { status: 500 });
	}

	const incoming = new URL(request.url);
	const target = new URL(env.BACKEND_ORIGIN);
	target.pathname = incoming.pathname;
	target.search = incoming.search;

	const proxied = new Request(target.toString(), request);
	proxied.headers.set("X-Forwarded-Host", incoming.host);
	proxied.headers.set("X-Forwarded-Proto", incoming.protocol.replace(":", ""));

	// Origin을 그대로 넘기면 백엔드는 Host(railway)와 Origin(pages.dev)이 다르다는 이유로
	// 크로스 오리진 요청으로 판정해 "Invalid CORS request"로 막는다. 하지만 브라우저 입장에서
	// 이 요청은 이미 same-origin이고, 동일 출처 정책은 pages.dev에서 이미 적용됐다.
	// 여기서 Origin은 프록시가 만들어낸 유령이므로 지운다. CSRF 방어는 토큰이 그대로 담당한다.
	proxied.headers.delete("Origin");

	// 응답을 그대로 돌려준다. 새 Response로 감싸면 Set-Cookie가 여러 개일 때
	// 하나로 합쳐질 수 있어, 액세스·리프레시 쿠키 중 하나가 사라진다.
	return fetch(proxied);
};
