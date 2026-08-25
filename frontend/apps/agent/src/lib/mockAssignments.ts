// MOCK DATA — no dedicated delivery endpoint client exists yet.
// These assignment/OTP shapes mirror @loqal/api-client Delivery/Order types
// and will be swapped for a real deliveryApi once the backend is ready.

export type AssignmentStatus = 'PENDING_PICKUP' | 'PICKED_UP' | 'DELIVERED'

export interface Assignment {
  id: string
  orderId: string
  customerName: string
  address: string
  amountMinor: number
  status: AssignmentStatus
  pickupOtp: string
  deliveryOtp: string
}

export const AGENT_ID = '00000000-0000-0000-0000-000000000002'

export const mockAssignments: Assignment[] = [
  {
    id: 'del_001',
    orderId: 'ord_a1b2c3',
    customerName: 'Ananya Rao',
    address: '12 Rosewood Lane, Indiranagar, Bengaluru 560038',
    amountMinor: 84900,
    status: 'PENDING_PICKUP',
    pickupOtp: '4821',
    deliveryOtp: '7390',
  },
  {
    id: 'del_002',
    orderId: 'ord_d4e5f6',
    customerName: 'Mohit Verma',
    address: '88 Cedar Court, Koramangala, Bengaluru 560034',
    amountMinor: 129900,
    status: 'PICKED_UP',
    pickupOtp: '1156',
    deliveryOtp: '9043',
  },
  {
    id: 'del_003',
    orderId: 'ord_g7h8i9',
    customerName: 'Sara Iqbal',
    address: '5 Maple Street, Whitefield, Bengaluru 560066',
    amountMinor: 45900,
    status: 'PENDING_PICKUP',
    pickupOtp: '3320',
    deliveryOtp: '6618',
  },
]
