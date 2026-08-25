import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Link } from 'react-router-dom';
import { Card, CardContent } from '@loqal/ui';
import { Plus } from 'lucide-react';
import { Button } from '@loqal/ui';
import { formatPrice } from '@/lib/format';
import { useCartStore } from '@/store/cartStore';
export function ProductCard({ product }) {
    const addItem = useCartStore((s) => s.addItem);
    const image = product.imageUrls?.[0] ?? 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=600';
    return (_jsxs(Card, { className: "group flex flex-col overflow-hidden transition-shadow hover:shadow-hover", children: [_jsx(Link, { to: `/product/${product.id}`, className: "block overflow-hidden", children: _jsx("img", { src: image, alt: product.name, loading: "lazy", className: "aspect-[4/3] w-full object-cover transition-transform duration-500 group-hover:scale-105" }) }), _jsxs(CardContent, { className: "flex flex-1 flex-col gap-3 p-4", children: [_jsxs("div", { className: "flex items-start justify-between gap-2", children: [_jsx(Link, { to: `/product/${product.id}`, children: _jsx("h3", { className: "font-display text-xl font-semibold leading-snug text-foreground", children: product.name }) }), _jsx("span", { className: "shrink-0 font-display text-lg text-accent", children: formatPrice(product.priceMinor) })] }), product.description && (_jsx("p", { className: "line-clamp-2 font-body text-sm text-foreground-secondary", children: product.description })), _jsxs(Button, { size: "sm", className: "mt-auto w-full", onClick: () => addItem({
                            productId: product.id,
                            name: product.name,
                            priceMinor: product.priceMinor,
                            imageUrl: image,
                        }), children: [_jsx(Plus, { className: "h-4 w-4" }), " Add to cart"] })] })] }));
}
