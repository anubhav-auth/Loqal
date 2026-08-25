import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { motion } from 'framer-motion';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, } from 'recharts';
import { Users, ShoppingBag, IndianRupee, Bot } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@loqal/ui';
import { MOCK_METRICS, MOCK_ORDERS_PER_DAY } from '@/lib/mockAdmin';
function formatPrice(priceMinor) {
    return `₹${(priceMinor / 100).toLocaleString('en-IN', {
        maximumFractionDigits: 0,
    })}`;
}
const STATS = [
    {
        label: 'Total Merchants',
        value: MOCK_METRICS.totalMerchants.toLocaleString(),
        icon: Users,
    },
    {
        label: 'Total Orders',
        value: MOCK_METRICS.totalOrders.toLocaleString(),
        icon: ShoppingBag,
    },
    { label: 'GMV', value: formatPrice(MOCK_METRICS.gmvMinor), icon: IndianRupee },
    { label: 'Active Agents', value: String(MOCK_METRICS.activeAgents), icon: Bot },
];
export function Metrics() {
    return (_jsxs("div", { className: "space-y-8", children: [_jsxs("header", { children: [_jsx("h1", { className: "font-display text-4xl font-semibold text-foreground", children: "Platform Metrics" }), _jsx("p", { className: "mt-1 font-body text-foreground-secondary", children: "A platform-wide view of commerce activity and operations." })] }), _jsx("div", { className: "grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4", children: STATS.map((s, i) => (_jsx(motion.div, { initial: { opacity: 0, y: 12 }, animate: { opacity: 1, y: 0 }, transition: { delay: i * 0.05 }, children: _jsx(Card, { className: "shadow-sm", children: _jsxs(CardContent, { className: "flex items-center justify-between p-5", children: [_jsxs("div", { children: [_jsx("p", { className: "font-body text-sm text-foreground-secondary", children: s.label }), _jsx("p", { className: "mt-1 font-display text-3xl font-semibold text-foreground", children: s.value })] }), _jsx("span", { className: "flex h-11 w-11 items-center justify-center rounded-button bg-accent-light text-accent", children: _jsx(s.icon, { className: "h-5 w-5" }) })] }) }) }, s.label))) }), _jsxs(Card, { className: "shadow-sm", children: [_jsx(CardHeader, { children: _jsx(CardTitle, { className: "font-display text-2xl", children: "Orders per day" }) }), _jsx(CardContent, { children: _jsx("div", { className: "h-80 w-full", children: _jsx(ResponsiveContainer, { width: "100%", height: "100%", children: _jsxs(BarChart, { data: MOCK_ORDERS_PER_DAY, children: [_jsx(CartesianGrid, { strokeDasharray: "3 3", stroke: "#f0ebe6", vertical: false }), _jsx(XAxis, { dataKey: "day", tick: { fontFamily: 'Plus Jakarta Sans', fontSize: 12, fill: '#8a7d72' }, axisLine: { stroke: '#f0ebe6' }, tickLine: false }), _jsx(YAxis, { tick: { fontFamily: 'Plus Jakarta Sans', fontSize: 12, fill: '#8a7d72' }, axisLine: false, tickLine: false }), _jsx(Tooltip, { cursor: { fill: '#fdf0e6' }, formatter: (value) => [Number(value).toLocaleString(), 'Orders'], contentStyle: {
                                                borderRadius: 12,
                                                border: '1px solid #f0ebe6',
                                                fontFamily: 'Plus Jakarta Sans',
                                            } }), _jsx(Bar, { dataKey: "orders", fill: "#c4956a", radius: [8, 8, 0, 0] })] }) }) }) })] })] }));
}
