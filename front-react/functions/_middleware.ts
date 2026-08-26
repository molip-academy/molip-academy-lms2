/**
 * 옛 pages.dev 주소로 들어오면 정식 주소로 넘긴다.
 *
 * 백엔드의 CORS 허용 오리진이 molip.sik2.site 하나뿐이라, pages.dev 에서 열면
 * API 호출이 전부 막힌다. 그대로 두면 "화면은 뜨는데 아무것도 안 되는" 상태가 된다.
 *
 * 미리보기 배포(<해시>.molip-academy-lms2.pages.dev)는 건드리지 않는다.
 */
const LEGACY_HOST = "molip-academy-lms2.pages.dev";
const CANONICAL_HOST = "molip.sik2.site";

export const onRequest: PagesFunction = ({ request, next }) => {
  const url = new URL(request.url);

  if (url.hostname === LEGACY_HOST) {
    url.hostname = CANONICAL_HOST;
    // 302 를 쓴다. 301 은 브라우저가 영구 캐시해서, 새 주소에 문제가 생겨도
    // 되돌릴 방법이 사라진다.
    return Response.redirect(url.toString(), 302);
  }

  return next();
};
