import { api } from "../client"
import type {
  Order,
  OrderRequest,
  OrderCreationResponse,
} from "../types"

export const ordersApi = {
  create(request: OrderRequest, idempotencyKey: string): Promise<OrderCreationResponse> {
    return api.post<OrderCreationResponse>("/api/orders", request, {
      headers: { "Idempotency-Key": idempotencyKey },
    })
  },

  getById(id: string): Promise<Order> {
    return api.get<Order>(`/api/orders/${id}`)
  },

  getByUser(userId: string): Promise<Order[]> {
    return api.get<Order[]>(`/api/orders?userId=${encodeURIComponent(userId)}`)
  },

  cancel(id: string): Promise<Order> {
    return api.post<Order>(`/api/orders/${id}/cancel`)
  },

  return(id: string): Promise<Order> {
    return api.post<Order>(`/api/orders/${id}/return`)
  },
}
