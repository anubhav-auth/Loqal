import { create } from 'zustand';
import { persist } from 'zustand/middleware';
export const useCartStore = create()(persist((set, get) => ({
    items: [],
    addItem: (item, quantity = 1) => set((state) => {
        const existing = state.items.find((i) => i.productId === item.productId);
        if (existing) {
            return {
                items: state.items.map((i) => i.productId === item.productId
                    ? { ...i, quantity: i.quantity + quantity }
                    : i),
            };
        }
        return { items: [...state.items, { ...item, quantity }] };
    }),
    removeItem: (productId) => set((state) => ({
        items: state.items.filter((i) => i.productId !== productId),
    })),
    updateQty: (productId, quantity) => set((state) => ({
        items: state.items.map((i) => i.productId === productId
            ? { ...i, quantity: Math.max(0, quantity) }
            : i),
    })),
    clear: () => set({ items: [] }),
    itemCount: () => get().items.reduce((sum, i) => sum + i.quantity, 0),
    subtotalMinor: () => get().items.reduce((sum, i) => sum + i.priceMinor * i.quantity, 0),
}), { name: 'loqal-cart' }));
