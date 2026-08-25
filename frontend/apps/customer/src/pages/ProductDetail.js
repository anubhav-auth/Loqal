import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { productsApi } from '@loqal/api-client';
import { Button, Skeleton } from '@loqal/ui';
import { Minus, Plus } from 'lucide-react';
import { formatPrice } from '@/lib/format';
import { useCartStore } from '@/store/cartStore';
export function ProductDetail() {
    const { id } = useParams();
    const [qty, setQty] = useState(1);
    const [activeImage, setActiveImage] = useState(0);
    const addItem = useCartStore((s) => s.addItem);
    const { data: product, isLoading, isError } = useQuery({
        queryKey: ['product', id],
        queryFn: () => productsApi.getById(id),
        enabled: !!id,
    });
    if (isLoading) {
        return (_jsx("div", { className: "mx-auto max-w-5xl px-4 py-10", children: _jsx(Skeleton, { className: "aspect-[16/9] w-full rounded-card" }) }));
    }
    if (isError || !product) {
        return (_jsxs("div", { className: "mx-auto max-w-5xl px-4 py-20 text-center", children: [_jsx("p", { className: "font-body text-error", children: "Product not found." }), _jsx(Button, { asChild: true, variant: "link", children: _jsx(Link, { to: "/", children: "Back to store" }) })] }));
    }
    const images = product.imageUrls.length > 0 ? product.imageUrls : [];
    return (_jsxs("div", { className: "mx-auto max-w-5xl px-4 py-10 sm:px-6", children: [_jsx(Link, { to: "/", className: "font-body text-sm text-foreground-secondary hover:text-accent", children: "\u2190 Back to store" }), _jsxs("div", { className: "mt-6 grid gap-8 md:grid-cols-2", children: [_jsxs("div", { children: [_jsx("img", { src: images[activeImage] ?? 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800', alt: product.name, className: "aspect-[4/3] w-full rounded-card object-cover" }), images.length > 1 && (_jsx("div", { className: "mt-4 flex gap-3", children: images.map((img, i) => (_jsx("button", { onClick: () => setActiveImage(i), className: `h-16 w-16 overflow-hidden rounded-card border-2 ${i === activeImage ? 'border-accent' : 'border-border'}`, children: _jsx("img", { src: img, alt: "", className: "h-full w-full object-cover" }) }, i))) }))] }), _jsxs("div", { className: "flex flex-col", children: [_jsx("h1", { className: "font-display text-4xl font-semibold text-foreground", children: product.name }), _jsx("p", { className: "mt-2 font-display text-2xl text-accent", children: formatPrice(product.priceMinor) }), product.description && (_jsx("p", { className: "mt-4 font-body leading-relaxed text-foreground-secondary", children: product.description })), _jsxs("div", { className: "mt-8 flex items-center gap-4", children: [_jsxs("div", { className: "flex items-center gap-2", children: [_jsx(Button, { variant: "outline", size: "icon", onClick: () => setQty(Math.max(1, qty - 1)), "aria-label": "Decrease", children: _jsx(Minus, { className: "h-4 w-4" }) }), _jsx("span", { className: "w-8 text-center font-body", children: qty }), _jsx(Button, { variant: "outline", size: "icon", onClick: () => setQty(qty + 1), "aria-label": "Increase", children: _jsx(Plus, { className: "h-4 w-4" }) })] }), _jsx(Button, { className: "flex-1", onClick: () => addItem({
                                            productId: product.id,
                                            name: product.name,
                                            priceMinor: product.priceMinor,
                                            imageUrl: images[0] ?? 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=600',
                                        }, qty), children: "Add to cart" })] })] })] })] }));
}
