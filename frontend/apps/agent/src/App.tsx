import { Routes, Route, Navigate } from 'react-router-dom'
import { RoleGuard } from '@loqal/auth'
import { BottomNav } from './components/BottomNav'
import { Dashboard } from './pages/Dashboard'
import { Assignments } from './pages/Assignments'
import { OtpVerification } from './pages/OtpVerification'
import { Profile } from './pages/Profile'

export default function App() {
  return (
    <RoleGuard role="ROLE_DELIVERY_AGENT">
      <div className="flex min-h-screen flex-col bg-background font-body text-foreground">
        <main className="flex-1 px-4 pb-24 pt-6">
          <div className="mx-auto w-full max-w-md">
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/assignments" element={<Assignments />} />
              <Route path="/otp/:orderId" element={<OtpVerification />} />
              <Route path="/profile" element={<Profile />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </div>
        </main>
        <BottomNav />
      </div>
    </RoleGuard>
  )
}
