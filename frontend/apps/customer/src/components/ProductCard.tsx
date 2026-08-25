import { Link } from 'react-router-dom'
import { Card, CardContent } from '@loqal/ui'
import { Plus } from 'lucide-react'
import { Button } from '@loqal/ui'
import type { Product } from '@loqal/api-client'
import { formatPrice } from '@/lib/format'
import { useCartStore } from '@/store/cartStore'

interface ProductCardProps {
  product: Product
}

export function ProductCard({ product }: ProductCardProps) {
  const addItem = useCartStore((s) => s.addItem)
  const image = product.imageUrls?.[0] ?? 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=600'

  return (
    <Card className="group flex flex-col overflow-hidden transition-shadow hover:shadow-hover">
      <Link to={`/product/${product.id}`} className="block overflow-hidden">
        <img
          src={image}
          alt={product.name}
          loading="lazy"
          className="aspect-[4/3] w-full object-cover transition-transform duration-500 group-hover:scale-105"
        />
      </Link>
      <CardContent className="flex flex-1 flex-col gap-3 p-4">
        <div className="flex items-start justify-between gap-2">
          <Link to={`/product/${product.id}`}>
            <h3 className="font-display text-xl font-semibold leading-snug text-foreground">
              {product.name}
            </h3>
          </Link>
          <span className="shrink-0 font-display text-lg text-accent">
            {formatPrice(product.priceMinor)}
          </span>
        </div>

        {product.description && (
          <p className="line-clamp-2 font-body text-sm text-foreground-secondary">
            {product.description}
          </p>
        )}

        <Button
          size="sm"
          className="mt-auto w-full"
          onClick={() =>
            addItem({
              productId: product.id,
              name: product.name,
              priceMinor: product.priceMinor,
              imageUrl: image,
            })
          }
        >
          <Plus className="h-4 w-4" /> Add to cart
        </Button>
      </CardContent>
    </Card>
  )
}
