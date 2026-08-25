import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { MapPin, CheckCircle2, ShieldCheck } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription, Button, Input, Badge, useToast, } from '@loqal/ui';
import { formatPrice } from '@/lib/format';
import { mockAssignments } from '@/lib/mockAssignments';
export function OtpVerification() {
    const { orderId } = useParams();
    const navigate = useNavigate();
    const { toast } = useToast();
    const assignment = mockAssignments.find((a) => a.orderId === orderId);
    const [status, setStatus] = useState(assignment?.status ?? 'PENDING_PICKUP');
    const [pickupInput, setPickupInput] = useState('');
    const [deliveryInput, setDeliveryInput] = useState('');
    const [error, setError] = useState('');
    if (!assignment) {
        return (_jsxs("div", { className: "space-y-4 pt-10 text-center", children: [_jsx("p", { className: "font-body text-foreground-secondary", children: "Assignment not found." }), _jsx(Button, { variant: "secondary", onClick: () => navigate('/assignments'), children: "Back to tasks" })] }));
    }
    const verifyPickup = () => {
        setError('');
        if (pickupInput.trim() !== assignment.pickupOtp) {
            setError('Incorrect pickup OTP. Please check with the merchant.');
            return;
        }
        setStatus('PICKED_UP');
        setPickupInput('');
        toast('Pickup confirmed — on your way!', 'success');
    };
    const verifyDelivery = () => {
        setError('');
        if (deliveryInput.trim() !== assignment.deliveryOtp) {
            setError('Incorrect delivery OTP. Please check with the customer.');
            return;
        }
        setStatus('DELIVERED');
        setDeliveryInput('');
        toast('Delivery completed. Great job!', 'success');
    };
    const stage = status === 'PENDING_PICKUP'
        ? 'pickup'
        : status === 'PICKED_UP'
            ? 'delivery'
            : 'done';
    return (_jsxs("div", { className: "space-y-6 pt-2", children: [_jsx("button", { onClick: () => navigate(-1), className: "font-body text-sm text-foreground-secondary", children: "\u2190 Back" }), _jsxs(Card, { className: "shadow-sm", children: [_jsxs(CardHeader, { children: [_jsxs("div", { className: "flex items-center justify-between", children: [_jsxs(CardTitle, { className: "font-display text-xl", children: ["#", assignment.orderId] }), _jsx(Badge, { variant: stage === 'done' ? 'success' : stage === 'delivery' ? 'accent' : 'secondary', size: "sm", children: stage === 'done'
                                            ? 'Delivered'
                                            : stage === 'delivery'
                                                ? 'On the way'
                                                : 'To pick up' })] }), _jsx(CardDescription, { className: "font-body", children: assignment.customerName })] }), _jsxs(CardContent, { className: "space-y-3", children: [_jsxs("div", { className: "flex items-start gap-2 text-foreground-secondary", children: [_jsx(MapPin, { size: 18, className: "mt-0.5 shrink-0 text-accent" }), _jsx("span", { className: "font-body text-sm", children: assignment.address })] }), _jsx("p", { className: "font-display text-2xl font-semibold text-foreground", children: formatPrice(assignment.amountMinor) })] })] }), stage !== 'done' && (_jsxs(motion.div, { initial: { opacity: 0, y: 8 }, animate: { opacity: 1, y: 0 }, className: "space-y-4", children: [stage === 'pickup' && (_jsxs(Card, { className: "shadow-sm", children: [_jsxs(CardHeader, { className: "flex-row items-center gap-3 space-y-0", children: [_jsx(ShieldCheck, { size: 20, className: "text-accent" }), _jsx(CardTitle, { className: "font-display text-lg", children: "Pickup OTP" })] }), _jsxs(CardContent, { className: "space-y-3", children: [_jsx(Input, { inputMode: "numeric", maxLength: 4, placeholder: "Enter 4-digit code", value: pickupInput, onChange: (e) => setPickupInput(e.target.value), className: "py-4 text-center text-2xl tracking-[0.5em]" }), _jsx(Button, { className: "w-full py-4 text-base", onClick: verifyPickup, children: "Confirm Pickup" })] })] })), stage === 'delivery' && (_jsxs(Card, { className: "shadow-sm", children: [_jsxs(CardHeader, { className: "flex-row items-center gap-3 space-y-0", children: [_jsx(CheckCircle2, { size: 20, className: "text-accent" }), _jsx(CardTitle, { className: "font-display text-lg", children: "Delivery OTP" })] }), _jsxs(CardContent, { className: "space-y-3", children: [_jsx(Input, { inputMode: "numeric", maxLength: 4, placeholder: "Enter 4-digit code", value: deliveryInput, onChange: (e) => setDeliveryInput(e.target.value), className: "py-4 text-center text-2xl tracking-[0.5em]" }), _jsx(Button, { variant: "default", className: "w-full bg-accent py-4 text-base text-white hover:bg-accent/90", onClick: verifyDelivery, children: "Confirm Delivery" })] })] })), error && (_jsx("p", { className: "text-center font-body text-sm text-error", children: error })), stage === 'pickup' && (_jsxs("p", { className: "text-center font-body text-xs text-foreground-muted", children: ["MOCK OTP (demo): ", assignment.pickupOtp] })), stage === 'delivery' && (_jsxs("p", { className: "text-center font-body text-xs text-foreground-muted", children: ["MOCK OTP (demo): ", assignment.deliveryOtp] }))] })), stage === 'done' && (_jsxs("div", { className: "flex flex-col items-center gap-3 rounded-card border border-success/30 bg-success/5 py-10 text-center", children: [_jsx(CheckCircle2, { size: 40, className: "text-success" }), _jsx("p", { className: "font-display text-2xl font-semibold text-foreground", children: "Delivered!" }), _jsx(Button, { variant: "secondary", onClick: () => navigate('/assignments'), children: "Back to tasks" })] }))] }));
}
