import { Routes, Route, Navigate } from 'react-router-dom'
import { RoleGuard } from '@loqal/auth'
import { Sidebar } from './components/Sidebar'
import { Merchants } from './pages/Merchants'
import { AuditTrail } from './pages/AuditTrail'
import { Metrics } from './pages/Metrics'

export default function App() {
  return (
    <RoleGuard role="ROLE_ADMIN">
      <div className="flex min-h-screen bg-background font-body text-foreground">
        <Sidebar />
        <main className="flex-1 px-4 py-6 sm:px-8 sm:py-10">
          <div className="mx-auto w-full max-w-6xl">
            <Routes>
              <Route path="/" element={<Navigate to="/metrics" replace />} />
              <Route path="/merchants" element={<Merchants />} />
              <Route path="/audit" element={<AuditTrail />} />
              <Route path="/metrics" element={<Metrics />} />
            </Routes>
          </div>
        </main>
      </div>
    </RoleGuard>
  )
}
