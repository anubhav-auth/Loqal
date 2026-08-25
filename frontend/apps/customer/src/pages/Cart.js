import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Link } from 'react-router-dom';
import { Button, Card, CardContent } from '@loqal/ui';
import { ShoppingBag } from 'lucide-react';
import { CartItem } from '@/components/CartItem';
import { useCartStore } from '@/store/cartStore';
import { formatPrice } from '@/lib/format';
export function Cart() {
    const items = useCartStore((s) => s.items);
    const subtotalMinor = useCartStore((s) => s.subtotalMinor());
    const itemCount = useCartStore((s) => s.itemCount());
    if (items.length === 0) {
        return (_jsxs("div", { className: "mx-auto flex max-w-md flex-col items-center justify-center px-4 py-24 text-center", children: [_jsx(ShoppingBag, { className: "h-12 w-12 text-foreground-secondary" }), _jsx("h1", { className: "mt-4 font-display text-3xl text-foreground", children: "Your cart is empty" }), _jsx(Button, { asChild: true, className: "mt-6", children: _jsx(Link, { to: "/", children: "Browse the store" }) })] }));
    }
    return (_jsxs("div", { className: "mx-auto max-w-4xl px-4 py-10 sm:px-6", children: [_jsx("h1", { className: "mb-6 font-display text-4xl font-semibold text-foreground", children: "Your Cart" }), _jsxs("div", { className: "grid gap-8 md:grid-cols-3", children: [_jsx("div", { className: "md:col-span-2", children: items.map((item) => (_jsx(CartItem, { item: item }, item.productId))) }), _jsx(Card, { className: "h-fit", children: _jsxs(CardContent, { className: "p-6", children: [_jsx("h2", { className: "font-display text-2xl text-foreground", children: "Summary" }), _jsxs("div", { className: "mt-4 flex justify-between font-body text-foreground-secondary", children: [_jsxs("span", { children: ["Items (", itemCount, ")"] }), _jsx("span", { children: formatPrice(subtotalMinor) })] }), _jsxs("div", { className: "mt-2 flex justify-between font-body font-semibold text-foreground", children: [_jsx("span", { children: "Total" }), _jsx("span", { className: "font-display text-xl text-accent", children: formatPrice(subtotalMinor) })] }), _jsx(Button, { asChild: true, className: "mt-6 w-full", children: _jsx(Link, { to: "/checkout", children: "Proceed to checkout" }) })] }) })] })] }));
}
