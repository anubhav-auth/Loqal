import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { motion } from 'framer-motion';
import { LayoutGrid, ShoppingBag, Truck } from 'lucide-react';
import { Card, CardContent } from '@loqal/ui';
const steps = [
    {
        icon: LayoutGrid,
        title: 'Set up your store',
        description: 'Import your catalog, add photos, and publish a storefront in minutes — no code, no designers.',
    },
    {
        icon: ShoppingBag,
        title: 'Take orders',
        description: 'Customers browse and buy through a clean, mobile-first experience with fast, local checkout.',
    },
    {
        icon: Truck,
        title: 'Deliver & grow',
        description: 'Fulfill with integrated delivery and keep shoppers coming back with built-in rewards.',
    },
];
export default function HowItWorks() {
    return (_jsx("section", { id: "how-it-works", className: "py-24", children: _jsxs("div", { className: "mx-auto max-w-6xl px-4 sm:px-6", children: [_jsxs("div", { className: "mx-auto max-w-2xl text-center", children: [_jsx("p", { className: "text-sm font-medium uppercase tracking-widest text-accent", children: "How it works" }), _jsx("h2", { className: "mt-3 font-display text-4xl font-semibold text-foreground sm:text-5xl", children: "Live in three simple steps" })] }), _jsx("div", { className: "mt-14 grid gap-8 md:grid-cols-3", children: steps.map((step, i) => (_jsx(motion.div, { initial: { opacity: 0, y: 24 }, whileInView: { opacity: 1, y: 0 }, viewport: { once: true, margin: '-80px' }, transition: { duration: 0.5, delay: i * 0.1 }, children: _jsx(Card, { className: "h-full bg-accent-light/40", children: _jsxs(CardContent, { className: "p-8", children: [_jsxs("div", { className: "flex items-center gap-4", children: [_jsx("span", { className: "flex h-12 w-12 items-center justify-center rounded-button bg-surface text-accent shadow-sm", children: _jsx(step.icon, { size: 22 }) }), _jsxs("span", { className: "font-display text-3xl font-semibold text-foreground/30", children: ["0", i + 1] })] }), _jsx("h3", { className: "mt-6 font-display text-2xl font-semibold text-foreground", children: step.title }), _jsx("p", { className: "mt-3 text-foreground-secondary", children: step.description })] }) }) }, step.title))) })] }) }));
}
