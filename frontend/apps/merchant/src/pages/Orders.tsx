import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Eye } from 'lucide-react'
import {
  Card,
  CardContent,
  Button,
  Badge,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  useToast,
} from '@loqal/ui'
import { ordersApi } from '@loqal/api-client'
import type { Order } from '@loqal/api-client'
import { formatPrice } from '@/lib/format'
import { MOCK_ORDERS } from '@/lib/mockOrders'

const FILTERS = [
  'ALL',
  'PENDING',
  'CONFIRMED',
  'SHIPPED',
  'DELIVERED',
  'CANCELLED',
] as const

type Filter = (typeof FILTERS)[number]

function statusVariant(
  status: string
): 'success' | 'accent' | 'secondary' | 'error' | 'default' {
  switch (status) {
    case 'DELIVERED':
    case 'RETURNED':
      return 'success'
    case 'SHIPPED':
      return 'accent'
    case 'CONFIRMED':
      return 'secondary'
    case 'CANCELLED':
      return 'error'
    default:
      return 'default'
  }
}

export function Orders() {
  const { toast } = useToast()
  const [orders, setOrders] = useState<Order[]>(MOCK_ORDERS)
  const [filter, setFilter] = useState<Filter>('ALL')
  const [selected, setSelected] = useState<Order | null>(null)

  const visible =
    filter === 'ALL'
      ? orders
      : orders.filter((o) => o.currentStatus === filter)

  async function handleCancel(o: Order) {
    try {
      await ordersApi.cancel(o.id)
      setOrders((prev) =>
        prev.map((x) =>
          x.id === o.id ? { ...x, currentStatus: 'CANCELLED' } : x
        )
      )
      setSelected((s) => (s?.id === o.id ? { ...s, currentStatus: 'CANCELLED' } : s))
      toast('Order cancelled', 'success')
    } catch {
      toast('Could not cancel order', 'error')
    }
  }

  async function handleReturn(o: Order) {
    try {
      await ordersApi.return(o.id)
      setOrders((prev) =>
        prev.map((x) =>
          x.id === o.id ? { ...x, currentStatus: 'RETURNED' } : x
        )
      )
      setSelected((s) => (s?.id === o.id ? { ...s, currentStatus: 'RETURNED' } : s))
      toast('Return requested', 'success')
    } catch {
      toast('Could not request return', 'error')
    }
  }

  return (
    <div className="space-y-8">
      <header>
        <h1 className="font-display text-4xl font-semibold text-foreground">
          Orders
        </h1>
        <p className="mt-1 font-body text-foreground-secondary">
          Track and manage every order placed with your store.
        </p>
      </header>

      <div className="flex flex-wrap gap-2">
        {FILTERS.map((f) => (
          <button
            key={f}
            type="button"
            onClick={() => setFilter(f)}
            className={
              f === filter
                ? 'rounded-button bg-foreground px-4 py-2 font-body text-sm text-white'
                : 'rounded-button border border-border bg-surface px-4 py-2 font-body text-sm text-foreground-secondary hover:bg-accent-light/60'
            }
          >
            {f === 'ALL' ? 'All' : f.charAt(0) + f.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      <div className="space-y-3">
        <AnimatePresence>
          {visible.map((o) => (
            <motion.div
              key={o.id}
              layout
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
            >
              <Card className="shadow-sm">
                <CardContent className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex items-center gap-4">
                    <div>
                      <p className="font-body text-sm font-medium text-foreground">
                        #{o.id.slice(4)}
                      </p>
                      <p className="font-body text-xs text-foreground-secondary">
                        {o.items.length} item(s) · {formatPrice(o.finalAmountMinor)}
                      </p>
                    </div>
                    <Badge variant={statusVariant(o.currentStatus)}>
                      {o.currentStatus}
                    </Badge>
                  </div>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setSelected(o)}
                  >
                    <Eye className="h-4 w-4" /> View
                  </Button>
                </CardContent>
              </Card>
            </motion.div>
          ))}
        </AnimatePresence>

        {visible.length === 0 && (
          <div className="rounded-card border border-dashed border-border p-12 text-center">
            <p className="font-body text-foreground-secondary">
              No orders match this filter.
            </p>
          </div>
        )}
      </div>

      <Dialog
        open={!!selected}
        onOpenChange={(o) => !o && setSelected(null)}
      >
        <DialogContent>
          {selected && (
            <>
              <DialogHeader>
                <DialogTitle>Order #{selected.id.slice(4)}</DialogTitle>
                <DialogDescription>
                  Placed by customer {selected.customerId}
                </DialogDescription>
              </DialogHeader>

              <div className="space-y-2">
                {selected.items.map((it, idx) => (
                  <div
                    key={idx}
                    className="flex items-center justify-between rounded-card border border-border px-4 py-3"
                  >
                    <span className="font-body text-sm text-foreground">
                      {it.quantity} × product {it.productId.slice(0, 6)}
                    </span>
                    <span className="font-body text-sm text-foreground-secondary">
                      {formatPrice(it.priceAtPurchaseMinor * it.quantity)}
                    </span>
                  </div>
                ))}
                <div className="space-y-1 pt-2 font-body text-sm">
                  <Row label="Subtotal" value={formatPrice(selected.totalAmountMinor)} />
                  <Row
                    label="Discount"
                    value={`- ${formatPrice(selected.discountAmountMinor)}`}
                  />
                  <Row
                    label="Final"
                    value={formatPrice(selected.finalAmountMinor)}
                    strong
                  />
                </div>
              </div>

              <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end sm:space-x-2">
                {selected.currentStatus === 'PENDING' ||
                selected.currentStatus === 'CONFIRMED' ? (
                  <Button variant="outline" onClick={() => handleCancel(selected)}>
                    Cancel Order
                  </Button>
                ) : null}
                {selected.currentStatus === 'DELIVERED' ? (
                  <Button variant="outline" onClick={() => handleReturn(selected)}>
                    Request Return
                  </Button>
                ) : null}
                <Button onClick={() => setSelected(null)}>Close</Button>
              </div>
            </>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}

function Row({
  label,
  value,
  strong,
}: {
  label: string
  value: string
  strong?: boolean
}) {
  return (
    <div className="flex justify-between">
      <span className={strong ? 'font-semibold text-foreground' : 'text-foreground-secondary'}>
        {label}
      </span>
      <span className={strong ? 'font-semibold text-foreground' : 'text-foreground'}>
        {value}
      </span>
    </div>
  )
}
