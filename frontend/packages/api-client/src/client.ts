export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.name = "ApiError"
    this.status = status
  }
}

const ACCESS_TOKEN_KEY = "accessToken"
const REFRESH_TOKEN_KEY = "refreshToken"

const API_BASE: string =
  ((import.meta as any).env?.VITE_API_BASE as string | undefined) || "/api"

export class ApiClient {
  private accessToken: string | null = null
  private refreshToken: string | null = null

  constructor() {
    if (typeof localStorage !== "undefined") {
      this.accessToken = localStorage.getItem(ACCESS_TOKEN_KEY)
      this.refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
    }
  }

  setTokens(accessToken: string | null, refreshToken: string | null) {
    this.accessToken = accessToken
    this.refreshToken = refreshToken
    if (typeof localStorage !== "undefined") {
      if (accessToken) localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
      else localStorage.removeItem(ACCESS_TOKEN_KEY)
      if (refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
      else localStorage.removeItem(REFRESH_TOKEN_KEY)
    }
  }

  getTokens(): { accessToken: string | null; refreshToken: string | null } {
    return {
      accessToken: this.accessToken,
      refreshToken: this.refreshToken,
    }
  }

  private buildUrl(endpoint: string): string {
    if (endpoint.startsWith("http")) return endpoint
    const base = API_BASE.replace(/\/$/, "")
    const path = endpoint.startsWith("/") ? endpoint : `/${endpoint}`
    return `${base}${path}`
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {},
  ): Promise<T> {
    const url = this.buildUrl(endpoint)
    const headers = new Headers(options.headers)

    if (!headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json")
    }
    if (this.accessToken) {
      headers.set("Authorization", `Bearer ${this.accessToken}`)
    }

    const init: RequestInit = { ...options, headers }

    let response = await fetch(url, init)

    if (response.status === 401 && this.refreshToken) {
      const refreshed = await this.attemptRefresh()
      if (refreshed) {
        if (this.accessToken) {
          headers.set("Authorization", `Bearer ${this.accessToken}`)
        }
        response = await fetch(url, { ...init, headers })
      }
    }

    if (!response.ok) {
      let message = response.statusText
      try {
        const body = await response.json()
        if (body && typeof body.message === "string") message = body.message
      } catch {
        // ignore parse errors, keep statusText
      }
      throw new ApiError(response.status, message)
    }

    if (response.status === 204) {
      return undefined as T
    }

    return (await response.json()) as T
  }

  private async attemptRefresh(): Promise<boolean> {
    if (!this.refreshToken) return false
    try {
      const res = await fetch(this.buildUrl("/auth/refresh"), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: this.refreshToken }),
      })
      if (!res.ok) return false
      const data = (await res.json()) as {
        accessToken: string
        refreshToken?: string
      }
      this.setTokens(
        data.accessToken,
        data.refreshToken ?? this.refreshToken,
      )
      return true
    } catch {
      return false
    }
  }

  get<T>(endpoint: string, options?: RequestInit): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: "GET" })
  }

  post<T>(
    endpoint: string,
    body?: unknown,
    options?: RequestInit,
  ): Promise<T> {
    return this.request<T>(endpoint, {
      ...options,
      method: "POST",
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  }

  put<T>(endpoint: string, body?: unknown, options?: RequestInit): Promise<T> {
    return this.request<T>(endpoint, {
      ...options,
      method: "PUT",
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  }

  delete<T>(endpoint: string, options?: RequestInit): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: "DELETE" })
  }
}

export const api = new ApiClient()
