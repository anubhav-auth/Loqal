import { api } from "../client"
import type { AuthResponse, UserProfile } from "../types"

export interface RegisterData {
  email: string
  password: string
  fullName: string
  phoneNumber?: string
  tenantId?: string
}

export const authApi = {
  login(email: string, password: string): Promise<AuthResponse> {
    return api.post<AuthResponse>("/auth/login", { email, password })
  },

  register(data: RegisterData): Promise<AuthResponse> {
    return api.post<AuthResponse>("/auth/register", data)
  },

  refreshToken(token: string): Promise<AuthResponse> {
    return api.post<AuthResponse>("/auth/refresh", { refreshToken: token })
  },

  getProfile(id: string): Promise<UserProfile> {
    return api.get<UserProfile>(`/users/profile/${id}`)
  },
}
