import { createContext, useEffect, useState, type ReactNode } from "react"
import { api, authApi } from "@loqal/api-client"
import type { AuthContextValue, JwtPayload } from "./types"

const ACCESS_TOKEN_KEY = "accessToken"
const REFRESH_TOKEN_KEY = "refreshToken"

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function parseJwt(token: string): JwtPayload | null {
  try {
    const parts = token.split(".")
    if (parts.length !== 3) return null
    let payload = parts[1].replace(/-/g, "+").replace(/_/g, "/")
    const pad = payload.length % 4
    if (pad) payload += "=".repeat(4 - pad)
    const json = decodeURIComponent(
      atob(payload)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join(""),
    )
    return JSON.parse(json) as JwtPayload
  } catch {
    return null
  }
}

interface AuthProviderProps {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<JwtPayload | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY)
    if (token) {
      const parsed = parseJwt(token)
      if (parsed && parsed.exp * 1000 > Date.now()) {
        setUser(parsed)
      } else {
        localStorage.removeItem(ACCESS_TOKEN_KEY)
        localStorage.removeItem(REFRESH_TOKEN_KEY)
      }
    }
    setLoading(false)
  }, [])

  const login = async (email: string, password: string) => {
    const res = await authApi.login(email, password)
    localStorage.setItem(ACCESS_TOKEN_KEY, res.accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, res.refreshToken)
    api.setTokens(res.accessToken, res.refreshToken)
    const parsed = parseJwt(res.accessToken)
    setUser(parsed)
  }

  const register = async (data: {
    email: string
    password: string
    fullName: string
    phoneNumber?: string
  }) => {
    await authApi.register(data)
  }

  const logout = () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    api.setTokens(null, null)
    setUser(null)
    window.location.href = "/login"
  }

  const hasRole = (role: string) => user?.roles?.includes(role) ?? false

  const value: AuthContextValue = {
    user,
    loading,
    isAuthenticated: !!user,
    login,
    register,
    logout,
    hasRole,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
