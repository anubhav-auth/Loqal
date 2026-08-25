import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { motion, AnimatePresence } from 'framer-motion'
import { Plus, Pencil, Trash2, ImageOff } from 'lucide-react'
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
  Skeleton,
} from '@loqal/ui'
import { productsApi } from '@loqal/api-client'
import type { Product } from '@loqal/api-client'
import { formatPrice } from '@/lib/format'
import { MERCHANT_ID } from '@/lib/constants'

interface ProductForm {
  name: string
  description: string
  priceMinor: string
  quantity: string
  imageUrls: string
}

const EMPTY_FORM: ProductForm = {
  name: '',
  description: '',
  priceMinor: '',
  quantity: '',
  imageUrls: '',
}

export function Catalog() {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Product | null>(null)
  const [form, setForm] = useState<ProductForm>(EMPTY_FORM)

  const { data: products, isLoading } = useQuery({
    queryKey: ['products', MERCHANT_ID],
    queryFn: () => productsApi.getByMerchant(MERCHANT_ID),
  })

  const createMutation = useMutation({
    mutationFn: (data: Omit<Product, 'id' | 'merchantId'>) =>
      productsApi.create(MERCHANT_ID, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products', MERCHANT_ID] })
      closeForm()
    },
  })

  const updateMutation = useMutation({
    mutationFn: (data: { id: string; patch: Partial<Product> }) =>
      productsApi.update(data.id, MERCHANT_ID, data.patch),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products', MERCHANT_ID] })
      closeForm()
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => productsApi.delete(id, MERCHANT_ID),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['products', MERCHANT_ID] }),
  })

  function openCreate() {
    setEditing(null)
    setForm(EMPTY_FORM)
    setOpen(true)
  }

  function openEdit(p: Product) {
    setEditing(p)
    setForm({
      name: p.name,
      description: p.description ?? '',
      priceMinor: String(p.priceMinor),
      quantity: String(p.quantity),
      imageUrls: p.imageUrls.join(', '),
    })
    setOpen(true)
  }

  function closeForm() {
    setOpen(false)
    setEditing(null)
    setForm(EMPTY_FORM)
  }

  function submit() {
    const payload = {
      name: form.name,
      description: form.description || undefined,
      priceMinor: Number(form.priceMinor) || 0,
      quantity: Number(form.quantity) || 0,
      imageUrls: form.imageUrls
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean),
    }
    if (editing) {
      updateMutation.mutate({ id: editing.id, patch: payload })
    } else {
      createMutation.mutate(payload)
    }
  }

  return (
    <div className="space-y-8">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-4xl font-semibold text-foreground">
            Catalog
          </h1>
          <p className="mt-1 font-body text-foreground-secondary">
            Manage the products available in your storefront.
          </p>
        </div>

        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button onClick={openCreate}>
              <Plus className="h-4 w-4" /> Add Product
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>
                {editing ? 'Edit Product' : 'Add Product'}
              </DialogTitle>
              <DialogDescription>
                Fill in the details below to{' '}
                {editing ? 'update' : 'create'} a product.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4">
              <Field label="Name">
                <Input
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  placeholder="Handwoven Basket"
                />
              </Field>
              <Field label="Description">
                <Input
                  value={form.description}
                  onChange={(e) =>
                    setForm({ ...form, description: e.target.value })
                  }
                  placeholder="Short product description"
                />
              </Field>
              <div className="grid grid-cols-2 gap-4">
                <Field label="Price (₹ minor units)">
                  <Input
                    type="number"
                    value={form.priceMinor}
                    onChange={(e) =>
                      setForm({ ...form, priceMinor: e.target.value })
                    }
                    placeholder="12000"
                  />
                </Field>
                <Field label="Quantity">
                  <Input
                    type="number"
                    value={form.quantity}
                    onChange={(e) =>
                      setForm({ ...form, quantity: e.target.value })
                    }
                    placeholder="25"
                  />
                </Field>
              </div>
              <Field label="Image URLs (comma separated)">
                <Input
                  value={form.imageUrls}
                  onChange={(e) =>
                    setForm({ ...form, imageUrls: e.target.value })
                  }
                  placeholder="https://.../a.jpg, https://.../b.jpg"
                />
              </Field>
            </div>

            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end sm:space-x-2">
              <Button variant="outline" onClick={closeForm}>
                Cancel
              </Button>
              <Button onClick={submit}>
                {editing ? 'Save Changes' : 'Create'}
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      </header>

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-72 w-full rounded-card" />
          ))}
        </div>
      ) : products && products.length > 0 ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <AnimatePresence>
            {products.map((p) => (
              <motion.div
                key={p.id}
                layout
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
              >
                <Card className="overflow-hidden shadow-sm">
                  <div className="relative h-40 w-full bg-accent-light">
                    {p.imageUrls[0] ? (
                      <img
                        src={p.imageUrls[0]}
                        alt={p.name}
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <div className="flex h-full w-full items-center justify-center text-accent">
                        <ImageOff className="h-8 w-8" />
                      </div>
                    )}
                  </div>
                  <CardContent className="space-y-2 p-4">
                    <div className="flex items-start justify-between gap-2">
                      <h3 className="font-display text-xl font-semibold text-foreground">
                        {p.name}
                      </h3>
                      <Badge
                        variant={p.quantity > 0 ? 'accent' : 'error'}
                      >
                        {p.quantity > 0 ? `${p.quantity} left` : 'Out'}
                      </Badge>
                    </div>
                    <p className="font-body text-sm text-foreground-secondary">
                      {p.description}
                    </p>
                    <p className="font-display text-2xl font-semibold text-accent">
                      {formatPrice(p.priceMinor)}
                    </p>
                    <div className="flex gap-2 pt-2">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => openEdit(p)}
                      >
                        <Pencil className="h-4 w-4" /> Edit
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => deleteMutation.mutate(p.id)}
                      >
                        <Trash2 className="h-4 w-4" /> Delete
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      ) : (
        <div className="rounded-card border border-dashed border-border p-12 text-center">
          <p className="font-body text-foreground-secondary">
            No products yet. Click “Add Product” to get started.
          </p>
        </div>
      )}
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
