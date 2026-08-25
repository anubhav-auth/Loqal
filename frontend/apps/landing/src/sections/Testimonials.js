import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { motion } from 'framer-motion';
import { Card, CardContent, Avatar, AvatarImage, AvatarFallback } from '@loqal/ui';
const testimonials = [
    {
        quote: 'Loqal gave my bakery a storefront that actually feels like us. Orders tripled in the first month.',
        name: 'Amara N.',
        role: 'Owner, Saffron & Crumb',
        image: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&q=80',
    },
    {
        quote: 'The delivery tracking alone saved us hours every week. Our regulars love the experience.',
        name: 'Diego R.',
        role: 'Founder, Verde Market',
        image: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&q=80',
    },
    {
        quote: 'Finally a platform built for small merchants. Simple, beautiful, and it just works.',
        name: 'Priya S.',
        role: 'Co-owner, The Daily Grain',
        image: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&q=80',
    },
];
export default function Testimonials() {
    return (_jsx("section", { id: "testimonials", className: "bg-surface/60 py-24", children: _jsxs("div", { className: "mx-auto max-w-6xl px-4 sm:px-6", children: [_jsxs("div", { className: "mx-auto max-w-2xl text-center", children: [_jsx("p", { className: "text-sm font-medium uppercase tracking-widest text-accent", children: "Loved by merchants" }), _jsx("h2", { className: "mt-3 font-display text-4xl font-semibold text-foreground sm:text-5xl", children: "Stories from the neighborhood" })] }), _jsx("div", { className: "mt-14 grid gap-8 md:grid-cols-3", children: testimonials.map((t, i) => (_jsx(motion.div, { initial: { opacity: 0, y: 24 }, whileInView: { opacity: 1, y: 0 }, viewport: { once: true, margin: '-80px' }, transition: { duration: 0.5, delay: i * 0.1 }, children: _jsx(Card, { className: "h-full", children: _jsxs(CardContent, { className: "flex h-full flex-col p-8", children: [_jsxs("p", { className: "flex-1 font-display text-xl leading-relaxed text-foreground", children: ["\u201C", t.quote, "\u201D"] }), _jsxs("div", { className: "mt-6 flex items-center gap-4", children: [_jsxs(Avatar, { children: [_jsx(AvatarImage, { src: t.image, alt: t.name }), _jsx(AvatarFallback, { children: t.name.charAt(0) })] }), _jsxs("div", { children: [_jsx("p", { className: "font-medium text-foreground", children: t.name }), _jsx("p", { className: "text-sm text-foreground-secondary", children: t.role })] })] })] }) }) }, t.name))) })] }) }));
}
