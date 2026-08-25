import { motion } from 'framer-motion'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from 'recharts'
import {
  IndianRupee,
  ShoppingBag,
  PackageX,
  ReceiptText,
} from 'lucide-react'
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Badge,
} from '@loqal/ui'
import { formatPrice } from '@/lib/format'
import { MOCK_ORDERS } from '@/lib/mockOrders'

const REVENUE_DATA = [
  { day: 'Mon', revenue: 12000 },
  { day: 'Tue', revenue: 18500 },
  { day: 'Wed', revenue: 9800 },
  { day: 'Thu', revenue: 22400 },
  { day: 'Fri', revenue: 31000 },
  { day: 'Sat', revenue: 27600 },
  { day: 'Sun', revenue: 15200 },
]

const STATS = [
  { label: 'Revenue Today', value: formatPrice(15200), icon: IndianRupee },
  { label: 'Orders', value: '38', icon: ShoppingBag },
  { label: 'Low Stock', value: '4', icon: PackageX },
  { label: 'Avg Order', value: formatPrice(2140), icon: ReceiptText },
]

function statusVariant(
  status: string
): 'success' | 'accent' | 'secondary' | 'error' | 'default' {
  switch (status) {
    case 'DELIVERED':
      return 'success'
    case 'SHIPPED':
      return 'accent'
    case 'CONFIRMED':
      return 'secondary'
    case 'CANCELLED':
      return 'error'
    default:
      return 'default'
  }
}

export function Dashboard() {
  const recent = MOCK_ORDERS.slice(0, 5)

  return (
    <div className="space-y-8">
      <header>
        <h1 className="font-display text-4xl font-semibold text-foreground">
          Dashboard
        </h1>
        <p className="mt-1 font-body text-foreground-secondary">
          A snapshot of how your storefront is performing today.
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

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card className="shadow-sm lg:col-span-2">
          <CardHeader>
            <CardTitle className="font-display text-2xl">Revenue (7 days)</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-72 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={REVENUE_DATA}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0ebe6" />
                  <XAxis
                    dataKey="day"
                    tick={{ fontFamily: 'Plus Jakarta Sans', fontSize: 12, fill: '#8a7d72' }}
                  />
                  <YAxis
                    tickFormatter={(v) => `₹${(v / 100).toFixed(0)}`}
                    tick={{ fontFamily: 'Plus Jakarta Sans', fontSize: 12, fill: '#8a7d72' }}
                  />
                  <Tooltip
                    formatter={(value) => [formatPrice(Number(value)), 'Revenue']}
                    contentStyle={{
                      borderRadius: 12,
                      border: '1px solid #f0ebe6',
                      fontFamily: 'Plus Jakarta Sans',
                    }}
                  />
                  <Line
                    type="monotone"
                    dataKey="revenue"
                    stroke="#c4956a"
                    strokeWidth={3}
                    dot={{ r: 4, fill: '#c4956a' }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        <Card className="shadow-sm">
          <CardHeader>
            <CardTitle className="font-display text-2xl">Recent Orders</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {recent.map((o) => (
              <div
                key={o.id}
                className="flex items-center justify-between rounded-card border border-border bg-background px-4 py-3"
              >
                <div>
                  <p className="font-body text-sm font-medium text-foreground">
                    #{o.id.slice(4)}
                  </p>
                  <p className="font-body text-xs text-foreground-secondary">
                    {formatPrice(o.finalAmountMinor)}
                  </p>
                </div>
                <Badge variant={statusVariant(o.currentStatus)}>
                  {o.currentStatus}
                </Badge>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
