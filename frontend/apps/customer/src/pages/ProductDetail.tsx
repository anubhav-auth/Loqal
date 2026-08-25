import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { productsApi } from '@loqal/api-client'
import { Button, Skeleton } from '@loqal/ui'
import { Minus, Plus } from 'lucide-react'
import { formatPrice } from '@/lib/format'
import { useCartStore } from '@/store/cartStore'

export function ProductDetail() {
  const { id } = useParams<{ id: string }>()
  const [qty, setQty] = useState(1)
  const [activeImage, setActiveImage] = useState(0)
  const addItem = useCartStore((s) => s.addItem)

  const { data: product, isLoading, isError } = useQuery({
    queryKey: ['product', id],
    queryFn: () => productsApi.getById(id!),
    enabled: !!id,
  })

  if (isLoading) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-10">
        <Skeleton className="aspect-[16/9] w-full rounded-card" />
      </div>
    )
  }

  if (isError || !product) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-20 text-center">
        <p className="font-body text-error">Product not found.</p>
        <Button asChild variant="link">
          <Link to="/">Back to store</Link>
        </Button>
      </div>
    )
  }

  const images = product.imageUrls.length > 0 ? product.imageUrls : []

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
      <Link to="/" className="font-body text-sm text-foreground-secondary hover:text-accent">
        ← Back to store
      </Link>

      <div className="mt-6 grid gap-8 md:grid-cols-2">
        <div>
          <img
            src={images[activeImage] ?? 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800'}
            alt={product.name}
            className="aspect-[4/3] w-full rounded-card object-cover"
          />
          {images.length > 1 && (
            <div className="mt-4 flex gap-3">
              {images.map((img, i) => (
                <button
                  key={i}
                  onClick={() => setActiveImage(i)}
                  className={`h-16 w-16 overflow-hidden rounded-card border-2 ${
                    i === activeImage ? 'border-accent' : 'border-border'
                  }`}
                >
                  <img src={img} alt="" className="h-full w-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="flex flex-col">
          <h1 className="font-display text-4xl font-semibold text-foreground">
            {product.name}
          </h1>
          <p className="mt-2 font-display text-2xl text-accent">
            {formatPrice(product.priceMinor)}
          </p>
          {product.description && (
            <p className="mt-4 font-body leading-relaxed text-foreground-secondary">
              {product.description}
            </p>
          )}

          <div className="mt-8 flex items-center gap-4">
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="icon"
                onClick={() => setQty(Math.max(1, qty - 1))}
                aria-label="Decrease"
              >
                <Minus className="h-4 w-4" />
              </Button>
              <span className="w-8 text-center font-body">{qty}</span>
              <Button
                variant="outline"
                size="icon"
                onClick={() => setQty(qty + 1)}
                aria-label="Increase"
              >
                <Plus className="h-4 w-4" />
              </Button>
            </div>
            <Button
              className="flex-1"
              onClick={() =>
                addItem(
                  {
                    productId: product.id,
                    name: product.name,
                    priceMinor: product.priceMinor,
                    imageUrl: images[0] ?? 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=600',
                  },
                  qty
                )
              }
            >
              Add to cart
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
