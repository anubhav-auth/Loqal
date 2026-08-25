import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Menu, X } from 'lucide-react';
import { Button } from '@loqal/ui';
const links = [
    { label: 'Features', href: '#features' },
    { label: 'How It Works', href: '#how-it-works' },
    { label: 'Merchants', href: '#testimonials' },
];
export default function Navbar() {
    const [open, setOpen] = useState(false);
    return (_jsxs("header", { className: "fixed inset-x-0 top-0 z-50 border-b border-border/60 bg-background/70 backdrop-blur-md", children: [_jsxs("nav", { className: "mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6", children: [_jsx(Link, { to: "/", className: "font-display text-2xl font-semibold tracking-tight text-foreground", children: "Loqal" }), _jsxs("div", { className: "hidden items-center gap-8 md:flex", children: [links.map((link) => (_jsx("a", { href: link.href, className: "text-sm font-medium text-foreground-secondary transition-colors hover:text-foreground", children: link.label }, link.href))), _jsx(Button, { asChild: true, size: "sm", children: _jsx(Link, { to: "/login", children: "Get Started" }) })] }), _jsx("button", { type: "button", "aria-label": "Toggle menu", className: "text-foreground md:hidden", onClick: () => setOpen((v) => !v), children: open ? _jsx(X, { size: 22 }) : _jsx(Menu, { size: 22 }) })] }), _jsx(AnimatePresence, { children: open && (_jsx(motion.div, { initial: { height: 0, opacity: 0 }, animate: { height: 'auto', opacity: 1 }, exit: { height: 0, opacity: 0 }, transition: { duration: 0.2 }, className: "overflow-hidden border-t border-border/60 bg-background/95 md:hidden", children: _jsxs("div", { className: "flex flex-col gap-4 px-4 py-6", children: [links.map((link) => (_jsx("a", { href: link.href, onClick: () => setOpen(false), className: "text-base font-medium text-foreground-secondary transition-colors hover:text-foreground", children: link.label }, link.href))), _jsx(Button, { asChild: true, className: "mt-2", children: _jsx(Link, { to: "/login", onClick: () => setOpen(false), children: "Get Started" }) })] }) })) })] }));
}
