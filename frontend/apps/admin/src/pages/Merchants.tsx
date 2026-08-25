import { useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import { CheckCircle2, Search, Store } from 'lucide-react'
import { Card, CardContent, Badge, Button, Input, useToast } from '@loqal/ui'
import {
  MOCK_MERCHANTS,
  type Merchant,
  type MerchantStatus,
} from '@/lib/mockAdmin'

function statusVariant(status: MerchantStatus): 'accent' | 'success' {
  return status === 'active' ? 'success' : 'accent'
}

export function Merchants() {
  const { toast } = useToast()
  const [merchants, setMerchants] = useState<Merchant[]>(MOCK_MERCHANTS)
  const [query, setQuery] = useState('')

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return merchants
    return merchants.filter(
      (m) =>
        m.name.toLowerCase().includes(q) || m.email.toLowerCase().includes(q)
    )
  }, [merchants, query])

  function approve(id: string) {
    setMerchants((prev) =>
      prev.map((m) => (m.id === id ? { ...m, status: 'active' } : m))
    )
    const m = merchants.find((x) => x.id === id)
    toast(`${m?.name ?? 'Merchant'} approved`, 'success')
  }

  return (
    <div className="space-y-8">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="font-display text-4xl font-semibold text-foreground">
            Merchants
          </h1>
          <p className="mt-1 font-body text-foreground-secondary">
            Review onboarding requests and activate storefronts.
          </p>
        </div>
        <div className="relative w-full sm:w-72">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-foreground-secondary" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search name or email"
            className="pl-9"
          />
        </div>
      </header>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
        {filtered.map((m, i) => (
          <motion.div
            key={m.id}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.04 }}
          >
            <Card className="shadow-sm">
              <CardContent className="flex flex-col gap-4 p-5">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <span className="flex h-10 w-10 items-center justify-center rounded-button bg-accent-light text-accent">
                      <Store className="h-5 w-5" />
                    </span>
                    <div>
                      <p className="font-body text-sm font-medium text-foreground">
                        {m.name}
                      </p>
                      <p className="font-body text-xs text-foreground-secondary">
                        {m.email}
                      </p>
                    </div>
                  </div>
                  <Badge variant={statusVariant(m.status)}>{m.status}</Badge>
                </div>

                <div className="flex items-center justify-between border-t border-border pt-3">
                  <span className="font-body text-xs text-foreground-secondary">
                    Joined {new Date(m.joinedAt).toLocaleDateString()}
                  </span>
                  {m.status === 'pending' ? (
                    <Button size="sm" onClick={() => approve(m.id)}>
                      <CheckCircle2 className="h-4 w-4" />
                      Approve
                    </Button>
                  ) : (
                    <span className="font-body text-xs text-foreground-secondary">
                      Active storefront
                    </span>
                  )}
                </div>
              </CardContent>
            </Card>
          </motion.div>
        ))}
      </div>

      {filtered.length === 0 && (
        <p className="font-body text-center text-foreground-secondary">
          No merchants match “{query}”.
        </p>
      )}
    </div>
  )
}
