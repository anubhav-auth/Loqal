import { useState } from 'react'
import { motion } from 'framer-motion'
import { Store, Save } from 'lucide-react'
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Button,
  Input,
  Avatar,
  AvatarImage,
  AvatarFallback,
  useToast,
} from '@loqal/ui'

interface ProfileForm {
  storeName: string
  description: string
  logoUrl: string
  street: string
  city: string
  state: string
  postalCode: string
  country: string
}

const INITIAL: ProfileForm = {
  storeName: 'Sunrise Handicrafts',
  description: 'Locally made crafts delivered to your doorstep.',
  logoUrl: '',
  street: '12 Market Road',
  city: 'Jaipur',
  state: 'Rajasthan',
  postalCode: '302001',
  country: 'India',
}

export function Profile() {
  const { toast } = useToast()
  const [form, setForm] = useState<ProfileForm>(INITIAL)

  function set<K extends keyof ProfileForm>(key: K, value: ProfileForm[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  function save() {
    toast('Storefront settings saved', 'success')
  }

  return (
    <div className="space-y-8">
      <header>
        <h1 className="font-display text-4xl font-semibold text-foreground">
          Profile
        </h1>
        <p className="mt-1 font-body text-foreground-secondary">
          Customize how your storefront appears to customers.
        </p>
      </header>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <Card className="shadow-sm">
          <CardHeader>
            <div className="flex items-center gap-4">
              <Avatar className="h-16 w-16">
                {form.logoUrl ? (
                  <AvatarImage src={form.logoUrl} alt={form.storeName} />
                ) : null}
                <AvatarFallback>
                  <Store className="h-7 w-7" />
                </AvatarFallback>
              </Avatar>
              <CardTitle className="font-display text-2xl">
                {form.storeName || 'Your Store'}
              </CardTitle>
            </div>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Store name">
                <Input
                  value={form.storeName}
                  onChange={(e) => set('storeName', e.target.value)}
                />
              </Field>
              <Field label="Logo URL">
                <Input
                  value={form.logoUrl}
                  onChange={(e) => set('logoUrl', e.target.value)}
                  placeholder="https://.../logo.png"
                />
              </Field>
            </div>
            <Field label="Description">
              <Input
                value={form.description}
                onChange={(e) => set('description', e.target.value)}
              />
            </Field>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Street">
                <Input
                  value={form.street}
                  onChange={(e) => set('street', e.target.value)}
                />
              </Field>
              <Field label="City">
                <Input
                  value={form.city}
                  onChange={(e) => set('city', e.target.value)}
                />
              </Field>
              <Field label="State">
                <Input
                  value={form.state}
                  onChange={(e) => set('state', e.target.value)}
                />
              </Field>
              <Field label="Postal code">
                <Input
                  value={form.postalCode}
                  onChange={(e) => set('postalCode', e.target.value)}
                />
              </Field>
              <Field label="Country">
                <Input
                  value={form.country}
                  onChange={(e) => set('country', e.target.value)}
                />
              </Field>
            </div>

            <div className="flex justify-end">
              <Button onClick={save}>
                <Save className="h-4 w-4" /> Save Changes
              </Button>
            </div>
          </CardContent>
        </Card>
      </motion.div>
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
