import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ordersApi } from '@loqal/api-client'
import { Button, Card, CardContent, Input } from '@loqal/ui'
import { CheckCircle2 } from 'lucide-react'
import { useCartStore } from '@/store/cartStore'
import { formatPrice } from '@/lib/format'
import { MERCHANT_ID } from '@/lib/constants'

export function Checkout() {
  const items = useCartStore((s) => s.items)
  const subtotalMinor = useCartStore((s) => s.subtotalMinor())
  const clear = useCartStore((s) => s.clear)
  const navigate = useNavigate()

  const [coupon, setCoupon] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [createdOrderId, setCreatedOrderId] = useState<string | null>(null)

  if (createdOrderId) {
    return (
      <div className="mx-auto max-w-md px-4 py-24 text-center">
        <CheckCircle2 className="mx-auto h-16 w-16 text-success" />
        <h1 className="mt-4 font-display text-4xl text-foreground">Order placed!</h1>
        <p className="mt-2 font-body text-foreground-secondary">
          Your order is confirmed and the merchant is preparing it.
        </p>
        <div className="mt-6 flex justify-center gap-3">
          <Button asChild>
            <Link to={`/orders/${createdOrderId}`}>Track your order</Link>
          </Button>
          <Button asChild variant="outline">
            <Link to="/">Continue shopping</Link>
          </Button>
        </div>
      </div>
    )
  }

  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-md px-4 py-24 text-center">
        <h1 className="font-display text-3xl text-foreground">Nothing to check out</h1>
        <Button asChild className="mt-6">
          <Link to="/">Browse the store</Link>
        </Button>
      </div>
    )
  }

  const handlePlaceOrder = async () => {
    setError(null)
    setLoading(true)
    try {
      const request = {
        items: items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
        merchantId: MERCHANT_ID,
        couponCode: coupon.trim() || undefined,
      }
      const res = await ordersApi.create(request, crypto.randomUUID())
      clear()
      setCreatedOrderId(res.orderId)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not place order')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <h1 className="mb-6 font-display text-4xl font-semibold text-foreground">Checkout</h1>

      <div className="grid gap-8 md:grid-cols-3">
        <div className="md:col-span-2">
          <Card>
            <CardContent className="p-6">
              <h2 className="font-display text-2xl text-foreground">Order items</h2>
              <ul className="mt-4 divide-y divide-border">
                {items.map((i) => (
                  <li key={i.productId} className="flex items-center gap-4 py-3">
                    <img src={i.imageUrl} alt="" className="h-14 w-14 rounded-card object-cover" />
                    <div className="flex-1">
                      <p className="font-body text-foreground">{i.name}</p>
                      <p className="font-body text-sm text-foreground-secondary">
                        Qty {i.quantity} × {formatPrice(i.priceMinor)}
                      </p>
                    </div>
                    <span className="font-display text-accent">
                      {formatPrice(i.priceMinor * i.quantity)}
                    </span>
                  </li>
                ))}
              </ul>
              <div className="mt-4">
                <label className="font-body text-sm text-foreground-secondary">Coupon code</label>
                <Input
                  value={coupon}
                  onChange={(e) => setCoupon(e.target.value)}
                  placeholder="Optional"
                  className="mt-1"
                />
              </div>
            </CardContent>
          </Card>
        </div>

        <Card className="h-fit">
          <CardContent className="p-6">
            <h2 className="font-display text-2xl text-foreground">Total</h2>
            <div className="mt-4 flex justify-between font-body font-semibold text-foreground">
              <span>Amount due</span>
              <span className="font-display text-xl text-accent">
                {formatPrice(subtotalMinor)}
              </span>
            </div>
            {error && <p className="mt-3 font-body text-sm text-error">{error}</p>}
            <Button className="mt-6 w-full" onClick={handlePlaceOrder} disabled={loading}>
              {loading ? 'Placing order…' : 'Place Order'}
            </Button>
            <Button
              variant="ghost"
              className="mt-2 w-full"
              onClick={() => navigate('/cart')}
            >
              Back to cart
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
