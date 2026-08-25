import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { motion } from 'framer-motion';
import { Store, Save } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent, Button, Input, Avatar, AvatarImage, AvatarFallback, useToast, } from '@loqal/ui';
const INITIAL = {
    storeName: 'Sunrise Handicrafts',
    description: 'Locally made crafts delivered to your doorstep.',
    logoUrl: '',
    street: '12 Market Road',
    city: 'Jaipur',
    state: 'Rajasthan',
    postalCode: '302001',
    country: 'India',
};
export function Profile() {
    const { toast } = useToast();
    const [form, setForm] = useState(INITIAL);
    function set(key, value) {
        setForm((prev) => ({ ...prev, [key]: value }));
    }
    function save() {
        toast('Storefront settings saved', 'success');
    }
    return (_jsxs("div", { className: "space-y-8", children: [_jsxs("header", { children: [_jsx("h1", { className: "font-display text-4xl font-semibold text-foreground", children: "Profile" }), _jsx("p", { className: "mt-1 font-body text-foreground-secondary", children: "Customize how your storefront appears to customers." })] }), _jsx(motion.div, { initial: { opacity: 0, y: 12 }, animate: { opacity: 1, y: 0 }, children: _jsxs(Card, { className: "shadow-sm", children: [_jsx(CardHeader, { children: _jsxs("div", { className: "flex items-center gap-4", children: [_jsxs(Avatar, { className: "h-16 w-16", children: [form.logoUrl ? (_jsx(AvatarImage, { src: form.logoUrl, alt: form.storeName })) : null, _jsx(AvatarFallback, { children: _jsx(Store, { className: "h-7 w-7" }) })] }), _jsx(CardTitle, { className: "font-display text-2xl", children: form.storeName || 'Your Store' })] }) }), _jsxs(CardContent, { className: "space-y-6", children: [_jsxs("div", { className: "grid grid-cols-1 gap-4 sm:grid-cols-2", children: [_jsx(Field, { label: "Store name", children: _jsx(Input, { value: form.storeName, onChange: (e) => set('storeName', e.target.value) }) }), _jsx(Field, { label: "Logo URL", children: _jsx(Input, { value: form.logoUrl, onChange: (e) => set('logoUrl', e.target.value), placeholder: "https://.../logo.png" }) })] }), _jsx(Field, { label: "Description", children: _jsx(Input, { value: form.description, onChange: (e) => set('description', e.target.value) }) }), _jsxs("div", { className: "grid grid-cols-1 gap-4 sm:grid-cols-2", children: [_jsx(Field, { label: "Street", children: _jsx(Input, { value: form.street, onChange: (e) => set('street', e.target.value) }) }), _jsx(Field, { label: "City", children: _jsx(Input, { value: form.city, onChange: (e) => set('city', e.target.value) }) }), _jsx(Field, { label: "State", children: _jsx(Input, { value: form.state, onChange: (e) => set('state', e.target.value) }) }), _jsx(Field, { label: "Postal code", children: _jsx(Input, { value: form.postalCode, onChange: (e) => set('postalCode', e.target.value) }) }), _jsx(Field, { label: "Country", children: _jsx(Input, { value: form.country, onChange: (e) => set('country', e.target.value) }) })] }), _jsx("div", { className: "flex justify-end", children: _jsxs(Button, { onClick: save, children: [_jsx(Save, { className: "h-4 w-4" }), " Save Changes"] }) })] })] }) })] }));
}
function Field({ label, children, }) {
    return (_jsxs("label", { className: "block space-y-1", children: [_jsx("span", { className: "font-body text-sm text-foreground-secondary", children: label }), children] }));
}
