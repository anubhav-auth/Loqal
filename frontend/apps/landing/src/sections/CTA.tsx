import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Button } from '@loqal/ui'

export default function CTA() {
  return (
    <section className="py-24">
      <div className="mx-auto max-w-6xl px-4 sm:px-6">
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-80px' }}
          transition={{ duration: 0.6 }}
          className="rounded-card bg-accent-light px-6 py-16 text-center sm:px-12"
        >
          <h2 className="mx-auto max-w-2xl font-display text-4xl font-semibold text-foreground sm:text-5xl">
            Open your storefront today
          </h2>
          <p className="mx-auto mt-5 max-w-xl text-lg text-foreground-secondary">
            Join the local merchants building loyal communities with Loqal. Free
            to start — no credit card required.
          </p>
          <div className="mt-8 flex justify-center">
            <Button asChild size="lg">
              <Link to="/signup">Get Started</Link>
            </Button>
          </div>
        </motion.div>
      </div>
    </section>
  )
}
