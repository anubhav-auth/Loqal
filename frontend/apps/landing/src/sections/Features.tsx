import { motion } from 'framer-motion'
import { Card, CardContent } from '@loqal/ui'

const features = [
  {
    title: 'Catalog',
    description:
      'Curate a gorgeous storefront with rich imagery, variants, and instant search — no design skills required.',
    image:
      'https://images.unsplash.com/photo-1542838132-92c53300491e?w=800&q=80',
  },
  {
    title: 'Orders',
    description:
      'Capture, track, and fulfill orders in real time across pickup, delivery, and in-store — all in one place.',
    image:
      'https://images.unsplash.com/photo-1556740738-b6a63e27c4df?w=800&q=80',
  },
  {
    title: 'Delivery',
    description:
      'Route local deliveries with live status and notifications that keep your customers in the loop.',
    image:
      'https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=800&q=80',
  },
]

export default function Features() {
  return (
    <section id="features" className="bg-surface/60 py-24">
      <div className="mx-auto max-w-6xl px-4 sm:px-6">
        <div className="mx-auto max-w-2xl text-center">
          <p className="text-sm font-medium uppercase tracking-widest text-accent">
            Everything you need
          </p>
          <h2 className="mt-3 font-display text-4xl font-semibold text-foreground sm:text-5xl">
            One platform for the whole shop
          </h2>
        </div>

        <div className="mt-14 grid gap-8 md:grid-cols-3">
          {features.map((feature, i) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 24 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-80px' }}
              transition={{ duration: 0.5, delay: i * 0.1 }}
            >
              <Card className="group h-full overflow-hidden transition-shadow duration-300 hover:shadow-hover">
                <div className="overflow-hidden">
                  <img
                    src={feature.image}
                    alt={feature.title}
                    className="h-48 w-full object-cover transition-transform duration-500 group-hover:scale-105"
                  />
                </div>
                <CardContent className="p-6">
                  <h3 className="font-display text-2xl font-semibold text-foreground">
                    {feature.title}
                  </h3>
                  <p className="mt-3 text-foreground-secondary">{feature.description}</p>
                </CardContent>
              </Card>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}
