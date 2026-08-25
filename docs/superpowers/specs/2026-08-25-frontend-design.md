# Loqal Frontend — Design Spec

**Date:** 2026-08-25
**Status:** Approved
**Stack:** React (Vite) + Turborepo + Tailwind CSS

---

## 1. Vision

A light, gallery-like commerce experience for local merchants and their customers. The design evokes a high-end editorial magazine — generous white space, restrained typography, large imagery, and soft rounded edges. Mobile-first layout across all 5 apps.

## 2. Design System

### 2.1 Typography

| Role | Font | Weight | Usage |
|---|---|---|---|
| Display | Cormorant Garamond | 300, 400, 600 | Headlines, hero text, prices |
| Body | Plus Jakarta Sans | 300, 400, 500, 600 | UI text, labels, descriptions |
| Monospace | JetBrains Mono | 400 | Order IDs, codes |

**Scale:** 8px base, modular scale 1.25 (8, 10, 12.5, 16, 20, 25, 31, 39, 49)

### 2.2 Color Palette

```
Background:    #fdfcfa  (warm white)
Surface:       #ffffff  (cards, modals)
Border:        #f0ebe6  (soft warm gray)
Text Primary:  #2d2520  (deep espresso)
Text Secondary:#8a7d72  (warm gray)
Text Muted:    #999999  (light gray)
Accent:        #c4956a  (warm copper/terracotta)
Accent Light:  #fdf0e6  (copper tint)
Success:       #22c55e
Error:         #ef4444
```

### 2.3 Spacing & Radius

- **Base unit:** 4px
- **Card radius:** 12-16px
- **Button radius:** 50px (pill) or 12px
- **Input radius:** 12px
- **Image radius:** 10-12px
- **Section padding:** 24-32px mobile, 48-64px desktop

### 2.4 Shadows

```css
--shadow-sm: 0 1px 3px rgba(0,0,0,0.04)
--shadow-md: 0 4px 16px rgba(0,0,0,0.06)
--shadow-lg: 0 8px 32px rgba(0,0,0,0.08)
--shadow-hover: 0 8px 24px rgba(0,0,0,0.1)
```

### 2.5 Components

- **Buttons:** Pill-shaped primary (filled), outlined secondary
- **Cards:** Rounded (12-16px), subtle border, hover lift
- **Inputs:** Rounded (12px), warm border, focus ring in accent
- **Navigation:** Visual image-forward, minimal text
- **Modals:** Rounded (16px), backdrop blur, centered
- **Toasts:** Rounded (12px), bottom-center, auto-dismiss

## 3. App Architecture

### 3.1 Monorepo Structure

```
frontend/
├── packages/
│   ├── ui/              # Shared component library
│   ├── api-client/      # Typed API wrapper
│   ├── auth/            # Auth context + hooks
│   └── config/          # Shared TS/ESLint/Tailwind config
├── apps/
│   ├── landing/         # Marketing site (prerender)
│   ├── merchant/        # Merchant dashboard
│   ├── customer/        # Customer PWA
│   ├── admin/           # Admin panel
│   └── agent/           # Delivery agent PWA
├── turbo.json
├── package.json
└── docker/
    ├── nginx.conf
    └── Dockerfile
```

### 3.2 Routing

| App | Base path | Auth required |
|---|---|---|
| Landing | `/` | No |
| Merchant | `/merchant/*` | ROLE_MERCHANT |
| Customer | `/customer/*` | Partial (public browse, auth checkout) |
| Admin | `/admin/*` | ROLE_ADMIN |
| Agent | `/agent/*` | ROLE_DELIVERY_AGENT |

### 3.3 Auth Flow

1. Login → POST /auth/login → { accessToken, refreshToken }
2. Store in localStorage (httpOnly not possible for SPAs)
3. API client auto-attaches Bearer token
4. 401 → auto-refresh via /auth/refresh → retry original request
5. Refresh fails → redirect to login
6. Role guard: decode JWT → check roles → redirect to correct app if wrong role

## 4. Page Designs

### 4.1 Landing Page

- **Hero:** Full-bleed food/lifestyle image with overlay text
- **Features:** 3-column image cards (Catalog, Orders, Delivery)
- **Social proof:** Merchant testimonials with photos
- **CTA:** "Start Free" + "See Examples"
- **Footer:** Minimal, editorial layout

### 4.2 Customer Storefront

- **Browse:** Image grid (2-col mobile, 3-col tablet, 4-col desktop)
- **Product cards:** Large image + gradient overlay + name + price
- **Product detail:** Hero image, thumbnail strip, description, add-to-cart
- **Cart:** Image thumbnails, clean totals, pill checkout button
- **Order tracking:** Timeline with agent card, live status

### 4.3 Merchant Dashboard

- **Overview:** Revenue stats, order count, recent orders
- **Catalog:** Product grid with edit/delete, add product flow
- **Orders:** Order list with status filters, detail view
- **Coupons:** Create/manage coupons, usage stats
- **Profile:** Storefront settings, logo, description

### 4.4 Admin Panel

- **Merchants:** Onboard list, pending approvals
- **Audit trail:** Recent actions log
- **Metrics:** Platform-wide stats

### 4.5 Delivery Agent

- **Dashboard:** Clock in/out, availability toggle
- **Assignments:** Active delivery cards with map
- **OTP:** Pickup/delivery verification screens

## 5. Tech Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Framework | React (Vite) | SPA, no SSR needed, fast dev |
| Monorepo | Turborepo | Caching, parallel builds |
| Styling | Tailwind CSS | Utility-first, matches design system |
| Components | shadcn/ui | Copy-paste, Radix primitives, Tailwind |
| State | Zustand | Lightweight, no boilerplate |
| Data fetching | TanStack Query | Caching, background refetch, optimistic |
| Forms | React Hook Form + Zod | Validation, performance |
| Routing | React Router v6 | Standard, well-documented |
| PWA | vite-plugin-pwa | Service worker, manifest |
| Icons | Lucide React | Consistent, tree-shakeable |
| Animation | Framer Motion | Page transitions, micro-interactions |

## 6. Deployment

- Build all 5 apps via Turborepo
- Bundle into single Docker image with nginx
- nginx routes by path prefix to each app's dist/
- Deploy behind Kong gateway
- K8s manifests in `k8s/frontend/`

## 7. Testing

- Unit tests: Vitest + React Testing Library
- E2E tests: Playwright (critical flows only)
- Visual regression: Chromatic or Percy (optional)
- Lint: ESLint + Prettier
- Type checking: TypeScript strict mode
