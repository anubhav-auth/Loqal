import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { motion } from 'framer-motion';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, } from 'recharts';
import { IndianRupee, ShoppingBag, PackageX, ReceiptText, } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent, Badge, } from '@loqal/ui';
import { formatPrice } from '@/lib/format';
import { MOCK_ORDERS } from '@/lib/mockOrders';
const REVENUE_DATA = [
    { day: 'Mon', revenue: 12000 },
    { day: 'Tue', revenue: 18500 },
    { day: 'Wed', revenue: 9800 },
    { day: 'Thu', revenue: 22400 },
    { day: 'Fri', revenue: 31000 },
    { day: 'Sat', revenue: 27600 },
    { day: 'Sun', revenue: 15200 },
];
const STATS = [
    { label: 'Revenue Today', value: formatPrice(15200), icon: IndianRupee },
    { label: 'Orders', value: '38', icon: ShoppingBag },
    { label: 'Low Stock', value: '4', icon: PackageX },
    { label: 'Avg Order', value: formatPrice(2140), icon: ReceiptText },
];
function statusVariant(status) {
    switch (status) {
        case 'DELIVERED':
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
export function Dashboard() {
    const recent = MOCK_ORDERS.slice(0, 5);
    return (_jsxs("div", { className: "space-y-8", children: [_jsxs("header", { children: [_jsx("h1", { className: "font-display text-4xl font-semibold text-foreground", children: "Dashboard" }), _jsx("p", { className: "mt-1 font-body text-foreground-secondary", children: "A snapshot of how your storefront is performing today." })] }), _jsx("div", { className: "grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4", children: STATS.map((s, i) => (_jsx(motion.div, { initial: { opacity: 0, y: 12 }, animate: { opacity: 1, y: 0 }, transition: { delay: i * 0.05 }, children: _jsx(Card, { className: "shadow-sm", children: _jsxs(CardContent, { className: "flex items-center justify-between p-5", children: [_jsxs("div", { children: [_jsx("p", { className: "font-body text-sm text-foreground-secondary", children: s.label }), _jsx("p", { className: "mt-1 font-display text-3xl font-semibold text-foreground", children: s.value })] }), _jsx("span", { className: "flex h-11 w-11 items-center justify-center rounded-button bg-accent-light text-accent", children: _jsx(s.icon, { className: "h-5 w-5" }) })] }) }) }, s.label))) }), _jsxs("div", { className: "grid grid-cols-1 gap-6 lg:grid-cols-3", children: [_jsxs(Card, { className: "shadow-sm lg:col-span-2", children: [_jsx(CardHeader, { children: _jsx(CardTitle, { className: "font-display text-2xl", children: "Revenue (7 days)" }) }), _jsx(CardContent, { children: _jsx("div", { className: "h-72 w-full", children: _jsx(ResponsiveContainer, { width: "100%", height: "100%", children: _jsxs(LineChart, { data: REVENUE_DATA, children: [_jsx(CartesianGrid, { strokeDasharray: "3 3", stroke: "#f0ebe6" }), _jsx(XAxis, { dataKey: "day", tick: { fontFamily: 'Plus Jakarta Sans', fontSize: 12, fill: '#8a7d72' } }), _jsx(YAxis, { tickFormatter: (v) => `₹${(v / 100).toFixed(0)}`, tick: { fontFamily: 'Plus Jakarta Sans', fontSize: 12, fill: '#8a7d72' } }), _jsx(Tooltip, { formatter: (value) => [formatPrice(Number(value)), 'Revenue'], contentStyle: {
                                                        borderRadius: 12,
                                                        border: '1px solid #f0ebe6',
                                                        fontFamily: 'Plus Jakarta Sans',
                                                    } }), _jsx(Line, { type: "monotone", dataKey: "revenue", stroke: "#c4956a", strokeWidth: 3, dot: { r: 4, fill: '#c4956a' } })] }) }) }) })] }), _jsxs(Card, { className: "shadow-sm", children: [_jsx(CardHeader, { children: _jsx(CardTitle, { className: "font-display text-2xl", children: "Recent Orders" }) }), _jsx(CardContent, { className: "space-y-3", children: recent.map((o) => (_jsxs("div", { className: "flex items-center justify-between rounded-card border border-border bg-background px-4 py-3", children: [_jsxs("div", { children: [_jsxs("p", { className: "font-body text-sm font-medium text-foreground", children: ["#", o.id.slice(4)] }), _jsx("p", { className: "font-body text-xs text-foreground-secondary", children: formatPrice(o.finalAmountMinor) })] }), _jsx(Badge, { variant: statusVariant(o.currentStatus), children: o.currentStatus })] }, o.id))) })] })] })] }));
}
