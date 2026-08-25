import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Minus, Plus, Trash2 } from 'lucide-react';
import { Button } from '@loqal/ui';
import { formatPrice } from '@/lib/format';
import { useCartStore } from '@/store/cartStore';
export function CartItem({ item }) {
    const updateQty = useCartStore((s) => s.updateQty);
    const removeItem = useCartStore((s) => s.removeItem);
    return (_jsxs("div", { className: "flex gap-4 border-b border-border py-4", children: [_jsx("img", { src: item.imageUrl, alt: item.name, className: "h-20 w-20 shrink-0 rounded-card object-cover" }), _jsxs("div", { className: "flex flex-1 flex-col", children: [_jsxs("div", { className: "flex items-start justify-between gap-2", children: [_jsx("h3", { className: "font-display text-lg font-semibold text-foreground", children: item.name }), _jsx("span", { className: "font-display text-lg text-accent", children: formatPrice(item.priceMinor * item.quantity) })] }), _jsxs("div", { className: "mt-auto flex items-center justify-between pt-2", children: [_jsxs("div", { className: "flex items-center gap-2", children: [_jsx(Button, { variant: "outline", size: "icon", className: "h-8 w-8", "aria-label": "Decrease quantity", onClick: () => updateQty(item.productId, item.quantity - 1), disabled: item.quantity <= 1, children: _jsx(Minus, { className: "h-4 w-4" }) }), _jsx("span", { className: "w-8 text-center font-body text-sm", children: item.quantity }), _jsx(Button, { variant: "outline", size: "icon", className: "h-8 w-8", "aria-label": "Increase quantity", onClick: () => updateQty(item.productId, item.quantity + 1), children: _jsx(Plus, { className: "h-4 w-4" }) })] }), _jsxs(Button, { variant: "ghost", size: "sm", className: "text-error hover:bg-error/10", onClick: () => removeItem(item.productId), children: [_jsx(Trash2, { className: "h-4 w-4" }), " Remove"] })] })] })] }));
}
