import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ordersApi } from '@loqal/api-client';
import { Button, Card, CardContent, Skeleton } from '@loqal/ui';
import { Package, CookingPot, Bike, CheckCircle2 } from 'lucide-react';
import { formatPrice } from '@/lib/format';
const STATUS_FLOW = ['PLACED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED'];
const STATUS_META = {
    PLACED: { label: 'Order placed', icon: Package },
    PREPARING: { label: 'Being prepared', icon: CookingPot },
    OUT_FOR_DELIVERY: { label: 'Out for delivery', icon: Bike },
    DELIVERED: { label: 'Delivered', icon: CheckCircle2 },
};
export function OrderTracking() {
    const { id } = useParams();
    const { data: order, isLoading, isError } = useQuery({
        queryKey: ['order', id],
        queryFn: () => ordersApi.getById(id),
        enabled: !!id,
    });
    if (isLoading) {
        return (_jsx("div", { className: "mx-auto max-w-3xl px-4 py-10", children: _jsx(Skeleton, { className: "h-64 w-full rounded-card" }) }));
    }
    if (isError || !order) {
        return (_jsxs("div", { className: "mx-auto max-w-3xl px-4 py-20 text-center", children: [_jsx("p", { className: "font-body text-error", children: "Order not found." }), _jsx(Button, { asChild: true, variant: "link", children: _jsx(Link, { to: "/", children: "Back to store" }) })] }));
    }
    const currentIndex = Math.max(0, STATUS_FLOW.indexOf(order.currentStatus));
    return (_jsxs("div", { className: "mx-auto max-w-3xl px-4 py-10 sm:px-6", children: [_jsx(Link, { to: "/", className: "font-body text-sm text-foreground-secondary hover:text-accent", children: "\u2190 Back to store" }), _jsxs("h1", { className: "mt-4 font-display text-4xl font-semibold text-foreground", children: ["Order #", order.id.slice(0, 8)] }), _jsx(Card, { className: "mt-6", children: _jsx(CardContent, { className: "p-6", children: _jsx("ol", { className: "relative space-y-6 border-l border-border pl-6", children: STATUS_FLOW.map((status, i) => {
                            const meta = STATUS_META[status];
                            const Icon = meta.icon;
                            const done = i <= currentIndex;
                            return (_jsxs("li", { className: "flex items-center gap-3", children: [_jsx("span", { className: `absolute -left-[15px] flex h-7 w-7 items-center justify-center rounded-full ${done ? 'bg-accent text-white' : 'bg-accent-light text-accent'}`, children: _jsx(Icon, { className: "h-4 w-4" }) }), _jsx("span", { className: `font-body ${done ? 'text-foreground' : 'text-foreground-secondary'}`, children: meta.label })] }, status));
                        }) }) }) }), _jsx(Card, { className: "mt-6", children: _jsxs(CardContent, { className: "p-6", children: [_jsx("h2", { className: "font-display text-2xl text-foreground", children: "Items" }), _jsx("ul", { className: "mt-3 divide-y divide-border", children: order.items.map((item) => (_jsxs("li", { className: "flex justify-between py-2 font-body", children: [_jsxs("span", { className: "text-foreground", children: [item.quantity, " \u00D7 ", item.productId.slice(0, 8)] }), _jsx("span", { className: "text-foreground-secondary", children: formatPrice(item.priceAtPurchaseMinor * item.quantity) })] }, item.productId))) }), _jsxs("div", { className: "mt-4 flex justify-between border-t border-border pt-4 font-body font-semibold", children: [_jsx("span", { children: "Total paid" }), _jsx("span", { className: "font-display text-xl text-accent", children: formatPrice(order.finalAmountMinor) })] })] }) }), _jsx(Card, { className: "mt-6", children: _jsxs(CardContent, { className: "p-6", children: [_jsx("h2", { className: "font-display text-2xl text-foreground", children: "Delivery partner" }), _jsxs("div", { className: "mt-3 flex items-center gap-4", children: [_jsx("div", { className: "flex h-12 w-12 items-center justify-center rounded-full bg-accent-light font-display text-lg text-accent", children: "DA" }), _jsxs("div", { children: [_jsx("p", { className: "font-body text-foreground", children: "Agent assigned" }), _jsx("p", { className: "font-body text-sm text-foreground-secondary", children: "Live tracking will appear here once the agent picks up your order." })] })] })] }) })] }));
}
