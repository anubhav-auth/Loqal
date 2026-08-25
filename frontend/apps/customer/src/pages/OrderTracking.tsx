import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ordersApi } from '@loqal/api-client'
import { Button, Card, CardContent, Skeleton } from '@loqal/ui'
import { Package, CookingPot, Bike, CheckCircle2 } from 'lucide-react'
import { formatPrice } from '@/lib/format'

const STATUS_FLOW = ['PLACED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED']

const STATUS_META: Record<string, { label: string; icon: typeof Package }> = {
  PLACED: { label: 'Order placed', icon: Package },
  PREPARING: { label: 'Being prepared', icon: CookingPot },
  OUT_FOR_DELIVERY: { label: 'Out for delivery', icon: Bike },
  DELIVERED: { label: 'Delivered', icon: CheckCircle2 },
}

export function OrderTracking() {
  const { id } = useParams<{ id: string }>()

  const { data: order, isLoading, isError } = useQuery({
    queryKey: ['order', id],
    queryFn: () => ordersApi.getById(id!),
    enabled: !!id,
  })

  if (isLoading) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <Skeleton className="h-64 w-full rounded-card" />
      </div>
    )
  }

  if (isError || !order) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-20 text-center">
        <p className="font-body text-error">Order not found.</p>
        <Button asChild variant="link">
          <Link to="/">Back to store</Link>
        </Button>
      </div>
    )
  }

  const currentIndex = Math.max(0, STATUS_FLOW.indexOf(order.currentStatus))

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <Link to="/" className="font-body text-sm text-foreground-secondary hover:text-accent">
        ← Back to store
      </Link>
      <h1 className="mt-4 font-display text-4xl font-semibold text-foreground">
        Order #{order.id.slice(0, 8)}
      </h1>

      <Card className="mt-6">
        <CardContent className="p-6">
          <ol className="relative space-y-6 border-l border-border pl-6">
            {STATUS_FLOW.map((status, i) => {
              const meta = STATUS_META[status]
              const Icon = meta.icon
              const done = i <= currentIndex
              return (
                <li key={status} className="flex items-center gap-3">
                  <span
                    className={`absolute -left-[15px] flex h-7 w-7 items-center justify-center rounded-full ${
                      done ? 'bg-accent text-white' : 'bg-accent-light text-accent'
                    }`}
                  >
                    <Icon className="h-4 w-4" />
                  </span>
                  <span
                    className={`font-body ${
                      done ? 'text-foreground' : 'text-foreground-secondary'
                    }`}
                  >
                    {meta.label}
                  </span>
                </li>
              )
            })}
          </ol>
        </CardContent>
      </Card>

      <Card className="mt-6">
        <CardContent className="p-6">
          <h2 className="font-display text-2xl text-foreground">Items</h2>
          <ul className="mt-3 divide-y divide-border">
            {order.items.map((item) => (
              <li key={item.productId} className="flex justify-between py-2 font-body">
                <span className="text-foreground">
                  {item.quantity} × {item.productId.slice(0, 8)}
                </span>
                <span className="text-foreground-secondary">
                  {formatPrice(item.priceAtPurchaseMinor * item.quantity)}
                </span>
              </li>
            ))}
          </ul>
          <div className="mt-4 flex justify-between border-t border-border pt-4 font-body font-semibold">
            <span>Total paid</span>
            <span className="font-display text-xl text-accent">
              {formatPrice(order.finalAmountMinor)}
            </span>
          </div>
        </CardContent>
      </Card>

      <Card className="mt-6">
        <CardContent className="p-6">
          <h2 className="font-display text-2xl text-foreground">Delivery partner</h2>
          <div className="mt-3 flex items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-accent-light font-display text-lg text-accent">
              DA
            </div>
            <div>
              <p className="font-body text-foreground">Agent assigned</p>
              <p className="font-body text-sm text-foreground-secondary">
                Live tracking will appear here once the agent picks up your order.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
