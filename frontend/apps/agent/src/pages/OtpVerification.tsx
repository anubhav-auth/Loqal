import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { MapPin, CheckCircle2, ShieldCheck } from 'lucide-react'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
  Button,
  Input,
  Badge,
  useToast,
} from '@loqal/ui'
import { formatPrice } from '@/lib/format'
import { mockAssignments } from '@/lib/mockAssignments'

export function OtpVerification() {
  const { orderId } = useParams<{ orderId: string }>()
  const navigate = useNavigate()
  const { toast } = useToast()

  const assignment = mockAssignments.find((a) => a.orderId === orderId)

  const [status, setStatus] = useState(assignment?.status ?? 'PENDING_PICKUP')
  const [pickupInput, setPickupInput] = useState('')
  const [deliveryInput, setDeliveryInput] = useState('')
  const [error, setError] = useState('')

  if (!assignment) {
    return (
      <div className="space-y-4 pt-10 text-center">
        <p className="font-body text-foreground-secondary">Assignment not found.</p>
        <Button variant="secondary" onClick={() => navigate('/assignments')}>
          Back to tasks
        </Button>
      </div>
    )
  }

  const verifyPickup = () => {
    setError('')
    if (pickupInput.trim() !== assignment.pickupOtp) {
      setError('Incorrect pickup OTP. Please check with the merchant.')
      return
    }
    setStatus('PICKED_UP')
    setPickupInput('')
    toast('Pickup confirmed — on your way!', 'success')
  }

  const verifyDelivery = () => {
    setError('')
    if (deliveryInput.trim() !== assignment.deliveryOtp) {
      setError('Incorrect delivery OTP. Please check with the customer.')
      return
    }
    setStatus('DELIVERED')
    setDeliveryInput('')
    toast('Delivery completed. Great job!', 'success')
  }

  const stage =
    status === 'PENDING_PICKUP'
      ? 'pickup'
      : status === 'PICKED_UP'
        ? 'delivery'
        : 'done'

  return (
    <div className="space-y-6 pt-2">
      <button
        onClick={() => navigate(-1)}
        className="font-body text-sm text-foreground-secondary"
      >
        ← Back
      </button>

      <Card className="shadow-sm">
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="font-display text-xl">#{assignment.orderId}</CardTitle>
            <Badge
              variant={
                stage === 'done' ? 'success' : stage === 'delivery' ? 'accent' : 'secondary'
              }
              size="sm"
            >
              {stage === 'done'
                ? 'Delivered'
                : stage === 'delivery'
                  ? 'On the way'
                  : 'To pick up'}
            </Badge>
          </div>
          <CardDescription className="font-body">
            {assignment.customerName}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="flex items-start gap-2 text-foreground-secondary">
            <MapPin size={18} className="mt-0.5 shrink-0 text-accent" />
            <span className="font-body text-sm">{assignment.address}</span>
          </div>
          <p className="font-display text-2xl font-semibold text-foreground">
            {formatPrice(assignment.amountMinor)}
          </p>
        </CardContent>
      </Card>

      {stage !== 'done' && (
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-4"
        >
          {stage === 'pickup' && (
            <Card className="shadow-sm">
              <CardHeader className="flex-row items-center gap-3 space-y-0">
                <ShieldCheck size={20} className="text-accent" />
                <CardTitle className="font-display text-lg">Pickup OTP</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Input
                  inputMode="numeric"
                  maxLength={4}
                  placeholder="Enter 4-digit code"
                  value={pickupInput}
                  onChange={(e) => setPickupInput(e.target.value)}
                  className="py-4 text-center text-2xl tracking-[0.5em]"
                />
                <Button className="w-full py-4 text-base" onClick={verifyPickup}>
                  Confirm Pickup
                </Button>
              </CardContent>
            </Card>
          )}

          {stage === 'delivery' && (
            <Card className="shadow-sm">
              <CardHeader className="flex-row items-center gap-3 space-y-0">
                <CheckCircle2 size={20} className="text-accent" />
                <CardTitle className="font-display text-lg">Delivery OTP</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Input
                  inputMode="numeric"
                  maxLength={4}
                  placeholder="Enter 4-digit code"
                  value={deliveryInput}
                  onChange={(e) => setDeliveryInput(e.target.value)}
                  className="py-4 text-center text-2xl tracking-[0.5em]"
                />
                <Button
                  variant="default"
                  className="w-full bg-accent py-4 text-base text-white hover:bg-accent/90"
                  onClick={verifyDelivery}
                >
                  Confirm Delivery
                </Button>
              </CardContent>
            </Card>
          )}

          {error && (
            <p className="text-center font-body text-sm text-error">{error}</p>
          )}

          {stage === 'pickup' && (
            <p className="text-center font-body text-xs text-foreground-muted">
              MOCK OTP (demo): {assignment.pickupOtp}
            </p>
          )}
          {stage === 'delivery' && (
            <p className="text-center font-body text-xs text-foreground-muted">
              MOCK OTP (demo): {assignment.deliveryOtp}
            </p>
          )}
        </motion.div>
      )}

      {stage === 'done' && (
        <div className="flex flex-col items-center gap-3 rounded-card border border-success/30 bg-success/5 py-10 text-center">
          <CheckCircle2 size={40} className="text-success" />
          <p className="font-display text-2xl font-semibold text-foreground">
            Delivered!
          </p>
          <Button variant="secondary" onClick={() => navigate('/assignments')}>
            Back to tasks
          </Button>
        </div>
      )}
    </div>
  )
}
