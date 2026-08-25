import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { CheckCircle2, XCircle, Ticket, Flag, UserPlus, Settings2, ScrollText, } from 'lucide-react';
import { Card, CardContent, Badge } from '@loqal/ui';
import { MOCK_AUDIT } from '@/lib/mockAdmin';
const ACTION_META = {
    MERCHANT_APPROVED: { icon: CheckCircle2, label: 'Merchant approved', tint: 'text-success' },
    MERCHANT_REJECTED: { icon: XCircle, label: 'Merchant rejected', tint: 'text-error' },
    COUPON_CREATED: { icon: Ticket, label: 'Coupon created', tint: 'text-accent' },
    ORDER_FLAGGED: { icon: Flag, label: 'Order flagged', tint: 'text-error' },
    AGENT_INVITED: { icon: UserPlus, label: 'Agent invited', tint: 'text-accent' },
    SETTINGS_UPDATED: { icon: Settings2, label: 'Settings updated', tint: 'text-foreground-secondary' },
};
const FILTERS = [
    { value: 'ALL', label: 'All' },
    { value: 'MERCHANT_APPROVED', label: 'Approvals' },
    { value: 'MERCHANT_REJECTED', label: 'Rejections' },
    { value: 'ORDER_FLAGGED', label: 'Flags' },
    { value: 'COUPON_CREATED', label: 'Coupons' },
    { value: 'AGENT_INVITED', label: 'Agents' },
    { value: 'SETTINGS_UPDATED', label: 'Settings' },
];
export function AuditTrail() {
    const [filter, setFilter] = useState('ALL');
    const events = useMemo(() => {
        const sorted = [...MOCK_AUDIT].sort((a, b) => b.timestamp.localeCompare(a.timestamp));
        return filter === 'ALL'
            ? sorted
            : sorted.filter((e) => e.action === filter);
    }, [filter]);
    return (_jsxs("div", { className: "space-y-8", children: [_jsxs("header", { children: [_jsx("h1", { className: "font-display text-4xl font-semibold text-foreground", children: "Audit Trail" }), _jsx("p", { className: "mt-1 font-body text-foreground-secondary", children: "A chronological record of administrative actions across the platform." })] }), _jsx("div", { className: "flex flex-wrap gap-2", children: FILTERS.map((f) => (_jsx("button", { type: "button", onClick: () => setFilter(f.value), className: filter === f.value
                        ? 'rounded-button bg-accent-light px-4 py-1.5 font-body text-sm text-accent'
                        : 'rounded-button border border-border px-4 py-1.5 font-body text-sm text-foreground-secondary hover:bg-accent-light/60', children: f.label }, f.value))) }), _jsx(Card, { className: "shadow-sm", children: _jsxs(CardContent, { className: "p-0", children: [_jsx("ul", { className: "divide-y divide-border", children: events.map((e, i) => {
                                const meta = ACTION_META[e.action];
                                const Icon = meta.icon;
                                return (_jsxs(motion.li, { initial: { opacity: 0, x: -8 }, animate: { opacity: 1, x: 0 }, transition: { delay: i * 0.03 }, className: "flex items-center gap-4 px-5 py-4", children: [_jsx("span", { className: "flex h-10 w-10 shrink-0 items-center justify-center rounded-button bg-accent-light", children: _jsx(Icon, { className: "h-5 w-5 text-accent" }) }), _jsxs("div", { className: "min-w-0 flex-1", children: [_jsxs("p", { className: "font-body text-sm font-medium text-foreground", children: [meta.label, ": ", _jsx("span", { className: "text-foreground-secondary", children: e.target })] }), _jsx("p", { className: "font-body text-xs text-foreground-secondary", children: e.actor })] }), _jsxs("div", { className: "text-right", children: [_jsx(Badge, { variant: "secondary", size: "sm", children: e.action }), _jsx("p", { className: "mt-1 font-body text-xs text-foreground-secondary", children: new Date(e.timestamp).toLocaleString() })] })] }, e.id));
                            }) }), events.length === 0 && (_jsxs("div", { className: "flex flex-col items-center gap-2 px-5 py-12 text-foreground-secondary", children: [_jsx(ScrollText, { className: "h-8 w-8" }), _jsx("p", { className: "font-body text-sm", children: "No events for this filter." })] }))] }) })] }));
}
