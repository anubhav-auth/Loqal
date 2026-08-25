export interface JwtPayload {
  sub: string
  user_id: string
  tenant_id?: string
  roles?: string[]
  exp: number
  iat: number
}

export interface AuthContextValue {
  user: JwtPayload | null
  loading: boolean
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (data: { email: string; password: string; fullName: string; phoneNumber?: string }) => Promise<void>
  logout: () => void
  hasRole: (role: string) => boolean
}
