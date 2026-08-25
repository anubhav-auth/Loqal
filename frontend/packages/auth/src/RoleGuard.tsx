import type { ReactNode } from "react"
import { Navigate } from "react-router-dom"
import { useAuth } from "./useAuth"

interface RoleGuardProps {
  role: string
  children: ReactNode
}

export function RoleGuard({ role, children }: RoleGuardProps) {
  const { loading, isAuthenticated, hasRole } = useAuth()

  if (loading) {
    return (
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          minHeight: "100vh",
        }}
      >
        <span>Loading...</span>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (!hasRole(role)) {
    return <Navigate to="/" replace />
  }

  return <>{children}</>
}
