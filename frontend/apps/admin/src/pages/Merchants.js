import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { CheckCircle2, Search, Store } from 'lucide-react';
import { Card, CardContent, Badge, Button, Input, useToast } from '@loqal/ui';
import { MOCK_MERCHANTS, } from '@/lib/mockAdmin';
function statusVariant(status) {
    return status === 'active' ? 'success' : 'accent';
}
export function Merchants() {
    const { toast } = useToast();
    const [merchants, setMerchants] = useState(MOCK_MERCHANTS);
    const [query, setQuery] = useState('');
    const filtered = useMemo(() => {
        const q = query.trim().toLowerCase();
        if (!q)
            return merchants;
        return merchants.filter((m) => m.name.toLowerCase().includes(q) || m.email.toLowerCase().includes(q));
    }, [merchants, query]);
    function approve(id) {
        setMerchants((prev) => prev.map((m) => (m.id === id ? { ...m, status: 'active' } : m)));
        const m = merchants.find((x) => x.id === id);
        toast(`${m?.name ?? 'Merchant'} approved`, 'success');
    }
    return (_jsxs("div", { className: "space-y-8", children: [_jsxs("header", { className: "flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between", children: [_jsxs("div", { children: [_jsx("h1", { className: "font-display text-4xl font-semibold text-foreground", children: "Merchants" }), _jsx("p", { className: "mt-1 font-body text-foreground-secondary", children: "Review onboarding requests and activate storefronts." })] }), _jsxs("div", { className: "relative w-full sm:w-72", children: [_jsx(Search, { className: "pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-foreground-secondary" }), _jsx(Input, { value: query, onChange: (e) => setQuery(e.target.value), placeholder: "Search name or email", className: "pl-9" })] })] }), _jsx("div", { className: "grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3", children: filtered.map((m, i) => (_jsx(motion.div, { initial: { opacity: 0, y: 12 }, animate: { opacity: 1, y: 0 }, transition: { delay: i * 0.04 }, children: _jsx(Card, { className: "shadow-sm", children: _jsxs(CardContent, { className: "flex flex-col gap-4 p-5", children: [_jsxs("div", { className: "flex items-start justify-between", children: [_jsxs("div", { className: "flex items-center gap-3", children: [_jsx("span", { className: "flex h-10 w-10 items-center justify-center rounded-button bg-accent-light text-accent", children: _jsx(Store, { className: "h-5 w-5" }) }), _jsxs("div", { children: [_jsx("p", { className: "font-body text-sm font-medium text-foreground", children: m.name }), _jsx("p", { className: "font-body text-xs text-foreground-secondary", children: m.email })] })] }), _jsx(Badge, { variant: statusVariant(m.status), children: m.status })] }), _jsxs("div", { className: "flex items-center justify-between border-t border-border pt-3", children: [_jsxs("span", { className: "font-body text-xs text-foreground-secondary", children: ["Joined ", new Date(m.joinedAt).toLocaleDateString()] }), m.status === 'pending' ? (_jsxs(Button, { size: "sm", onClick: () => approve(m.id), children: [_jsx(CheckCircle2, { className: "h-4 w-4" }), "Approve"] })) : (_jsx("span", { className: "font-body text-xs text-foreground-secondary", children: "Active storefront" }))] })] }) }) }, m.id))) }), filtered.length === 0 && (_jsxs("p", { className: "font-body text-center text-foreground-secondary", children: ["No merchants match \u201C", query, "\u201D."] }))] }));
}
