import { motion } from 'framer-motion'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from 'recharts'
import { Users, ShoppingBag, IndianRupee, Bot } from 'lucide-react'
import { Card, CardHeader, CardTitle, CardContent } from '@loqal/ui'
import { MOCK_METRICS, MOCK_ORDERS_PER_DAY } from '@/lib/mockAdmin'

function formatPrice(priceMinor: number): string {
  return `₹${(priceMinor / 100).toLocaleString('en-IN', {
    maximumFractionDigits: 0,
  })}`
}

const STATS = [
  {
    label: 'Total Merchants',
    value: MOCK_METRICS.totalMerchants.toLocaleString(),
    icon: Users,
  },
  {
    label: 'Total Orders',
    value: MOCK_METRICS.totalOrders.toLocaleString(),
    icon: ShoppingBag,
  },
  { label: 'GMV', value: formatPrice(MOCK_METRICS.gmvMinor), icon: IndianRupee },
  { label: 'Active Agents', value: String(MOCK_METRICS.activeAgents), icon: Bot },
]

export function Metrics() {
  return (
    <div className="space-y-8">
      <header>
        <h1 className="font-display text-4xl font-semibold text-foreground">
          Platform Metrics
        </h1>
        <p className="mt-1 font-body text-foreground-secondary">
          A platform-wide view of commerce activity and operations.
        </p>
      </header>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {STATS.map((s, i) => (
          <motion.div
            key={s.label}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.05 }}
          >
            <Card className="shadow-sm">
              <CardContent className="flex items-center justify-between p-5">
                <div>
                  <p className="font-body text-sm text-foreground-secondary">
                    {s.label}
                  </p>
                  <p className="mt-1 font-display text-3xl font-semibold text-foreground">
                    {s.value}
                  </p>
                </div>
                <span className="flex h-11 w-11 items-center justify-center rounded-button bg-accent-light text-accent">
                  <s.icon className="h-5 w-5" />
                </span>
              </CardContent>
            </Card>
          </motion.div>
        ))}
      </div>

      <Card className="shadow-sm">
        <CardHeader>
          <CardTitle className="font-display text-2xl">Orders per day</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-80 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={MOCK_ORDERS_PER_DAY}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0ebe6" vertical={false} />
                <XAxis
                  dataKey="day"
                  tick={{ fontFamily: 'Plus Jakarta Sans', fontSize: 12, fill: '#8a7d72' }}
                  axisLine={{ stroke: '#f0ebe6' }}
                  tickLine={false}
                />
                <YAxis
                  tick={{ fontFamily: 'Plus Jakarta Sans', fontSize: 12, fill: '#8a7d72' }}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip
                  cursor={{ fill: '#fdf0e6' }}
                  formatter={(value) => [Number(value).toLocaleString(), 'Orders']}
                  contentStyle={{
                    borderRadius: 12,
                    border: '1px solid #f0ebe6',
                    fontFamily: 'Plus Jakarta Sans',
                  }}
                />
                <Bar dataKey="orders" fill="#c4956a" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
