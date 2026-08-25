import { motion } from 'framer-motion'
import { LayoutGrid, ShoppingBag, Truck } from 'lucide-react'
import { Card, CardContent } from '@loqal/ui'

const steps = [
  {
    icon: LayoutGrid,
    title: 'Set up your store',
    description:
      'Import your catalog, add photos, and publish a storefront in minutes — no code, no designers.',
  },
  {
    icon: ShoppingBag,
    title: 'Take orders',
    description:
      'Customers browse and buy through a clean, mobile-first experience with fast, local checkout.',
  },
  {
    icon: Truck,
    title: 'Deliver & grow',
    description:
      'Fulfill with integrated delivery and keep shoppers coming back with built-in rewards.',
  },
]

export default function HowItWorks() {
  return (
    <section id="how-it-works" className="py-24">
      <div className="mx-auto max-w-6xl px-4 sm:px-6">
        <div className="mx-auto max-w-2xl text-center">
          <p className="text-sm font-medium uppercase tracking-widest text-accent">
            How it works
          </p>
          <h2 className="mt-3 font-display text-4xl font-semibold text-foreground sm:text-5xl">
            Live in three simple steps
          </h2>
        </div>

        <div className="mt-14 grid gap-8 md:grid-cols-3">
          {steps.map((step, i) => (
            <motion.div
              key={step.title}
              initial={{ opacity: 0, y: 24 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-80px' }}
              transition={{ duration: 0.5, delay: i * 0.1 }}
            >
              <Card className="h-full bg-accent-light/40">
                <CardContent className="p-8">
                  <div className="flex items-center gap-4">
                    <span className="flex h-12 w-12 items-center justify-center rounded-button bg-surface text-accent shadow-sm">
                      <step.icon size={22} />
                    </span>
                    <span className="font-display text-3xl font-semibold text-foreground/30">
                      0{i + 1}
                    </span>
                  </div>
                  <h3 className="mt-6 font-display text-2xl font-semibold text-foreground">
                    {step.title}
                  </h3>
                  <p className="mt-3 text-foreground-secondary">{step.description}</p>
                </CardContent>
              </Card>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}
