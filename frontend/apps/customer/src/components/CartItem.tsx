import { Minus, Plus, Trash2 } from 'lucide-react'
import { Button } from '@loqal/ui'
import type { CartItem as CartItemType } from '@/store/cartStore'
import { formatPrice } from '@/lib/format'
import { useCartStore } from '@/store/cartStore'

interface CartItemRowProps {
  item: CartItemType
}

export function CartItem({ item }: CartItemRowProps) {
  const updateQty = useCartStore((s) => s.updateQty)
  const removeItem = useCartStore((s) => s.removeItem)

  return (
    <div className="flex gap-4 border-b border-border py-4">
      <img
        src={item.imageUrl}
        alt={item.name}
        className="h-20 w-20 shrink-0 rounded-card object-cover"
      />

      <div className="flex flex-1 flex-col">
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-display text-lg font-semibold text-foreground">
            {item.name}
          </h3>
          <span className="font-display text-lg text-accent">
            {formatPrice(item.priceMinor * item.quantity)}
          </span>
        </div>

        <div className="mt-auto flex items-center justify-between pt-2">
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="icon"
              className="h-8 w-8"
              aria-label="Decrease quantity"
              onClick={() => updateQty(item.productId, item.quantity - 1)}
              disabled={item.quantity <= 1}
            >
              <Minus className="h-4 w-4" />
            </Button>
            <span className="w-8 text-center font-body text-sm">{item.quantity}</span>
            <Button
              variant="outline"
              size="icon"
              className="h-8 w-8"
              aria-label="Increase quantity"
              onClick={() => updateQty(item.productId, item.quantity + 1)}
            >
              <Plus className="h-4 w-4" />
            </Button>
          </div>

          <Button
            variant="ghost"
            size="sm"
            className="text-error hover:bg-error/10"
            onClick={() => removeItem(item.productId)}
          >
            <Trash2 className="h-4 w-4" /> Remove
          </Button>
        </div>
      </div>
    </div>
  )
}
