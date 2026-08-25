export function formatPrice(priceMinor: number): string {
  return `₹${(priceMinor / 100).toFixed(2)}`
}
