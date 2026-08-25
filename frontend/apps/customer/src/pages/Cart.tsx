import { Link } from 'react-router-dom'
import { Button, Card, CardContent } from '@loqal/ui'
import { ShoppingBag } from 'lucide-react'
import { CartItem } from '@/components/CartItem'
import { useCartStore } from '@/store/cartStore'
import { formatPrice } from '@/lib/format'

export function Cart() {
  const items = useCartStore((s) => s.items)
  const subtotalMinor = useCartStore((s) => s.subtotalMinor())
  const itemCount = useCartStore((s) => s.itemCount())

  if (items.length === 0) {
    return (
      <div className="mx-auto flex max-w-md flex-col items-center justify-center px-4 py-24 text-center">
        <ShoppingBag className="h-12 w-12 text-foreground-secondary" />
        <h1 className="mt-4 font-display text-3xl text-foreground">Your cart is empty</h1>
        <Button asChild className="mt-6">
          <Link to="/">Browse the store</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <h1 className="mb-6 font-display text-4xl font-semibold text-foreground">Your Cart</h1>

      <div className="grid gap-8 md:grid-cols-3">
        <div className="md:col-span-2">
          {items.map((item) => (
            <CartItem key={item.productId} item={item} />
          ))}
        </div>

        <Card className="h-fit">
          <CardContent className="p-6">
            <h2 className="font-display text-2xl text-foreground">Summary</h2>
            <div className="mt-4 flex justify-between font-body text-foreground-secondary">
              <span>Items ({itemCount})</span>
              <span>{formatPrice(subtotalMinor)}</span>
            </div>
            <div className="mt-2 flex justify-between font-body font-semibold text-foreground">
              <span>Total</span>
              <span className="font-display text-xl text-accent">
                {formatPrice(subtotalMinor)}
              </span>
            </div>
            <Button asChild className="mt-6 w-full">
              <Link to="/checkout">Proceed to checkout</Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
