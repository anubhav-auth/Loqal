export function formatPrice(priceMinor) {
    return `₹${(priceMinor / 100).toFixed(2)}`;
}
