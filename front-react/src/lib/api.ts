const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  errors: Record<string, string>;

  constructor(status: number, message: string, errors: Record<string, string> = {}) {
    super(message);
    this.status = status;
    this.errors = errors;
  }

  /** 항목별 메시지가 있으면 그것을, 없으면 전체 메시지를 보여준다. */
  fieldMessage(field: string): string | undefined {
    return this.errors[field];
  }
}

/**
 * CSRF 토큰은 쿠키가 아니라 서버가 본문으로 준 값을 쓴다.
 *
 * 쿠키에서 읽으려면 프론트(molip.sik2.site)가 API(api.molip.sik2.site)의 쿠키를
 * 읽을 수 있어야 하는데, 같은 사이트여도 오리진 단위로 막는 브라우저가 있다.
 * 검증용 쿠키는 브라우저가 알아서 실어 보내므로, JS 는 값만 알면 된다.
 */
let csrfToken: string | null = null;
let csrfHeader = "X-XSRF-TOKEN";

type Options = {
  method?: string;
  body?: unknown;
  retryOnUnauthorized?: boolean;
};

/**
 * 인증은 전적으로 httpOnly 쿠키로 이뤄진다 (ADR 0001). 그래서 이 파일 어디에도
 * 토큰을 읽거나 저장하는 코드가 없다. 하는 일은 두 가지뿐이다:
 * 쿠키를 함께 보내는 것, 그리고 CSRF 토큰을 헤더로 되돌려주는 것.
 */
async function request<T>(path: string, options: Options = {}): Promise<T> {
  const { method = "GET", body, retryOnUnauthorized = true } = options;

  const headers: Record<string, string> = {};
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  if (method !== "GET") {
    if (!csrfToken) await primeCsrf();
    if (csrfToken) headers[csrfHeader] = csrfToken;
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    credentials: "include",
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  // 액세스 토큰이 만료됐을 뿐일 수 있다. 재발급해 보고 원래 요청을 이어간다.
  if (response.status === 401 && retryOnUnauthorized && path !== "/api/v1/auth/refresh") {
    const refreshed = await tryRefresh();
    if (refreshed) {
      return request<T>(path, { ...options, retryOnUnauthorized: false });
    }
  }

  if (!response.ok) {
    throw await toError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

type ErrorPayload = { message?: string; errors?: Record<string, string> };

async function toError(response: Response): Promise<ApiError> {
  let message = "요청을 처리하지 못했습니다.";
  let errors: Record<string, string> = {};
  try {
    const payload = (await response.json()) as ErrorPayload;
    if (payload?.message) message = payload.message;
    if (payload?.errors) errors = payload.errors;
  } catch {
    // 본문이 없는 응답(401 등)은 기본 메시지를 쓴다.
  }
  if (response.status === 401) {
    message = message === "요청을 처리하지 못했습니다." ? "로그인이 필요합니다." : message;
  }
  return new ApiError(response.status, message, errors);
}

async function tryRefresh(): Promise<boolean> {
  try {
    await request<void>("/api/v1/auth/refresh", { method: "POST", retryOnUnauthorized: false });
    return true;
  } catch {
    return false;
  }
}

/** 첫 쓰기 요청 전에 CSRF 토큰을 받아 둔다. 쓰기 직전에도 자동으로 불린다. */
export async function primeCsrf(): Promise<void> {
  try {
    const response = await fetch(`${BASE_URL}/api/v1/csrf`, { credentials: "include" });
    if (!response.ok) return;
    const payload = (await response.json()) as { token?: string; headerName?: string };
    if (payload.token) csrfToken = payload.token;
    if (payload.headerName) csrfHeader = payload.headerName;
  } catch {
    // 토큰을 못 받아도 여기서 막지 않는다. 실제 요청이 403 으로 알려준다.
  }
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: "POST", body }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: "PUT", body }),
  delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
};
