import { api } from "../client"
import type { Product, ProductPrice } from "../types"

export const productsApi = {
  getById(id: string): Promise<Product> {
    return api.get<Product>(`/products/${id}`)
  },

  getByMerchant(merchantId: string): Promise<Product[]> {
    return api.get<Product[]>(`/products/merchant?merchantId=${encodeURIComponent(merchantId)}`)
  },

  search(query: string): Promise<Product[]> {
    return api.get<Product[]>(`/products/search?query=${encodeURIComponent(query)}`)
  },

  findPrice(id: string): Promise<ProductPrice> {
    return api.get<ProductPrice>(`/products/${id}/price`)
  },

  create(merchantId: string, data: Omit<Product, "id" | "merchantId">): Promise<Product> {
    return api.post<Product>(`/products/${merchantId}`, data)
  },

  update(productId: string, merchantId: string, data: Partial<Product>): Promise<Product> {
    return api.put<Product>(`/products/${productId}/${merchantId}`, data)
  },

  delete(id: string, merchantId: string): Promise<void> {
    return api.delete<void>(`/products/${id}/${merchantId}`)
  },
}
