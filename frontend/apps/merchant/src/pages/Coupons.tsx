import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Plus } from 'lucide-react'
import {
  Card,
  CardContent,
  Button,
  Input,
  Badge,
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@loqal/ui'
import type { Coupon } from '@loqal/api-client'
import { formatPrice } from '@/lib/format'

const SEED_COUPONS: Coupon[] = [
  {
    id: 'cpn_1',
    code: 'WELCOME10',
    discountType: 'PERCENT',
    value: 10,
    validFrom: '2026-01-01',
    validUntil: '2026-12-31',
    active: true,
  },
  {
    id: 'cpn_2',
    code: 'FLAT50',
    discountType: 'FLAT',
    value: 5000,
    minOrderValueMinor: 20000,
    validFrom: '2026-01-01',
    validUntil: '2026-06-30',
    active: true,
  },
  {
    id: 'cpn_3',
    code: 'EXPIRED20',
    discountType: 'PERCENT',
    value: 20,
    validFrom: '2025-01-01',
    validUntil: '2025-12-31',
    active: false,
  },
]

interface CouponForm {
  code: string
  discountType: string
  value: string
  validFrom: string
  validUntil: string
}

const EMPTY: CouponForm = {
  code: '',
  discountType: 'PERCENT',
  value: '',
  validFrom: '',
  validUntil: '',
}

function describe(c: Coupon): string {
  return c.discountType === 'PERCENT'
    ? `${c.value}% off`
    : `${formatPrice(c.value)} off`
}

export function Coupons() {
  const [coupons, setCoupons] = useState<Coupon[]>(SEED_COUPONS)
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState<CouponForm>(EMPTY)

  function submit() {
    const created: Coupon = {
      id: `cpn_${Date.now()}`,
      code: form.code.toUpperCase(),
      discountType: form.discountType.toUpperCase(),
      value: Number(form.value) || 0,
      validFrom: form.validFrom,
      validUntil: form.validUntil,
      active: true,
    }
    setCoupons((prev) => [created, ...prev])
    setOpen(false)
    setForm(EMPTY)
  }

  return (
    <div className="space-y-8">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-4xl font-semibold text-foreground">
            Coupons
          </h1>
          <p className="mt-1 font-body text-foreground-secondary">
            Create and manage discount codes for your customers.
          </p>
        </div>

        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="h-4 w-4" /> Create Coupon
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Create Coupon</DialogTitle>
              <DialogDescription>
                Define a new discount code for your storefront.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4">
              <Field label="Code">
                <Input
                  value={form.code}
                  onChange={(e) => setForm({ ...form, code: e.target.value })}
                  placeholder="SUMMER25"
                />
              </Field>
              <div className="grid grid-cols-2 gap-4">
                <Field label="Discount type (PERCENT/FLAT)">
                  <Input
                    value={form.discountType}
                    onChange={(e) =>
                      setForm({ ...form, discountType: e.target.value })
                    }
                    placeholder="PERCENT"
                  />
                </Field>
                <Field label="Value">
                  <Input
                    type="number"
                    value={form.value}
                    onChange={(e) => setForm({ ...form, value: e.target.value })}
                    placeholder="10"
                  />
                </Field>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <Field label="Valid from">
                  <Input
                    type="date"
                    value={form.validFrom}
                    onChange={(e) =>
                      setForm({ ...form, validFrom: e.target.value })
                    }
                  />
                </Field>
                <Field label="Valid until">
                  <Input
                    type="date"
                    value={form.validUntil}
                    onChange={(e) =>
                      setForm({ ...form, validUntil: e.target.value })
                    }
                  />
                </Field>
              </div>
            </div>

            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end sm:space-x-2">
              <Button variant="outline" onClick={() => setOpen(false)}>
                Cancel
              </Button>
              <Button onClick={submit}>Create</Button>
            </div>
          </DialogContent>
        </Dialog>
      </header>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <AnimatePresence>
          {coupons.map((c) => (
            <motion.div
              key={c.id}
              layout
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
            >
              <Card className="shadow-sm">
                <CardContent className="space-y-3 p-5">
                  <div className="flex items-center justify-between">
                    <span className="font-display text-2xl font-semibold text-foreground">
                      {c.code}
                    </span>
                    <Badge variant={c.active ? 'success' : 'error'}>
                      {c.active ? 'Active' : 'Inactive'}
                    </Badge>
                  </div>
                  <p className="font-body text-sm text-accent">{describe(c)}</p>
                  <p className="font-body text-xs text-foreground-secondary">
                    {c.validFrom} → {c.validUntil}
                  </p>
                </CardContent>
              </Card>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </div>
  )
}

function Field({
  label,
  children,
}: {
  label: string
  children: React.ReactNode
}) {
  return (
    <label className="block space-y-1">
      <span className="font-body text-sm text-foreground-secondary">
        {label}
      </span>
      {children}
    </label>
  )
}
