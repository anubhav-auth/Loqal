import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { MapPin, ChevronRight } from 'lucide-react'
import { Card, CardContent, Badge, type BadgeProps } from '@loqal/ui'
import { formatPrice } from '@/lib/format'
import { mockAssignments, type AssignmentStatus } from '@/lib/mockAssignments'

const statusMeta: Record<
  AssignmentStatus,
  { label: string; variant: BadgeProps['variant'] }
> = {
  PENDING_PICKUP: { label: 'To pick up', variant: 'secondary' },
  PICKED_UP: { label: 'On the way', variant: 'accent' },
  DELIVERED: { label: 'Delivered', variant: 'success' },
}

export function Assignments() {
  const navigate = useNavigate()

  return (
    <div className="space-y-6">
      <header>
        <h1 className="font-display text-3xl font-semibold text-foreground">
          Assignments
        </h1>
        <p className="font-body text-sm text-foreground-secondary">
          {mockAssignments.length} deliveries in your queue
        </p>
      </header>

      <div className="space-y-4">
        {mockAssignments.map((a, i) => {
          const meta = statusMeta[a.status]
          return (
            <motion.div
              key={a.id}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.05 }}
            >
              <Card
                role="button"
                tabIndex={0}
                onClick={() => navigate(`/otp/${a.orderId}`)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault()
                    navigate(`/otp/${a.orderId}`)
                  }
                }}
                className="cursor-pointer shadow-sm transition-shadow hover:shadow-md"
              >
                <CardContent className="flex items-center gap-4 p-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center justify-between gap-2">
                      <p className="font-body text-xs font-medium text-foreground-secondary">
                        #{a.orderId}
                      </p>
                      <Badge variant={meta.variant} size="sm">
                        {meta.label}
                      </Badge>
                    </div>
                    <p className="mt-1 truncate font-body font-semibold text-foreground">
                      {a.customerName}
                    </p>
                    <div className="mt-1 flex items-start gap-1.5 text-foreground-secondary">
                      <MapPin size={15} className="mt-0.5 shrink-0 text-accent" />
                      <span className="truncate font-body text-xs">{a.address}</span>
                    </div>
                    <p className="mt-2 font-display text-lg font-semibold text-foreground">
                      {formatPrice(a.amountMinor)}
                    </p>
                  </div>
                  <ChevronRight size={22} className="shrink-0 text-foreground-muted" />
                </CardContent>
              </Card>
            </motion.div>
          )
        })}
      </div>
    </div>
  )
}
