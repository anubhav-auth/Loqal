import { useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import {
  CheckCircle2,
  XCircle,
  Ticket,
  Flag,
  UserPlus,
  Settings2,
  ScrollText,
} from 'lucide-react'
import { Card, CardContent, Badge } from '@loqal/ui'
import { MOCK_AUDIT, type AuditAction, type AuditEvent } from '@/lib/mockAdmin'

const ACTION_META: Record<
  AuditAction,
  { icon: typeof CheckCircle2; label: string; tint: string }
> = {
  MERCHANT_APPROVED: { icon: CheckCircle2, label: 'Merchant approved', tint: 'text-success' },
  MERCHANT_REJECTED: { icon: XCircle, label: 'Merchant rejected', tint: 'text-error' },
  COUPON_CREATED: { icon: Ticket, label: 'Coupon created', tint: 'text-accent' },
  ORDER_FLAGGED: { icon: Flag, label: 'Order flagged', tint: 'text-error' },
  AGENT_INVITED: { icon: UserPlus, label: 'Agent invited', tint: 'text-accent' },
  SETTINGS_UPDATED: { icon: Settings2, label: 'Settings updated', tint: 'text-foreground-secondary' },
}

const FILTERS: Array<{ value: AuditAction | 'ALL'; label: string }> = [
  { value: 'ALL', label: 'All' },
  { value: 'MERCHANT_APPROVED', label: 'Approvals' },
  { value: 'MERCHANT_REJECTED', label: 'Rejections' },
  { value: 'ORDER_FLAGGED', label: 'Flags' },
  { value: 'COUPON_CREATED', label: 'Coupons' },
  { value: 'AGENT_INVITED', label: 'Agents' },
  { value: 'SETTINGS_UPDATED', label: 'Settings' },
]

export function AuditTrail() {
  const [filter, setFilter] = useState<AuditAction | 'ALL'>('ALL')

  const events = useMemo<AuditEvent[]>(() => {
    const sorted = [...MOCK_AUDIT].sort((a, b) =>
      b.timestamp.localeCompare(a.timestamp)
    )
    return filter === 'ALL'
      ? sorted
      : sorted.filter((e) => e.action === filter)
  }, [filter])

  return (
    <div className="space-y-8">
      <header>
        <h1 className="font-display text-4xl font-semibold text-foreground">
          Audit Trail
        </h1>
        <p className="mt-1 font-body text-foreground-secondary">
          A chronological record of administrative actions across the platform.
        </p>
      </header>

      <div className="flex flex-wrap gap-2">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            type="button"
            onClick={() => setFilter(f.value)}
            className={
              filter === f.value
                ? 'rounded-button bg-accent-light px-4 py-1.5 font-body text-sm text-accent'
                : 'rounded-button border border-border px-4 py-1.5 font-body text-sm text-foreground-secondary hover:bg-accent-light/60'
            }
          >
            {f.label}
          </button>
        ))}
      </div>

      <Card className="shadow-sm">
        <CardContent className="p-0">
          <ul className="divide-y divide-border">
            {events.map((e, i) => {
              const meta = ACTION_META[e.action]
              const Icon = meta.icon
              return (
                <motion.li
                  key={e.id}
                  initial={{ opacity: 0, x: -8 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: i * 0.03 }}
                  className="flex items-center gap-4 px-5 py-4"
                >
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-button bg-accent-light">
                    <Icon className="h-5 w-5 text-accent" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="font-body text-sm font-medium text-foreground">
                      {meta.label}: <span className="text-foreground-secondary">{e.target}</span>
                    </p>
                    <p className="font-body text-xs text-foreground-secondary">
                      {e.actor}
                    </p>
                  </div>
                  <div className="text-right">
                    <Badge variant="secondary" size="sm">
                      {e.action}
                    </Badge>
                    <p className="mt-1 font-body text-xs text-foreground-secondary">
                      {new Date(e.timestamp).toLocaleString()}
                    </p>
                  </div>
                </motion.li>
              )
            })}
          </ul>

          {events.length === 0 && (
            <div className="flex flex-col items-center gap-2 px-5 py-12 text-foreground-secondary">
              <ScrollText className="h-8 w-8" />
              <p className="font-body text-sm">No events for this filter.</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
