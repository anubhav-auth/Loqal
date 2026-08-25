import { useQuery } from '@tanstack/react-query'
import { productsApi } from '@loqal/api-client'
import { Skeleton } from '@loqal/ui'
import { ProductCard } from '@/components/ProductCard'
import { MERCHANT_ID } from '@/lib/constants'

export function Storefront() {
  const { data: products, isLoading, isError } = useQuery({
    queryKey: ['products', MERCHANT_ID],
    queryFn: () => productsApi.getByMerchant(MERCHANT_ID),
  })

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <header className="mb-10 text-center">
        <p className="font-body text-sm uppercase tracking-widest text-accent">
          Local · Fresh · Delivered
        </p>
        <h1 className="mt-2 font-display text-5xl font-semibold text-foreground">
          The Neighbourhood Store
        </h1>
        <p className="mx-auto mt-3 max-w-xl font-body text-foreground-secondary">
          Handpicked everyday essentials from merchants around you — quality you can
          taste, prices you'll love.
        </p>
      </header>

      {isLoading && (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="aspect-[4/3] w-full rounded-card" />
          ))}
        </div>
      )}

      {isError && (
        <p className="text-center font-body text-error">
          Could not load products. Please try again later.
        </p>
      )}

      {products && products.length === 0 && (
        <p className="text-center font-body text-foreground-secondary">
          No products available right now.
        </p>
      )}

      {products && products.length > 0 && (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}
    </div>
  )
}
