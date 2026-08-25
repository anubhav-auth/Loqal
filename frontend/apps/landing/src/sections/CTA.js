import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Button } from '@loqal/ui';
export default function CTA() {
    return (_jsx("section", { className: "py-24", children: _jsx("div", { className: "mx-auto max-w-6xl px-4 sm:px-6", children: _jsxs(motion.div, { initial: { opacity: 0, y: 24 }, whileInView: { opacity: 1, y: 0 }, viewport: { once: true, margin: '-80px' }, transition: { duration: 0.6 }, className: "rounded-card bg-accent-light px-6 py-16 text-center sm:px-12", children: [_jsx("h2", { className: "mx-auto max-w-2xl font-display text-4xl font-semibold text-foreground sm:text-5xl", children: "Open your storefront today" }), _jsx("p", { className: "mx-auto mt-5 max-w-xl text-lg text-foreground-secondary", children: "Join the local merchants building loyal communities with Loqal. Free to start \u2014 no credit card required." }), _jsx("div", { className: "mt-8 flex justify-center", children: _jsx(Button, { asChild: true, size: "lg", children: _jsx(Link, { to: "/signup", children: "Get Started" }) }) })] }) }) }));
}
