import { api } from "../client"
import type { PaymentInitiation } from "../types"

export interface WebhookPayload {
  [key: string]: unknown
}

export const paymentsApi = {
  handleWebhook(payload: WebhookPayload, signature: string): Promise<PaymentInitiation> {
    return api.post<PaymentInitiation>("/payments/webhook", payload, {
      headers: { "X-Razorpay-Signature": signature },
    })
  },
}
