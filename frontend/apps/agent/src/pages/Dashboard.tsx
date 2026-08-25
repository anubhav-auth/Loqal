import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { MapPin, Wallet, Clock, Power } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle, Button, Badge } from '@loqal/ui'
import { formatPrice } from '@/lib/format'
import { mockAssignments } from '@/lib/mockAssignments'

export function Dashboard() {
  const navigate = useNavigate()
  const [online, setOnline] = useState(false)
  const [clockedIn, setClockedIn] = useState(false)

  // MOCK earnings figure — replace with real agent stats API.
  const todayEarningsMinor = 2187500
  const current = mockAssignments.find((a) => a.status !== 'DELIVERED')

  return (
    <div className="space-y-6">
      <header>
        <p className="font-body text-sm text-foreground-secondary">Good shift,</p>
        <h1 className="font-display text-3xl font-semibold text-foreground">
          Delivery Agent
        </h1>
      </header>

      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        className="rounded-card border border-border bg-surface p-5 shadow-sm"
      >
        <div className="flex items-center justify-between">
          <div>
            <p className="font-body text-sm text-foreground-secondary">
              Availability
            </p>
            <p
              className={`font-display text-2xl font-semibold ${
                online ? 'text-success' : 'text-foreground-secondary'
              }`}
            >
              {online ? 'Online' : 'Offline'}
            </p>
          </div>
          <button
            role="switch"
            aria-checked={online}
            aria-label="Toggle availability"
            onClick={() => setOnline((v) => !v)}
            className={`relative h-12 w-20 rounded-button border border-border transition-colors ${
              online ? 'bg-success' : 'bg-border'
            }`}
          >
            <span
              className={`absolute top-1 h-10 w-10 rounded-full bg-white shadow-md transition-all ${
                online ? 'left-9' : 'left-1'
              }`}
            />
          </button>
        </div>

        <Button
          variant={clockedIn ? 'secondary' : 'default'}
          className="mt-5 w-full py-4 text-base"
          onClick={() => setClockedIn((v) => !v)}
        >
          <Power size={18} className="mr-2" />
          {clockedIn ? 'Clock Out' : 'Clock In'}
        </Button>
      </motion.div>

      <Card className="shadow-sm">
        <CardHeader className="flex-row items-center gap-3 space-y-0">
          <span className="flex h-10 w-10 items-center justify-center rounded-card bg-accent-light text-accent">
            <Wallet size={20} />
          </span>
          <CardTitle className="font-display text-xl">Today's Earnings</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="font-display text-4xl font-semibold text-foreground">
            {formatPrice(todayEarningsMinor)}
          </p>
          <p className="mt-1 font-body text-sm text-foreground-secondary">
            7 deliveries completed
          </p>
        </CardContent>
      </Card>

      {current && (
        <Card className="border-accent/30 shadow-md">
          <CardHeader className="flex-row items-center justify-between space-y-0">
            <CardTitle className="font-display text-xl">Current Delivery</CardTitle>
            <Badge variant={current.status === 'PICKED_UP' ? 'accent' : 'secondary'}>
              {current.status === 'PICKED_UP' ? 'On the way' : 'To pick up'}
            </Badge>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex items-start gap-2 text-foreground-secondary">
              <MapPin size={18} className="mt-0.5 shrink-0 text-accent" />
              <span className="font-body text-sm">{current.address}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="font-body text-sm text-foreground-secondary">
                {current.customerName} · {formatPrice(current.amountMinor)}
              </span>
              <Button
                variant="default"
                className="bg-accent py-3 text-white hover:bg-accent/90"
                onClick={() => navigate(`/otp/${current.orderId}`)}
              >
                Verify OTP
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {!current && (
        <div className="flex flex-col items-center gap-2 rounded-card border border-dashed border-border py-10 text-center">
          <Clock size={28} className="text-foreground-muted" />
          <p className="font-body text-sm text-foreground-secondary">
            No active deliveries. You're all caught up.
          </p>
        </div>
      )}
    </div>
  )
}
