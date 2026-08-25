import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Link } from 'react-router-dom';
const columns = [
    {
        title: 'Product',
        links: [
            { label: 'Features', href: '#features' },
            { label: 'How It Works', href: '#how-it-works' },
            { label: 'Pricing', href: '#' },
        ],
    },
    {
        title: 'Company',
        links: [
            { label: 'About', href: '#' },
            { label: 'Merchants', href: '#testimonials' },
            { label: 'Careers', href: '#' },
        ],
    },
    {
        title: 'Resources',
        links: [
            { label: 'Help Center', href: '#' },
            { label: 'Blog', href: '#' },
            { label: 'Contact', href: '#' },
        ],
    },
];
export default function Footer() {
    return (_jsxs("footer", { className: "border-t border-border bg-surface/60", children: [_jsxs("div", { className: "mx-auto grid max-w-6xl gap-12 px-4 py-16 sm:px-6 md:grid-cols-[1.5fr_repeat(3,1fr)]", children: [_jsxs("div", { children: [_jsx("p", { className: "font-display text-3xl font-semibold text-foreground", children: "Loqal" }), _jsx("p", { className: "mt-4 max-w-xs text-foreground-secondary", children: "Commerce for local merchants, beautifully simple." })] }), columns.map((col) => (_jsxs("div", { children: [_jsx("h3", { className: "font-medium text-foreground", children: col.title }), _jsx("ul", { className: "mt-4 space-y-3", children: col.links.map((link) => (_jsx("li", { children: _jsx("a", { href: link.href, className: "text-foreground-secondary transition-colors hover:text-foreground", children: link.label }) }, link.label))) })] }, col.title)))] }), _jsx("div", { className: "border-t border-border", children: _jsxs("div", { className: "mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 px-4 py-6 text-sm text-foreground-secondary sm:flex-row sm:px-6", children: [_jsxs("p", { children: ["\u00A9 ", new Date().getFullYear(), " Loqal. All rights reserved."] }), _jsxs("div", { className: "flex gap-6", children: [_jsx(Link, { to: "/login", className: "transition-colors hover:text-foreground", children: "Merchant Login" }), _jsx("a", { href: "#", className: "transition-colors hover:text-foreground", children: "Privacy" }), _jsx("a", { href: "#", className: "transition-colors hover:text-foreground", children: "Terms" })] })] }) })] }));
}
