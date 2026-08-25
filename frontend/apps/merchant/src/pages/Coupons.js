import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Plus } from 'lucide-react';
import { Card, CardContent, Button, Input, Badge, Dialog, DialogTrigger, DialogContent, DialogHeader, DialogTitle, DialogDescription, } from '@loqal/ui';
import { formatPrice } from '@/lib/format';
const SEED_COUPONS = [
    {
        id: 'cpn_1',
        code: 'WELCOME10',
        discountType: 'PERCENT',
        value: 10,
        validFrom: '2026-01-01',
        validUntil: '2026-12-31',
        active: true,
    },
    {
        id: 'cpn_2',
        code: 'FLAT50',
        discountType: 'FLAT',
        value: 5000,
        minOrderValueMinor: 20000,
        validFrom: '2026-01-01',
        validUntil: '2026-06-30',
        active: true,
    },
    {
        id: 'cpn_3',
        code: 'EXPIRED20',
        discountType: 'PERCENT',
        value: 20,
        validFrom: '2025-01-01',
        validUntil: '2025-12-31',
        active: false,
    },
];
const EMPTY = {
    code: '',
    discountType: 'PERCENT',
    value: '',
    validFrom: '',
    validUntil: '',
};
function describe(c) {
    return c.discountType === 'PERCENT'
        ? `${c.value}% off`
        : `${formatPrice(c.value)} off`;
}
export function Coupons() {
    const [coupons, setCoupons] = useState(SEED_COUPONS);
    const [open, setOpen] = useState(false);
    const [form, setForm] = useState(EMPTY);
    function submit() {
        const created = {
            id: `cpn_${Date.now()}`,
            code: form.code.toUpperCase(),
            discountType: form.discountType.toUpperCase(),
            value: Number(form.value) || 0,
            validFrom: form.validFrom,
            validUntil: form.validUntil,
            active: true,
        };
        setCoupons((prev) => [created, ...prev]);
        setOpen(false);
        setForm(EMPTY);
    }
    return (_jsxs("div", { className: "space-y-8", children: [_jsxs("header", { className: "flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between", children: [_jsxs("div", { children: [_jsx("h1", { className: "font-display text-4xl font-semibold text-foreground", children: "Coupons" }), _jsx("p", { className: "mt-1 font-body text-foreground-secondary", children: "Create and manage discount codes for your customers." })] }), _jsxs(Dialog, { open: open, onOpenChange: setOpen, children: [_jsx(DialogTrigger, { asChild: true, children: _jsxs(Button, { children: [_jsx(Plus, { className: "h-4 w-4" }), " Create Coupon"] }) }), _jsxs(DialogContent, { children: [_jsxs(DialogHeader, { children: [_jsx(DialogTitle, { children: "Create Coupon" }), _jsx(DialogDescription, { children: "Define a new discount code for your storefront." })] }), _jsxs("div", { className: "space-y-4", children: [_jsx(Field, { label: "Code", children: _jsx(Input, { value: form.code, onChange: (e) => setForm({ ...form, code: e.target.value }), placeholder: "SUMMER25" }) }), _jsxs("div", { className: "grid grid-cols-2 gap-4", children: [_jsx(Field, { label: "Discount type (PERCENT/FLAT)", children: _jsx(Input, { value: form.discountType, onChange: (e) => setForm({ ...form, discountType: e.target.value }), placeholder: "PERCENT" }) }), _jsx(Field, { label: "Value", children: _jsx(Input, { type: "number", value: form.value, onChange: (e) => setForm({ ...form, value: e.target.value }), placeholder: "10" }) })] }), _jsxs("div", { className: "grid grid-cols-2 gap-4", children: [_jsx(Field, { label: "Valid from", children: _jsx(Input, { type: "date", value: form.validFrom, onChange: (e) => setForm({ ...form, validFrom: e.target.value }) }) }), _jsx(Field, { label: "Valid until", children: _jsx(Input, { type: "date", value: form.validUntil, onChange: (e) => setForm({ ...form, validUntil: e.target.value }) }) })] })] }), _jsxs("div", { className: "flex flex-col-reverse gap-2 sm:flex-row sm:justify-end sm:space-x-2", children: [_jsx(Button, { variant: "outline", onClick: () => setOpen(false), children: "Cancel" }), _jsx(Button, { onClick: submit, children: "Create" })] })] })] })] }), _jsx("div", { className: "grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3", children: _jsx(AnimatePresence, { children: coupons.map((c) => (_jsx(motion.div, { layout: true, initial: { opacity: 0, y: 12 }, animate: { opacity: 1, y: 0 }, exit: { opacity: 0 }, children: _jsx(Card, { className: "shadow-sm", children: _jsxs(CardContent, { className: "space-y-3 p-5", children: [_jsxs("div", { className: "flex items-center justify-between", children: [_jsx("span", { className: "font-display text-2xl font-semibold text-foreground", children: c.code }), _jsx(Badge, { variant: c.active ? 'success' : 'error', children: c.active ? 'Active' : 'Inactive' })] }), _jsx("p", { className: "font-body text-sm text-accent", children: describe(c) }), _jsxs("p", { className: "font-body text-xs text-foreground-secondary", children: [c.validFrom, " \u2192 ", c.validUntil] })] }) }) }, c.id))) }) })] }));
}
function Field({ label, children, }) {
    return (_jsxs("label", { className: "block space-y-1", children: [_jsx("span", { className: "font-body text-sm text-foreground-secondary", children: label }), children] }));
}
