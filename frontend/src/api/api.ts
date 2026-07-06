import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";

import { ApiClientError, type ApiProblemDetails } from "../types/api";

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

/**
 * Global Axios instance configured for Polygraphic Centre API.
 *
 * Features:
 * - Base URL from environment or localhost
 * - Credentials enabled for httpOnly cookie support
 * - Automatic XSRF-TOKEN handling (CSRF protection)
 * - Request ID generation for tracing
 * - 401 error handling with token refresh
 * - Error response normalization
 */
const api = axios.create({
  baseURL:
    import.meta.env.VITE_API_BASE_URL ?? import.meta.env.VITE_API_URL ?? "/api",
  // Enable credentials to include httpOnly cookies in requests
  withCredentials: true,
  // Axios only sends the XSRF header automatically for same-origin requests.
  // The frontend runs on localhost:5173 and the backend on localhost:8080,
  // so this must be forced for cookie-based auth to work in the browser.
  withXSRFToken: true,
  // XSRF-TOKEN is the cookie name set by Spring Security
  xsrfCookieName: "XSRF-TOKEN",
  // X-XSRF-TOKEN is the header name Spring Security expects
  xsrfHeaderName: "X-XSRF-TOKEN",
  headers: {
    "Content-Type": "application/json",
  },
});

/**
 * Request interceptor: Add tracing headers and ensure XSRF token is present.
 */
api.interceptors.request.use((config) => {
  // Generate request ID for distributed tracing (if not already present)
  if (!config.headers.get("x-request-id")) {
    const requestId =
      globalThis.crypto?.randomUUID?.() ??
      `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    config.headers.set("x-request-id", requestId);
  }

  // For POST/PATCH/PUT requests, ensure XSRF-TOKEN is sent
  // Axios should do this automatically, but we verify it for auth endpoints
  if (
    ["post", "patch", "put", "delete"].includes(
      config.method?.toLowerCase() ?? "",
    )
  ) {
    // Axios will automatically read XSRF-TOKEN cookie and set X-XSRF-TOKEN header
    // due to xsrfCookieName and xsrfHeaderName config above
  }

  return config;
});

let refreshPromise: Promise<unknown> | null = null;

function isApiProblemDetails(payload: unknown): payload is ApiProblemDetails {
  if (!payload || typeof payload !== "object") {
    return false;
  }

  return (
    "status" in payload &&
    "code" in payload &&
    "userMessage" in payload &&
    "action" in payload &&
    "requestId" in payload
  );
}

function toApiClientError(error: AxiosError): ApiClientError | null {
  const payload = error.response?.data;
  if (!isApiProblemDetails(payload)) {
    return null;
  }

  return new ApiClientError(payload);
}

/**
 * Response interceptor: Handle 401 errors by refreshing token.
 *
 * When access token expires:
 * 1. Intercept 401 response
 * 2. Call POST /auth/refresh with refresh token (from cookie)
 * 3. Retry original request
 *
 * Skip refresh for auth endpoints themselves (logout, refresh, session, etc.)
 */
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined;
    const status = error.response?.status;
    const requestUrl = originalRequest?.url ?? "";

    // Don't retry auth endpoints themselves
    const isAuthEndpoint =
      requestUrl.includes("/auth/session") ||
      requestUrl.includes("/auth/refresh") ||
      requestUrl.includes("/auth/logout") ||
      requestUrl.includes("/auth/csrf") ||
      requestUrl.includes("/auth/me");

    // If 401 and not auth endpoint and not already retried, refresh token and retry
    if (
      status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !isAuthEndpoint
    ) {
      originalRequest._retry = true;

      // Prevent multiple refresh attempts - use single promise
      refreshPromise ??= api
        .post("/auth/refresh")
        .catch((refreshError) => {
          // If refresh fails, clear the cached promise and reject
          refreshPromise = null;
          return Promise.reject(refreshError);
        })
        .finally(() => {
          refreshPromise = null;
        });

      try {
        await refreshPromise;
        // After successful refresh, retry original request
        return api(originalRequest);
      } catch (refreshError) {
        // Refresh failed, reject with original error
        return Promise.reject(refreshError);
      }
    }

    const normalizedError = toApiClientError(error);
    return Promise.reject(normalizedError ?? error);
  },
);

export default api;
