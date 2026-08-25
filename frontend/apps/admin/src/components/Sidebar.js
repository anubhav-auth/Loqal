import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Store, Users, ScrollText, BarChart3, Menu, X } from 'lucide-react';
import { cn } from '@loqal/ui';
const NAV_ITEMS = [
    { to: '/merchants', label: 'Merchants', icon: Users },
    { to: '/audit', label: 'Audit Trail', icon: ScrollText },
    { to: '/metrics', label: 'Metrics', icon: BarChart3 },
];
export function Sidebar() {
    const [open, setOpen] = useState(false);
    return (_jsxs(_Fragment, { children: [_jsx("button", { type: "button", onClick: () => setOpen((v) => !v), className: "fixed left-4 top-4 z-50 flex h-10 w-10 items-center justify-center rounded-button border border-border bg-surface text-foreground shadow-sm lg:hidden", "aria-label": "Toggle navigation", children: open ? _jsx(X, { className: "h-5 w-5" }) : _jsx(Menu, { className: "h-5 w-5" }) }), _jsx(AnimatePresence, { children: open && (_jsx(motion.div, { className: "fixed inset-0 z-40 bg-foreground/40 lg:hidden", initial: { opacity: 0 }, animate: { opacity: 1 }, exit: { opacity: 0 }, onClick: () => setOpen(false) })) }), _jsxs("aside", { className: cn('fixed inset-y-0 left-0 z-40 w-64 transform border-r border-border bg-surface transition-transform duration-300', 'lg:static lg:translate-x-0', open ? 'translate-x-0' : '-translate-x-full'), children: [_jsxs("div", { className: "flex items-center gap-2 px-6 py-6", children: [_jsx("span", { className: "flex h-9 w-9 items-center justify-center rounded-button bg-accent-light text-accent", children: _jsx(Store, { className: "h-5 w-5" }) }), _jsx("span", { className: "font-display text-2xl font-semibold text-foreground", children: "Loqal" }), _jsx("span", { className: "ml-1 rounded-button bg-accent-light px-2 py-0.5 font-body text-[10px] font-medium uppercase tracking-wide text-accent", children: "Admin" })] }), _jsx("nav", { className: "flex flex-col gap-1 px-3", children: NAV_ITEMS.map(({ to, label, icon: Icon }) => (_jsxs(NavLink, { to: to, onClick: () => setOpen(false), className: ({ isActive }) => cn('flex items-center gap-3 rounded-card px-4 py-3 font-body text-sm transition-colors', isActive
                                ? 'bg-accent-light text-accent'
                                : 'text-foreground-secondary hover:bg-accent-light/60 hover:text-foreground'), children: [_jsx(Icon, { className: "h-5 w-5" }), label] }, to))) })] })] }));
}
