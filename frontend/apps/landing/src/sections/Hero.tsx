import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ArrowRight, Sparkles } from 'lucide-react'
import { Button } from '@loqal/ui'

const fadeUp = {
  hidden: { opacity: 0, y: 24 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.6, ease: 'easeOut' } },
}

export default function Hero() {
  return (
    <section className="mx-auto grid max-w-6xl items-center gap-12 px-4 pb-20 pt-28 sm:px-6 md:grid-cols-2 md:pt-36">
      <motion.div initial="hidden" animate="visible" variants={fadeUp}>
        <span className="inline-flex items-center gap-2 rounded-button bg-accent-light px-4 py-1.5 text-sm font-medium text-accent">
          <Sparkles size={16} />
          Commerce Reimagined
        </span>
        <h1 className="mt-6 font-display text-5xl font-semibold leading-[1.05] text-foreground sm:text-6xl lg:text-7xl">
          Local commerce, <span className="italic text-accent">beautifully</span> simple.
        </h1>
        <p className="mt-6 max-w-md text-lg leading-relaxed text-foreground-secondary">
          Loqal gives neighborhood merchants a polished storefront, effortless
          ordering, and reliable delivery — without the enterprise price tag.
        </p>
        <div className="mt-8 flex flex-col gap-3 sm:flex-row">
          <Button asChild size="lg">
            <Link to="/signup">
              Start Free
              <ArrowRight size={18} />
            </Link>
          </Button>
          <Button asChild size="lg" variant="outline">
            <a href="#features">See Examples</a>
          </Button>
        </div>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.7, ease: 'easeOut' }}
        className="relative"
      >
        <img
          src="https://images.unsplash.com/photo-1488459716781-31f93360f497?w=800&q=80"
          alt="Fresh local produce arranged on a market table"
          className="aspect-[4/5] w-full rounded-card object-cover shadow-lg"
        />
        <div className="absolute -bottom-6 -left-4 hidden rounded-card bg-surface p-4 shadow-md sm:block">
          <p className="font-display text-2xl font-semibold text-foreground">+38%</p>
          <p className="text-xs text-foreground-secondary">avg. repeat orders</p>
        </div>
      </motion.div>
    </section>
  )
}
