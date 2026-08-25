import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { MapPin, ChevronRight } from 'lucide-react';
import { Card, CardContent, Badge } from '@loqal/ui';
import { formatPrice } from '@/lib/format';
import { mockAssignments } from '@/lib/mockAssignments';
const statusMeta = {
    PENDING_PICKUP: { label: 'To pick up', variant: 'secondary' },
    PICKED_UP: { label: 'On the way', variant: 'accent' },
    DELIVERED: { label: 'Delivered', variant: 'success' },
};
export function Assignments() {
    const navigate = useNavigate();
    return (_jsxs("div", { className: "space-y-6", children: [_jsxs("header", { children: [_jsx("h1", { className: "font-display text-3xl font-semibold text-foreground", children: "Assignments" }), _jsxs("p", { className: "font-body text-sm text-foreground-secondary", children: [mockAssignments.length, " deliveries in your queue"] })] }), _jsx("div", { className: "space-y-4", children: mockAssignments.map((a, i) => {
                    const meta = statusMeta[a.status];
                    return (_jsx(motion.div, { initial: { opacity: 0, y: 8 }, animate: { opacity: 1, y: 0 }, transition: { delay: i * 0.05 }, children: _jsx(Card, { role: "button", tabIndex: 0, onClick: () => navigate(`/otp/${a.orderId}`), onKeyDown: (e) => {
                                if (e.key === 'Enter' || e.key === ' ') {
                                    e.preventDefault();
                                    navigate(`/otp/${a.orderId}`);
                                }
                            }, className: "cursor-pointer shadow-sm transition-shadow hover:shadow-md", children: _jsxs(CardContent, { className: "flex items-center gap-4 p-4", children: [_jsxs("div", { className: "min-w-0 flex-1", children: [_jsxs("div", { className: "flex items-center justify-between gap-2", children: [_jsxs("p", { className: "font-body text-xs font-medium text-foreground-secondary", children: ["#", a.orderId] }), _jsx(Badge, { variant: meta.variant, size: "sm", children: meta.label })] }), _jsx("p", { className: "mt-1 truncate font-body font-semibold text-foreground", children: a.customerName }), _jsxs("div", { className: "mt-1 flex items-start gap-1.5 text-foreground-secondary", children: [_jsx(MapPin, { size: 15, className: "mt-0.5 shrink-0 text-accent" }), _jsx("span", { className: "truncate font-body text-xs", children: a.address })] }), _jsx("p", { className: "mt-2 font-display text-lg font-semibold text-foreground", children: formatPrice(a.amountMinor) })] }), _jsx(ChevronRight, { size: 22, className: "shrink-0 text-foreground-muted" })] }) }) }, a.id));
                }) })] }));
}
