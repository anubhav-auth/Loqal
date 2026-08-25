import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Eye } from 'lucide-react';
import { Card, CardContent, Button, Badge, Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, useToast, } from '@loqal/ui';
import { ordersApi } from '@loqal/api-client';
import { formatPrice } from '@/lib/format';
import { MOCK_ORDERS } from '@/lib/mockOrders';
const FILTERS = [
    'ALL',
    'PENDING',
    'CONFIRMED',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED',
];
function statusVariant(status) {
    switch (status) {
        case 'DELIVERED':
        case 'RETURNED':
            return 'success';
        case 'SHIPPED':
            return 'accent';
        case 'CONFIRMED':
            return 'secondary';
        case 'CANCELLED':
            return 'error';
        default:
            return 'default';
    }
}
export function Orders() {
    const { toast } = useToast();
    const [orders, setOrders] = useState(MOCK_ORDERS);
    const [filter, setFilter] = useState('ALL');
    const [selected, setSelected] = useState(null);
    const visible = filter === 'ALL'
        ? orders
        : orders.filter((o) => o.currentStatus === filter);
    async function handleCancel(o) {
        try {
            await ordersApi.cancel(o.id);
            setOrders((prev) => prev.map((x) => x.id === o.id ? { ...x, currentStatus: 'CANCELLED' } : x));
            setSelected((s) => (s?.id === o.id ? { ...s, currentStatus: 'CANCELLED' } : s));
            toast('Order cancelled', 'success');
        }
        catch {
            toast('Could not cancel order', 'error');
        }
    }
    async function handleReturn(o) {
        try {
            await ordersApi.return(o.id);
            setOrders((prev) => prev.map((x) => x.id === o.id ? { ...x, currentStatus: 'RETURNED' } : x));
            setSelected((s) => (s?.id === o.id ? { ...s, currentStatus: 'RETURNED' } : s));
            toast('Return requested', 'success');
        }
        catch {
            toast('Could not request return', 'error');
        }
    }
    return (_jsxs("div", { className: "space-y-8", children: [_jsxs("header", { children: [_jsx("h1", { className: "font-display text-4xl font-semibold text-foreground", children: "Orders" }), _jsx("p", { className: "mt-1 font-body text-foreground-secondary", children: "Track and manage every order placed with your store." })] }), _jsx("div", { className: "flex flex-wrap gap-2", children: FILTERS.map((f) => (_jsx("button", { type: "button", onClick: () => setFilter(f), className: f === filter
                        ? 'rounded-button bg-foreground px-4 py-2 font-body text-sm text-white'
                        : 'rounded-button border border-border bg-surface px-4 py-2 font-body text-sm text-foreground-secondary hover:bg-accent-light/60', children: f === 'ALL' ? 'All' : f.charAt(0) + f.slice(1).toLowerCase() }, f))) }), _jsxs("div", { className: "space-y-3", children: [_jsx(AnimatePresence, { children: visible.map((o) => (_jsx(motion.div, { layout: true, initial: { opacity: 0, y: 8 }, animate: { opacity: 1, y: 0 }, exit: { opacity: 0 }, children: _jsx(Card, { className: "shadow-sm", children: _jsxs(CardContent, { className: "flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between", children: [_jsxs("div", { className: "flex items-center gap-4", children: [_jsxs("div", { children: [_jsxs("p", { className: "font-body text-sm font-medium text-foreground", children: ["#", o.id.slice(4)] }), _jsxs("p", { className: "font-body text-xs text-foreground-secondary", children: [o.items.length, " item(s) \u00B7 ", formatPrice(o.finalAmountMinor)] })] }), _jsx(Badge, { variant: statusVariant(o.currentStatus), children: o.currentStatus })] }), _jsxs(Button, { size: "sm", variant: "outline", onClick: () => setSelected(o), children: [_jsx(Eye, { className: "h-4 w-4" }), " View"] })] }) }) }, o.id))) }), visible.length === 0 && (_jsx("div", { className: "rounded-card border border-dashed border-border p-12 text-center", children: _jsx("p", { className: "font-body text-foreground-secondary", children: "No orders match this filter." }) }))] }), _jsx(Dialog, { open: !!selected, onOpenChange: (o) => !o && setSelected(null), children: _jsx(DialogContent, { children: selected && (_jsxs(_Fragment, { children: [_jsxs(DialogHeader, { children: [_jsxs(DialogTitle, { children: ["Order #", selected.id.slice(4)] }), _jsxs(DialogDescription, { children: ["Placed by customer ", selected.customerId] })] }), _jsxs("div", { className: "space-y-2", children: [selected.items.map((it, idx) => (_jsxs("div", { className: "flex items-center justify-between rounded-card border border-border px-4 py-3", children: [_jsxs("span", { className: "font-body text-sm text-foreground", children: [it.quantity, " \u00D7 product ", it.productId.slice(0, 6)] }), _jsx("span", { className: "font-body text-sm text-foreground-secondary", children: formatPrice(it.priceAtPurchaseMinor * it.quantity) })] }, idx))), _jsxs("div", { className: "space-y-1 pt-2 font-body text-sm", children: [_jsx(Row, { label: "Subtotal", value: formatPrice(selected.totalAmountMinor) }), _jsx(Row, { label: "Discount", value: `- ${formatPrice(selected.discountAmountMinor)}` }), _jsx(Row, { label: "Final", value: formatPrice(selected.finalAmountMinor), strong: true })] })] }), _jsxs("div", { className: "flex flex-col-reverse gap-2 sm:flex-row sm:justify-end sm:space-x-2", children: [selected.currentStatus === 'PENDING' ||
                                        selected.currentStatus === 'CONFIRMED' ? (_jsx(Button, { variant: "outline", onClick: () => handleCancel(selected), children: "Cancel Order" })) : null, selected.currentStatus === 'DELIVERED' ? (_jsx(Button, { variant: "outline", onClick: () => handleReturn(selected), children: "Request Return" })) : null, _jsx(Button, { onClick: () => setSelected(null), children: "Close" })] })] })) }) })] }));
}
function Row({ label, value, strong, }) {
    return (_jsxs("div", { className: "flex justify-between", children: [_jsx("span", { className: strong ? 'font-semibold text-foreground' : 'text-foreground-secondary', children: label }), _jsx("span", { className: strong ? 'font-semibold text-foreground' : 'text-foreground', children: value })] }));
}
