# Loqal Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete 5-app React frontend (landing, merchant, customer, admin, agent) with a shared component library, gallery-like light aesthetic, and mobile-first design.

**Architecture:** Turborepo monorepo with 4 shared packages (ui, api-client, auth, config) and 5 Vite React apps. Each app is a standalone SPA routed by path prefix behind Kong. Single Docker image with nginx serves all apps.

**Tech Stack:** React 18, Vite, TypeScript, Tailwind CSS, shadcn/ui, Zustand, TanStack Query, React Router v6, React Hook Form + Zod, Framer Motion, Lucide React

---

## Phase 0: Monorepo Scaffold

### Task 0-1: Initialize Turborepo + shared config

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/turbo.json`
- Create: `frontend/packages/config/package.json`
- Create: `frontend/packages/config/tsconfig.base.json`
- Create: `frontend/packages/config/.eslintrc.js`
- Create: `frontend/packages/config/tailwind.config.ts`
- Create: `frontend/packages/config/postcss.config.js`
- Create: `frontend/.gitignore`

- [ ] **Step 1: Create root package.json**

```json
{
  "name": "loqal-frontend",
  "private": true,
  "scripts": {
    "dev": "turbo dev",
    "build": "turbo build",
    "lint": "turbo lint",
    "test": "turbo test",
    "typecheck": "turbo typecheck"
  },
  "devDependencies": {
    "turbo": "^2.0.0",
    "typescript": "^5.5.0"
  },
  "packageManager": "npm@10.0.0"
}
```

- [ ] **Step 2: Create turbo.json**

```json
{
  "$schema": "https://turbo.build/schema.json",
  "tasks": {
    "build": {
      "dependsOn": ["^build"],
      "outputs": ["dist/**"]
    },
    "dev": {
      "cache": false,
      "persistent": true
    },
    "lint": {},
    "test": {
      "dependsOn": ["build"]
    },
    "typecheck": {
      "dependsOn": ["^build"]
    }
  }
}
```

- [ ] **Step 3: Create shared config package**

```bash
mkdir -p frontend/packages/config
```

Create `frontend/packages/config/package.json`:
```json
{
  "name": "@loqal/config",
  "version": "0.0.0",
  "private": true,
  "exports": {
    "./tsconfig": "./tsconfig.base.json",
    "./tailwind": "./tailwind.config.ts",
    "./eslint": "./.eslintrc.js"
  }
}
```

- [ ] **Step 4: Create base TypeScript config**

Create `frontend/packages/config/tsconfig.base.json`:
```json
{
  "compilerOptions": {
    "target": "ES2022",
    "lib": ["ES2023", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "exclude": ["node_modules"]
}
```

- [ ] **Step 5: Create Tailwind config with design tokens**

Create `frontend/packages/config/tailwind.config.ts`:
```typescript
import type { Config } from 'tailwindcss'

export default {
  content: [],
  theme: {
    extend: {
      colors: {
        background: '#fdfcfa',
        surface: '#ffffff',
        border: '#f0ebe6',
        foreground: {
          DEFAULT: '#2d2520',
          secondary: '#8a7d72',
          muted: '#999999',
        },
        accent: {
          DEFAULT: '#c4956a',
          light: '#fdf0e6',
        },
        success: '#22c55e',
        error: '#ef4444',
      },
      fontFamily: {
        display: ['Cormorant Garamond', 'serif'],
        body: ['Plus Jakarta Sans', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      borderRadius: {
        'card': '12px',
        'button': '50px',
      },
      boxShadow: {
        'sm': '0 1px 3px rgba(0,0,0,0.04)',
        'md': '0 4px 16px rgba(0,0,0,0.06)',
        'lg': '0 8px 32px rgba(0,0,0,0.08)',
        'hover': '0 8px 24px rgba(0,0,0,0.1)',
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
} satisfies Config
```

- [ ] **Step 6: Create .gitignore**

```
node_modules
dist
.turbo
*.tsbuildinfo
.env
.env.local
.superpowers
```

- [ ] **Step 7: Install and verify**

```bash
cd frontend && npm install
```

- [ ] **Step 8: Commit**

```bash
git add frontend/
git commit -m "chore: initialize turborepo monorepo with shared config"
```

---

### Task 0-2: Create shared UI package (shadcn/ui + Tailwind)

**Files:**
- Create: `frontend/packages/ui/package.json`
- Create: `frontend/packages/ui/tsconfig.json`
- Create: `frontend/packages/ui/src/index.ts`
- Create: `frontend/packages/ui/src/globals.css`
- Create: `frontend/packages/ui/components.json`
- Create: `frontend/packages/ui/src/components/ui/button.tsx`
- Create: `frontend/packages/ui/src/components/ui/card.tsx`
- Create: `frontend/packages/ui/src/components/ui/input.tsx`
- Create: `frontend/packages/ui/src/components/ui/badge.tsx`
- Create: `frontend/packages/ui/src/components/ui/avatar.tsx`
- Create: `frontend/packages/ui/src/components/ui/dialog.tsx`
- Create: `frontend/packages/ui/src/components/ui/toast.tsx`
- Create: `frontend/packages/ui/src/components/ui/skeleton.tsx`
- Create: `frontend/packages/ui/src/lib/utils.ts`

- [ ] **Step 1: Create package.json**

```json
{
  "name": "@loqal/ui",
  "version": "0.0.0",
  "private": true,
  "type": "module",
  "main": "./src/index.ts",
  "types": "./src/index.ts",
  "exports": {
    ".": "./src/index.ts",
    "./globals.css": "./src/globals.css"
  },
  "dependencies": {
    "@radix-ui/react-avatar": "^1.1.0",
    "@radix-ui/react-dialog": "^1.1.0",
    "@radix-ui/react-slot": "^1.1.0",
    "class-variance-authority": "^0.7.0",
    "clsx": "^2.1.0",
    "framer-motion": "^11.0.0",
    "lucide-react": "^0.400.0",
    "react": "^18.3.0",
    "react-dom": "^18.3.0",
    "tailwind-merge": "^2.4.0",
    "tailwindcss-animate": "^1.0.7"
  },
  "devDependencies": {
    "@types/react": "^18.3.0",
    "@types/react-dom": "^18.3.0",
    "tailwindcss": "^3.4.0",
    "typescript": "^5.5.0"
  }
}
```

- [ ] **Step 2: Create globals.css with design tokens**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    --background: 40 30% 99%;
    --foreground: 20 20% 13%;
    --accent: 28 50% 60%;
    --accent-light: 28 60% 95%;
    --border: 24 30% 94%;
    --muted: 0 0% 60%;
  }

  body {
    @apply bg-background text-foreground font-body antialiased;
  }
}
```

- [ ] **Step 3: Create utils.ts**

```typescript
import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
```

- [ ] **Step 4: Create button component (shadcn/ui pattern)**

```typescript
import * as React from "react"
import { Slot } from "@radix-ui/react-slot"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "../../lib/utils"

const buttonVariants = cva(
  "inline-flex items-center justify-center whitespace-nowrap font-body text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        default: "bg-foreground text-white hover:bg-foreground/90 rounded-button",
        outline: "border border-border bg-transparent hover:bg-accent-light text-foreground rounded-button",
        ghost: "hover:bg-accent-light text-foreground rounded-button",
        link: "text-accent underline-offset-4 hover:underline",
      },
      size: {
        default: "h-10 px-6 py-2",
        sm: "h-9 px-4",
        lg: "h-12 px-8",
        icon: "h-10 w-10",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button"
    return (
      <Comp className={cn(buttonVariants({ variant, size, className }))} ref={ref} {...props} />
    )
  }
)
Button.displayName = "Button"

export { Button, buttonVariants }
```

- [ ] **Step 5: Create card, input, badge, avatar, skeleton, dialog, toast components**

Follow shadcn/ui patterns for each. Create minimal versions:

- `card.tsx`: Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter
- `input.tsx`: Input with focus ring in accent color
- `badge.tsx`: Badge with variants (default, secondary, accent, success, error)
- `avatar.tsx`: Avatar, AvatarImage, AvatarFallback using Radix
- `skeleton.tsx`: Skeleton with pulse animation
- `dialog.tsx`: Dialog, DialogTrigger, DialogContent, DialogHeader, DialogTitle, DialogDescription
- `toast.tsx`: Simple toast with auto-dismiss

- [ ] **Step 6: Create index.ts barrel export**

```typescript
export { Button, buttonVariants } from "./components/ui/button"
export type { ButtonProps } from "./components/ui/button"
export { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from "./components/ui/card"
export { Input } from "./components/ui/input"
export { Badge, badgeVariants } from "./components/ui/badge"
export { Avatar, AvatarImage, AvatarFallback } from "./components/ui/avatar"
export { Skeleton } from "./components/ui/skeleton"
export { Dialog, DialogTrigger, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "./components/ui/dialog"
export { cn } from "./lib/utils"
```

- [ ] **Step 7: Commit**

```bash
git add frontend/packages/ui/
git commit -m "feat(ui): shared component library — button, card, input, badge, avatar, dialog, toast, skeleton"
```

---

### Task 0-3: Create shared API client package

**Files:**
- Create: `frontend/packages/api-client/package.json`
- Create: `frontend/packages/api-client/src/index.ts`
- Create: `frontend/packages/api-client/src/client.ts`
- Create: `frontend/packages/api-client/src/types.ts`
- Create: `frontend/packages/api-client/src/endpoints/auth.ts`
- Create: `frontend/packages/api-client/src/endpoints/products.ts`
- Create: `frontend/packages/api-client/src/endpoints/orders.ts`
- Create: `frontend/packages/api-client/src/endpoints/payments.ts`
- Create: `frontend/packages/api-client/src/endpoints/chat.ts`

- [ ] **Step 1: Create client.ts with token management**

```typescript
const API_BASE = import.meta.env.VITE_API_BASE || '/api'

interface RequestOptions extends RequestInit {
  params?: Record<string, string>
}

class ApiClient {
  private getAccessToken(): string | null {
    return localStorage.getItem('accessToken')
  }

  private getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken')
  }

  private setTokens(accessToken: string, refreshToken: string) {
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
  }

  private clearTokens() {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  async request<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
    const { params, ...fetchOptions } = options
    const url = new URL(`${API_BASE}${endpoint}`, window.location.origin)
    if (params) {
      Object.entries(params).forEach(([key, value]) => url.searchParams.set(key, value))
    }

    const token = this.getAccessToken()
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(fetchOptions.headers as Record<string, string>),
    }
    if (token) headers['Authorization'] = `Bearer ${token}`

    let response = await fetch(url.toString(), { ...fetchOptions, headers })

    // Auto-refresh on 401
    if (response.status === 401 && this.getRefreshToken()) {
      const refreshed = await this.tryRefresh()
      if (refreshed) {
        headers['Authorization'] = `Bearer ${this.getAccessToken()}`
        response = await fetch(url.toString(), { ...fetchOptions, headers })
      }
    }

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Request failed' }))
      throw new ApiError(response.status, error.message || 'Request failed')
    }

    return response.json()
  }

  private async tryRefresh(): Promise<boolean> {
    try {
      const response = await fetch(`${API_BASE}/auth/refresh`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.getRefreshToken()}`,
          'Content-Type': 'application/json',
        },
      })
      if (!response.ok) {
        this.clearTokens()
        window.location.href = '/login'
        return false
      }
      const data = await response.json()
      this.setTokens(data.accessToken, data.refreshToken)
      return true
    } catch {
      this.clearTokens()
      return false
    }
  }

  // Convenience methods
  get<T>(endpoint: string, params?: Record<string, string>) {
    return this.request<T>(endpoint, { method: 'GET', params })
  }

  post<T>(endpoint: string, body?: unknown) {
    return this.request<T>(endpoint, { method: 'POST', body: body ? JSON.stringify(body) : undefined })
  }

  put<T>(endpoint: string, body?: unknown) {
    return this.request<T>(endpoint, { method: 'PUT', body: body ? JSON.stringify(body) : undefined })
  }

  delete<T>(endpoint: string) {
    return this.request<T>(endpoint, { method: 'DELETE' })
  }
}

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message)
  }
}

export const api = new ApiClient()
```

- [ ] **Step 2: Create types.ts matching backend DTOs**

```typescript
// Auth
export interface AuthResponse { accessToken: string; refreshToken: string }
export interface UserProfile { userId: string; fullName: string; email: string; phoneNumber?: string; tenantId: string; address?: Address }
export interface Address { street?: string; city?: string; state?: string; postalCode?: string; country?: string }

// Products
export interface Product { id: string; name: string; description?: string; priceMinor: number; quantity: number; imageUrls: string[]; merchantId: string }
export interface ProductPrice { productId: string; priceMinor: number; quantityAvailable: number; available: boolean }

// Orders
export interface Order { id: string; customerId: string; currentStatus: string; totalAmountMinor: number; discountAmountMinor: number; finalAmountMinor: number; razorpayOrderId?: string; items: OrderItem[] }
export interface OrderItem { productId: string; quantity: number; priceAtPurchaseMinor: number }
export interface OrderRequest { items: { productId: string; quantity: number }[]; merchantId: string; couponCode?: string }
export interface OrderCreationResponse { orderId: string; razorpayOrderId: string }

// Payments
export interface PaymentInitiation { paymentId: string; razorpayOrderId: string; amountMinor: number; currency: string }

// Promotions
export interface Coupon { id: string; code: string; discountType: string; value: number; minOrderValueMinor?: number; maxDiscountMinor?: number; validFrom: string; validUntil: string; active: boolean }
export interface DiscountResult { discountMinor: number }

// Delivery
export interface Delivery { id: string; orderId: string; agentId: string; status: string; pickupOtp?: string; deliveredOtp?: string }
export interface Agent { id: string; tenantId: string; userId: string; status: string; currentLat?: number; currentLng?: number }

// Chat
export interface ChatMessage { id: string; roomId: string; senderId: string; senderRole: string; content: string; createdAt: string }

// Notifications
export interface Notification { id: string; channel: string; recipient: string; template: string; body: string; status: string }
```

- [ ] **Step 3: Create endpoint modules**

```typescript
// endpoints/auth.ts
import { api } from '../client'
import type { AuthResponse, UserProfile } from '../types'

export const authApi = {
  login: (email: string, password: string) =>
    api.post<AuthResponse>('/auth/login', { email, password }),
  register: (data: { email: string; password: string; fullName: string; phoneNumber?: string }) =>
    api.post('/auth/register', data),
  refreshToken: (token: string) =>
    api.request<AuthResponse>('/auth/refresh', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),
  getProfile: (id: string) =>
    api.get<UserProfile>(`/users/profile/${id}`),
}
```

```typescript
// endpoints/products.ts
import { api } from '../client'
import type { Product, ProductPrice } from '../types'

export const productsApi = {
  getById: (id: string) => api.get<Product>(`/products/${id}`),
  getByMerchant: (merchantId: string) => api.get<Product[]>('/products/merchant', { merchantId }),
  search: (query: string) => api.get<Product[]>('/products/search', { query }),
  findPrice: (id: string) => api.get<ProductPrice>(`/products/${id}/price`),
  create: (merchantId: string, product: Partial<Product>) => api.post<Product>(`/products/${merchantId}`, product),
  update: (productId: string, merchantId: string, data: unknown) => api.put(`/products/${productId}/${merchantId}`, data),
  delete: (id: string, merchantId: string) => api.delete(`/products/${id}/${merchantId}`),
}
```

```typescript
// endpoints/orders.ts
import { api } from '../client'
import type { Order, OrderRequest, OrderCreationResponse } from '../types'

export const ordersApi = {
  create: (request: OrderRequest, idempotencyKey: string) =>
    api.post<OrderCreationResponse>('/api/orders', request),
  getById: (id: string) => api.get<Order>(`/api/orders/${id}`),
  getByUser: () => api.get<Order[]>('/api/orders'),
  cancel: (id: string) => api.post(`/api/orders/${id}/cancel`),
  return: (id: string) => api.post(`/api/orders/${id}/return`),
}
```

```typescript
// endpoints/payments.ts
import { api } from '../client'

export const paymentsApi = {
  handleWebhook: (payload: unknown, signature: string) =>
    api.post('/payments/webhook', payload),
}
```

```typescript
// endpoints/chat.ts
import { api } from '../client'
import type { ChatMessage } from '../types'

export const chatApi = {
  history: (roomId: string) => api.get<ChatMessage[]>(`/communication/chat/${roomId}/messages`),
  postMessage: (roomId: string, content: string) =>
    api.post<ChatMessage>('/communication/chat/messages', { roomId, content }),
}
```

- [ ] **Step 4: Create index.ts barrel export**

```typescript
export { api, ApiError } from './client'
export type * from './types'
export { authApi } from './endpoints/auth'
export { productsApi } from './endpoints/products'
export { ordersApi } from './endpoints/orders'
export { paymentsApi } from './endpoints/payments'
export { chatApi } from './endpoints/chat'
```

- [ ] **Step 5: Commit**

```bash
git add frontend/packages/api-client/
git commit -m "feat(api-client): typed API wrapper with auto-refresh and endpoint modules"
```

---

### Task 0-4: Create shared auth package

**Files:**
- Create: `frontend/packages/auth/package.json`
- Create: `frontend/packages/auth/src/index.ts`
- Create: `frontend/packages/auth/src/AuthProvider.tsx`
- Create: `frontend/packages/auth/src/useAuth.ts`
- Create: `frontend/packages/auth/src/RoleGuard.tsx`
- Create: `frontend/packages/auth/src/types.ts`

- [ ] **Step 1: Create AuthProvider with JWT management**

```typescript
// AuthProvider.tsx
import React, { createContext, useEffect, useState, useCallback } from 'react'
import { authApi, ApiError } from '@loqal/api-client'
import type { AuthContextValue, JwtPayload } from './types'

function parseJwt(token: string): JwtPayload | null {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(base64))
  } catch {
    return null
  }
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<JwtPayload | null>(null)
  const [loading, setLoading] = useState(true)

  const loadUser = useCallback(() => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      const payload = parseJwt(token)
      if (payload && payload.exp * 1000 > Date.now()) {
        setUser(payload)
      } else {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
      }
    }
    setLoading(false)
  }, [])

  useEffect(() => { loadUser() }, [loadUser])

  const login = async (email: string, password: string) => {
    const { accessToken, refreshToken } = await authApi.login(email, password)
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    setUser(parseJwt(accessToken))
  }

  const register = async (data: { email: string; password: string; fullName: string; phoneNumber?: string }) => {
    await authApi.register(data)
  }

  const logout = () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    setUser(null)
    window.location.href = '/login'
  }

  const hasRole = (role: string) => user?.roles?.includes(role) ?? false

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, hasRole, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  )
}
```

- [ ] **Step 2: Create useAuth hook**

```typescript
import { useContext } from 'react'
import { AuthContext } from './AuthProvider'

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}
```

- [ ] **Step 3: Create RoleGuard component**

```typescript
import { Navigate } from 'react-router-dom'
import { useAuth } from './useAuth'

export function RoleGuard({ role, children }: { role: string; children: React.ReactNode }) {
  const { hasRole, loading, isAuthenticated } = useAuth()

  if (loading) return <div className="flex items-center justify-center min-h-screen"><div className="animate-pulse text-foreground-muted">Loading...</div></div>
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (!hasRole(role)) return <Navigate to="/" replace />

  return <>{children}</>
}
```

- [ ] **Step 4: Create types**

```typescript
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
```

- [ ] **Step 5: Commit**

```bash
git add frontend/packages/auth/
git commit -m "feat(auth): auth provider, JWT management, role guard"
```

---

## Phase 1: Landing Page App

### Task 1-1: Scaffold landing app

**Files:**
- Create: `frontend/apps/landing/package.json`
- Create: `frontend/apps/landing/tsconfig.json`
- Create: `frontend/apps/landing/vite.config.ts`
- Create: `frontend/apps/landing/index.html`
- Create: `frontend/apps/landing/src/main.tsx`
- Create: `frontend/apps/landing/src/App.tsx`
- Create: `frontend/apps/landing/src/index.css`

- [ ] **Step 1: Create package.json**

```json
{
  "name": "@loqal/landing",
  "version": "0.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite --port 3000",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint src --ext .ts,.tsx",
    "typecheck": "tsc --noEmit"
  },
  "dependencies": {
    "@loqal/ui": "workspace:*",
    "react": "^18.3.0",
    "react-dom": "^18.3.0",
    "react-router-dom": "^6.26.0",
    "framer-motion": "^11.0.0",
    "lucide-react": "^0.400.0"
  },
  "devDependencies": {
    "@types/react": "^18.3.0",
    "@types/react-dom": "^18.3.0",
    "@vitejs/plugin-react": "^4.3.0",
    "autoprefixer": "^10.4.0",
    "postcss": "^8.4.0",
    "tailwindcss": "^3.4.0",
    "typescript": "^5.5.0",
    "vite": "^5.4.0",
    "vite-plugin-pwa": "^0.20.0"
  }
}
```

- [ ] **Step 2: Create vite.config.ts**

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
})
```

- [ ] **Step 3: Create index.html**

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Loqal — Commerce for Local Merchants</title>
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
  <link href="https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,300;0,400;0,600;1,300;1,400&family=Plus+Jakarta+Sans:wght@300;400;500;600&display=swap" rel="stylesheet" />
</head>
<body>
  <div id="root"></div>
  <script type="module" src="/src/main.tsx"></script>
</body>
</html>
```

- [ ] **Step 4: Create main.tsx and App.tsx with router**

```tsx
// main.tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import '@loqal/ui/globals.css'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter><App /></BrowserRouter>
  </React.StrictMode>
)
```

```tsx
// App.tsx
import { Routes, Route } from 'react-router-dom'
import { Hero } from './sections/Hero'
import { Features } from './sections/Features'
import { HowItWorks } from './sections/HowItWorks'
import { Testimonials } from './sections/Testimonials'
import { CTA } from './sections/CTA'
import { Footer } from './sections/Footer'
import { Navbar } from './components/Navbar'

export default function App() {
  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main>
        <Hero />
        <Features />
        <HowItWorks />
        <Testimonials />
        <CTA />
      </main>
      <Footer />
    </div>
  )
}
```

- [ ] **Step 5: Create index.css with Tailwind directives**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

html { scroll-behavior: smooth; }
```

- [ ] **Step 6: Commit**

```bash
git add frontend/apps/landing/
git commit -m "feat(landing): scaffold Vite React app with router and Tailwind"
```

---

### Task 1-2: Build landing page sections

**Files:**
- Create: `frontend/apps/landing/src/components/Navbar.tsx`
- Create: `frontend/apps/landing/src/sections/Hero.tsx`
- Create: `frontend/apps/landing/src/sections/Features.tsx`
- Create: `frontend/apps/landing/src/sections/HowItWorks.tsx`
- Create: `frontend/apps/landing/src/sections/Testimonials.tsx`
- Create: `frontend/apps/landing/src/sections/CTA.tsx`
- Create: `frontend/apps/landing/src/sections/Footer.tsx`

- [ ] **Step 1: Create Navbar**

```tsx
import { useState } from 'react'
import { Menu, X } from 'lucide-react'

export function Navbar() {
  const [open, setOpen] = useState(false)
  return (
    <nav className="fixed top-0 left-0 right-0 z-50 bg-background/80 backdrop-blur-lg border-b border-border">
      <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
        <a href="/" className="font-display text-xl font-semibold text-foreground">Loqal</a>
        <div className="hidden md:flex items-center gap-8 text-sm text-foreground-secondary">
          <a href="#features" className="hover:text-foreground transition-colors">Features</a>
          <a href="#how-it-works" className="hover:text-foreground transition-colors">How It Works</a>
          <a href="#testimonials" className="hover:text-foreground transition-colors">Merchants</a>
          <a href="/login" className="bg-foreground text-white px-5 py-2 rounded-button hover:bg-foreground/90 transition-colors">Get Started</a>
        </div>
        <button className="md:hidden" onClick={() => setOpen(!open)}>
          {open ? <X size={20} /> : <Menu size={20} />}
        </button>
      </div>
      {open && (
        <div className="md:hidden bg-surface border-b border-border px-6 py-4 space-y-3">
          <a href="#features" className="block text-sm text-foreground-secondary">Features</a>
          <a href="#how-it-works" className="block text-sm text-foreground-secondary">How It Works</a>
          <a href="/login" className="block text-sm bg-foreground text-white px-5 py-2 rounded-button text-center">Get Started</a>
        </div>
      )}
    </nav>
  )
}
```

- [ ] **Step 2: Create Hero section**

```tsx
import { motion } from 'framer-motion'

export function Hero() {
  return (
    <section className="pt-24 pb-16 px-6 md:pt-32 md:pb-24">
      <div className="max-w-6xl mx-auto grid md:grid-cols-2 gap-12 items-center">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}>
          <p className="text-xs text-accent uppercase tracking-[4px] mb-4">Commerce Reimagined</p>
          <h1 className="font-display text-4xl md:text-6xl font-light text-foreground leading-tight">
            Your store,<br /><em className="italic">beautifully</em> presented.
          </h1>
          <div className="w-8 h-px bg-accent my-6" />
          <p className="text-foreground-secondary text-sm leading-relaxed max-w-md">
            Launch your digital storefront with stunning product galleries, seamless checkout, and real-time delivery tracking. Built for merchants who know their customers by name.
          </p>
          <div className="flex gap-3 mt-8">
            <a href="/register" className="bg-foreground text-white px-6 py-3 rounded-button text-sm font-medium hover:bg-foreground/90 transition-colors">Start Free</a>
            <a href="#features" className="border border-border px-6 py-3 rounded-button text-sm text-foreground-secondary hover:bg-accent-light transition-colors">See Examples</a>
          </div>
        </motion.div>
        <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.6, delay: 0.2 }} className="relative">
          <div className="aspect-[4/3] rounded-card overflow-hidden shadow-lg">
            <img src="https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=800&q=80" alt="Merchant using Loqal" className="w-full h-full object-cover" />
          </div>
        </motion.div>
      </div>
    </section>
  )
}
```

- [ ] **Step 3: Create Features, HowItWorks, Testimonials, CTA, Footer sections**

Each section follows the same pattern: Framer Motion animations, the design system fonts/colors, responsive grid layouts, and Unsplash images where appropriate.

- [ ] **Step 4: Run dev server and verify**

```bash
cd frontend && npm run dev --workspace=@loqal/landing
```

Open http://localhost:3000 — verify landing page renders correctly.

- [ ] **Step 5: Commit**

```bash
git add frontend/apps/landing/src/
git commit -m "feat(landing): hero, features, how-it-works, testimonials, CTA, footer sections"
```

---

## Phase 2: Customer App

### Task 2-1: Scaffold customer app + auth flow

**Files:**
- Create: `frontend/apps/customer/` (same scaffold as landing)
- Create: `frontend/apps/customer/src/pages/Login.tsx`
- Create: `frontend/apps/customer/src/pages/Register.tsx`
- Create: `frontend/apps/customer/src/pages/Storefront.tsx`
- Create: `frontend/apps/customer/src/pages/ProductDetail.tsx`
- Create: `frontend/apps/customer/src/pages/Cart.tsx`
- Create: `frontend/apps/customer/src/pages/Checkout.tsx`
- Create: `frontend/apps/customer/src/pages/OrderTracking.tsx`
- Create: `frontend/apps/customer/src/components/ProductCard.tsx`
- Create: `frontend/apps/customer/src/components/CartItem.tsx`
- Create: `frontend/apps/customer/src/components/Navbar.tsx`
- Create: `frontend/apps/customer/src/store/cartStore.ts`

- [ ] **Step 1-10:** Scaffold app, create pages with image-forward design, wire up TanStack Query for data fetching, Zustand for cart state, Framer Motion for page transitions

---

## Phase 3: Merchant App

### Task 3-1: Scaffold merchant dashboard

**Files:**
- Create: `frontend/apps/merchant/` (same scaffold)
- Create: `frontend/apps/merchant/src/pages/Dashboard.tsx`
- Create: `frontend/apps/merchant/src/pages/Catalog.tsx`
- Create: `frontend/apps/merchant/src/pages/Orders.tsx`
- Create: `frontend/apps/merchant/src/pages/Coupons.tsx`
- Create: `frontend/apps/merchant/src/pages/Profile.tsx`
- Create: `frontend/apps/merchant/src/components/Sidebar.tsx`
- Create: `frontend/apps/merchant/src/components/OrderCard.tsx`

- [ ] **Step 1-10:** Build dashboard with stats, catalog CRUD, order management, coupon management, storefront profile editor

---

## Phase 4: Admin & Agent Apps

### Task 4-1: Admin panel

- [ ] Build admin app with merchant onboarding, audit trail, platform metrics

### Task 4-2: Delivery agent app

- [ ] Build agent app with clock in/out, assignment cards, OTP verification, location tracking

---

## Phase 5: Docker + Deployment

### Task 5-1: Docker build

**Files:**
- Create: `frontend/docker/nginx.conf`
- Create: `frontend/docker/Dockerfile`

- [ ] **Step 1: Create multi-stage Dockerfile**

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY . .
RUN npm ci && npm run build

FROM nginx:alpine
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/apps/landing/dist /usr/share/nginx/html/landing
COPY --from=builder /app/apps/customer/dist /usr/share/nginx/html/customer
COPY --from=builder /app/apps/merchant/dist /usr/share/nginx/html/merchant
COPY --from=builder /app/apps/admin/dist /usr/share/nginx/html/admin
COPY --from=builder /app/apps/agent/dist /usr/share/nginx/html/agent
EXPOSE 80
```

- [ ] **Step 2: Create nginx.conf routing by path**

- [ ] **Step 3: Build and test Docker image**

- [ ] **Step 4: Commit**

---

## Phase 6: Testing

### Task 6-1: Unit tests

- [ ] Vitest + React Testing Library for critical components
- [ ] Test auth flow, cart operations, order creation

### Task 6-2: E2E tests

- [ ] Playwright for critical flows: register → browse → add to cart → checkout → track order

---

## Summary

| Phase | Tasks | Description |
|---|---|---|
| 0 | 4 | Monorepo scaffold, shared packages |
| 1 | 2 | Landing page |
| 2 | 1 | Customer app |
| 3 | 1 | Merchant app |
| 4 | 2 | Admin + Agent apps |
| 5 | 1 | Docker deployment |
| 6 | 2 | Testing |
| **Total** | **13** | **5 apps + shared infra** |
