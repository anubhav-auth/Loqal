import { Link } from 'react-router-dom'
import { ShoppingBag } from 'lucide-react'
import { Button } from '@loqal/ui'
import { useCartStore } from '@/store/cartStore'
import { useAuth } from '@loqal/auth'

export function Navbar() {
  const count = useCartStore((s) => s.itemCount())
  const { isAuthenticated, logout, user } = useAuth()

  return (
    <header className="sticky top-0 z-40 border-b border-border bg-background/80 backdrop-blur-md">
      <nav className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
        <Link to="/" className="font-display text-2xl font-semibold tracking-tight text-foreground">
          Loqal
        </Link>

        <div className="flex items-center gap-2 sm:gap-4">
          {isAuthenticated ? (
            <span className="hidden font-body text-sm text-foreground-secondary sm:inline">
              {user?.user_id}
            </span>
          ) : (
            <>
              <Button asChild variant="ghost" size="sm">
                <Link to="/login">Sign in</Link>
              </Button>
              <Button asChild variant="default" size="sm">
                <Link to="/register">Sign up</Link>
              </Button>
            </>
          )}

          <Button
            asChild
            variant="outline"
            size="icon"
            className="relative"
            aria-label="Cart"
          >
            <Link to="/cart">
              <ShoppingBag className="h-5 w-5" />
              {count > 0 && (
                <span className="absolute -right-1.5 -top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-accent text-[11px] font-semibold text-white">
                  {count}
                </span>
              )}
            </Link>
          </Button>

          {isAuthenticated && (
            <Button variant="ghost" size="sm" onClick={logout}>
              Sign out
            </Button>
          )}
        </div>
      </nav>
    </header>
  )
}
