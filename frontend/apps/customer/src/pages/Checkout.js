import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ordersApi } from '@loqal/api-client';
import { Button, Card, CardContent, Input } from '@loqal/ui';
import { CheckCircle2 } from 'lucide-react';
import { useCartStore } from '@/store/cartStore';
import { formatPrice } from '@/lib/format';
import { MERCHANT_ID } from '@/lib/constants';
export function Checkout() {
    const items = useCartStore((s) => s.items);
    const subtotalMinor = useCartStore((s) => s.subtotalMinor());
    const clear = useCartStore((s) => s.clear);
    const navigate = useNavigate();
    const [coupon, setCoupon] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [createdOrderId, setCreatedOrderId] = useState(null);
    if (createdOrderId) {
        return (_jsxs("div", { className: "mx-auto max-w-md px-4 py-24 text-center", children: [_jsx(CheckCircle2, { className: "mx-auto h-16 w-16 text-success" }), _jsx("h1", { className: "mt-4 font-display text-4xl text-foreground", children: "Order placed!" }), _jsx("p", { className: "mt-2 font-body text-foreground-secondary", children: "Your order is confirmed and the merchant is preparing it." }), _jsxs("div", { className: "mt-6 flex justify-center gap-3", children: [_jsx(Button, { asChild: true, children: _jsx(Link, { to: `/orders/${createdOrderId}`, children: "Track your order" }) }), _jsx(Button, { asChild: true, variant: "outline", children: _jsx(Link, { to: "/", children: "Continue shopping" }) })] })] }));
    }
    if (items.length === 0) {
        return (_jsxs("div", { className: "mx-auto max-w-md px-4 py-24 text-center", children: [_jsx("h1", { className: "font-display text-3xl text-foreground", children: "Nothing to check out" }), _jsx(Button, { asChild: true, className: "mt-6", children: _jsx(Link, { to: "/", children: "Browse the store" }) })] }));
    }
    const handlePlaceOrder = async () => {
        setError(null);
        setLoading(true);
        try {
            const request = {
                items: items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
                merchantId: MERCHANT_ID,
                couponCode: coupon.trim() || undefined,
            };
            const res = await ordersApi.create(request, crypto.randomUUID());
            clear();
            setCreatedOrderId(res.orderId);
        }
        catch (err) {
            setError(err instanceof Error ? err.message : 'Could not place order');
        }
        finally {
            setLoading(false);
        }
    };
    return (_jsxs("div", { className: "mx-auto max-w-4xl px-4 py-10 sm:px-6", children: [_jsx("h1", { className: "mb-6 font-display text-4xl font-semibold text-foreground", children: "Checkout" }), _jsxs("div", { className: "grid gap-8 md:grid-cols-3", children: [_jsx("div", { className: "md:col-span-2", children: _jsx(Card, { children: _jsxs(CardContent, { className: "p-6", children: [_jsx("h2", { className: "font-display text-2xl text-foreground", children: "Order items" }), _jsx("ul", { className: "mt-4 divide-y divide-border", children: items.map((i) => (_jsxs("li", { className: "flex items-center gap-4 py-3", children: [_jsx("img", { src: i.imageUrl, alt: "", className: "h-14 w-14 rounded-card object-cover" }), _jsxs("div", { className: "flex-1", children: [_jsx("p", { className: "font-body text-foreground", children: i.name }), _jsxs("p", { className: "font-body text-sm text-foreground-secondary", children: ["Qty ", i.quantity, " \u00D7 ", formatPrice(i.priceMinor)] })] }), _jsx("span", { className: "font-display text-accent", children: formatPrice(i.priceMinor * i.quantity) })] }, i.productId))) }), _jsxs("div", { className: "mt-4", children: [_jsx("label", { className: "font-body text-sm text-foreground-secondary", children: "Coupon code" }), _jsx(Input, { value: coupon, onChange: (e) => setCoupon(e.target.value), placeholder: "Optional", className: "mt-1" })] })] }) }) }), _jsx(Card, { className: "h-fit", children: _jsxs(CardContent, { className: "p-6", children: [_jsx("h2", { className: "font-display text-2xl text-foreground", children: "Total" }), _jsxs("div", { className: "mt-4 flex justify-between font-body font-semibold text-foreground", children: [_jsx("span", { children: "Amount due" }), _jsx("span", { className: "font-display text-xl text-accent", children: formatPrice(subtotalMinor) })] }), error && _jsx("p", { className: "mt-3 font-body text-sm text-error", children: error }), _jsx(Button, { className: "mt-6 w-full", onClick: handlePlaceOrder, disabled: loading, children: loading ? 'Placing order…' : 'Place Order' }), _jsx(Button, { variant: "ghost", className: "mt-2 w-full", onClick: () => navigate('/cart'), children: "Back to cart" })] }) })] })] }));
}
