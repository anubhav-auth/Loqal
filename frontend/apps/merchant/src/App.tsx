import { Routes, Route, Navigate } from 'react-router-dom'
import { RoleGuard } from '@loqal/auth'
import { Sidebar } from './components/Sidebar'
import { Dashboard } from './pages/Dashboard'
import { Catalog } from './pages/Catalog'
import { Orders } from './pages/Orders'
import { Coupons } from './pages/Coupons'
import { Profile } from './pages/Profile'

export default function App() {
  return (
    <RoleGuard role="ROLE_MERCHANT">
      <div className="flex min-h-screen bg-background font-body text-foreground">
        <Sidebar />
        <main className="flex-1 px-4 py-6 sm:px-8 sm:py-10">
          <div className="mx-auto w-full max-w-6xl">
            <Routes>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/catalog" element={<Catalog />} />
              <Route path="/orders" element={<Orders />} />
              <Route path="/coupons" element={<Coupons />} />
              <Route path="/profile" element={<Profile />} />
            </Routes>
          </div>
        </main>
      </div>
    </RoleGuard>
  )
}
