export interface Address {
  street?: string
  city?: string
  state?: string
  postalCode?: string
  country?: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
}

export interface UserProfile {
  userId: string
  fullName: string
  email: string
  phoneNumber?: string
  tenantId: string
  address?: Address
}

export interface Product {
  id: string
  name: string
  description?: string
  priceMinor: number
  quantity: number
  imageUrls: string[]
  merchantId: string
}

export interface ProductPrice {
  productId: string
  priceMinor: number
  quantityAvailable: number
  available: boolean
}

export interface OrderItem {
  productId: string
  quantity: number
  priceAtPurchaseMinor: number
}

export interface Order {
  id: string
  customerId: string
  currentStatus: string
  totalAmountMinor: number
  discountAmountMinor: number
  finalAmountMinor: number
  razorpayOrderId?: string
  items: OrderItem[]
}

export interface OrderRequest {
  items: { productId: string; quantity: number }[]
  merchantId: string
  couponCode?: string
}

export interface OrderCreationResponse {
  orderId: string
  razorpayOrderId: string
}

export interface PaymentInitiation {
  paymentId: string
  razorpayOrderId: string
  amountMinor: number
  currency: string
}

export interface Coupon {
  id: string
  code: string
  discountType: string
  value: number
  minOrderValueMinor?: number
  maxDiscountMinor?: number
  validFrom: string
  validUntil: string
  active: boolean
}

export interface DiscountResult {
  discountMinor: number
}

export interface Delivery {
  id: string
  orderId: string
  agentId: string
  status: string
  pickupOtp?: string
  deliveredOtp?: string
}

export interface Agent {
  id: string
  tenantId: string
  userId: string
  status: string
  currentLat?: number
  currentLng?: number
}

export interface ChatMessage {
  id: string
  roomId: string
  senderId: string
  senderRole: string
  content: string
  createdAt: string
}

export interface Notification {
  id: string
  channel: string
  recipient: string
  template: string
  body: string
  status: string
}
