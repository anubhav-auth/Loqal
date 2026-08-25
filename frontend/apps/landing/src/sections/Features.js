import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { motion } from 'framer-motion';
import { Card, CardContent } from '@loqal/ui';
const features = [
    {
        title: 'Catalog',
        description: 'Curate a gorgeous storefront with rich imagery, variants, and instant search — no design skills required.',
        image: 'https://images.unsplash.com/photo-1542838132-92c53300491e?w=800&q=80',
    },
    {
        title: 'Orders',
        description: 'Capture, track, and fulfill orders in real time across pickup, delivery, and in-store — all in one place.',
        image: 'https://images.unsplash.com/photo-1556740738-b6a63e27c4df?w=800&q=80',
    },
    {
        title: 'Delivery',
        description: 'Route local deliveries with live status and notifications that keep your customers in the loop.',
        image: 'https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=800&q=80',
    },
];
export default function Features() {
    return (_jsx("section", { id: "features", className: "bg-surface/60 py-24", children: _jsxs("div", { className: "mx-auto max-w-6xl px-4 sm:px-6", children: [_jsxs("div", { className: "mx-auto max-w-2xl text-center", children: [_jsx("p", { className: "text-sm font-medium uppercase tracking-widest text-accent", children: "Everything you need" }), _jsx("h2", { className: "mt-3 font-display text-4xl font-semibold text-foreground sm:text-5xl", children: "One platform for the whole shop" })] }), _jsx("div", { className: "mt-14 grid gap-8 md:grid-cols-3", children: features.map((feature, i) => (_jsx(motion.div, { initial: { opacity: 0, y: 24 }, whileInView: { opacity: 1, y: 0 }, viewport: { once: true, margin: '-80px' }, transition: { duration: 0.5, delay: i * 0.1 }, children: _jsxs(Card, { className: "group h-full overflow-hidden transition-shadow duration-300 hover:shadow-hover", children: [_jsx("div", { className: "overflow-hidden", children: _jsx("img", { src: feature.image, alt: feature.title, className: "h-48 w-full object-cover transition-transform duration-500 group-hover:scale-105" }) }), _jsxs(CardContent, { className: "p-6", children: [_jsx("h3", { className: "font-display text-2xl font-semibold text-foreground", children: feature.title }), _jsx("p", { className: "mt-3 text-foreground-secondary", children: feature.description })] })] }) }, feature.title))) })] }) }));
}
