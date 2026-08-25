import { useState } from 'react'
import { NavLink } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  LayoutDashboard,
  Package,
  Receipt,
  Ticket,
  User,
  Menu,
  X,
  Store,
} from 'lucide-react'
import { cn } from '@loqal/ui'

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/catalog', label: 'Catalog', icon: Package },
  { to: '/orders', label: 'Orders', icon: Receipt },
  { to: '/coupons', label: 'Coupons', icon: Ticket },
  { to: '/profile', label: 'Profile', icon: User },
]

export function Sidebar() {
  const [open, setOpen] = useState(false)

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="fixed left-4 top-4 z-50 flex h-10 w-10 items-center justify-center rounded-button border border-border bg-surface text-foreground shadow-sm lg:hidden"
        aria-label="Toggle navigation"
      >
        {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            className="fixed inset-0 z-40 bg-foreground/40 lg:hidden"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setOpen(false)}
          />
        )}
      </AnimatePresence>

      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-40 w-64 transform border-r border-border bg-surface transition-transform duration-300',
          'lg:static lg:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        <div className="flex items-center gap-2 px-6 py-6">
          <span className="flex h-9 w-9 items-center justify-center rounded-button bg-accent-light text-accent">
            <Store className="h-5 w-5" />
          </span>
          <span className="font-display text-2xl font-semibold text-foreground">
            Loqal
          </span>
        </div>

        <nav className="flex flex-col gap-1 px-3">
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-card px-4 py-3 font-body text-sm transition-colors',
                  isActive
                    ? 'bg-accent-light text-accent'
                    : 'text-foreground-secondary hover:bg-accent-light/60 hover:text-foreground'
                )
              }
            >
              <Icon className="h-5 w-5" />
              {label}
            </NavLink>
          ))}
        </nav>
      </aside>
    </>
  )
}
