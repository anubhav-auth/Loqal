import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { Plus, Pencil, Trash2, ImageOff } from 'lucide-react';
import { Card, CardContent, Button, Input, Badge, Dialog, DialogTrigger, DialogContent, DialogHeader, DialogTitle, DialogDescription, Skeleton, } from '@loqal/ui';
import { productsApi } from '@loqal/api-client';
import { formatPrice } from '@/lib/format';
import { MERCHANT_ID } from '@/lib/constants';
const EMPTY_FORM = {
    name: '',
    description: '',
    priceMinor: '',
    quantity: '',
    imageUrls: '',
};
export function Catalog() {
    const queryClient = useQueryClient();
    const [open, setOpen] = useState(false);
    const [editing, setEditing] = useState(null);
    const [form, setForm] = useState(EMPTY_FORM);
    const { data: products, isLoading } = useQuery({
        queryKey: ['products', MERCHANT_ID],
        queryFn: () => productsApi.getByMerchant(MERCHANT_ID),
    });
    const createMutation = useMutation({
        mutationFn: (data) => productsApi.create(MERCHANT_ID, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['products', MERCHANT_ID] });
            closeForm();
        },
    });
    const updateMutation = useMutation({
        mutationFn: (data) => productsApi.update(data.id, MERCHANT_ID, data.patch),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['products', MERCHANT_ID] });
            closeForm();
        },
    });
    const deleteMutation = useMutation({
        mutationFn: (id) => productsApi.delete(id, MERCHANT_ID),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ['products', MERCHANT_ID] }),
    });
    function openCreate() {
        setEditing(null);
        setForm(EMPTY_FORM);
        setOpen(true);
    }
    function openEdit(p) {
        setEditing(p);
        setForm({
            name: p.name,
            description: p.description ?? '',
            priceMinor: String(p.priceMinor),
            quantity: String(p.quantity),
            imageUrls: p.imageUrls.join(', '),
        });
        setOpen(true);
    }
    function closeForm() {
        setOpen(false);
        setEditing(null);
        setForm(EMPTY_FORM);
    }
    function submit() {
        const payload = {
            name: form.name,
            description: form.description || undefined,
            priceMinor: Number(form.priceMinor) || 0,
            quantity: Number(form.quantity) || 0,
            imageUrls: form.imageUrls
                .split(',')
                .map((s) => s.trim())
                .filter(Boolean),
        };
        if (editing) {
            updateMutation.mutate({ id: editing.id, patch: payload });
        }
        else {
            createMutation.mutate(payload);
        }
    }
    return (_jsxs("div", { className: "space-y-8", children: [_jsxs("header", { className: "flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between", children: [_jsxs("div", { children: [_jsx("h1", { className: "font-display text-4xl font-semibold text-foreground", children: "Catalog" }), _jsx("p", { className: "mt-1 font-body text-foreground-secondary", children: "Manage the products available in your storefront." })] }), _jsxs(Dialog, { open: open, onOpenChange: setOpen, children: [_jsx(DialogTrigger, { asChild: true, children: _jsxs(Button, { onClick: openCreate, children: [_jsx(Plus, { className: "h-4 w-4" }), " Add Product"] }) }), _jsxs(DialogContent, { children: [_jsxs(DialogHeader, { children: [_jsx(DialogTitle, { children: editing ? 'Edit Product' : 'Add Product' }), _jsxs(DialogDescription, { children: ["Fill in the details below to", ' ', editing ? 'update' : 'create', " a product."] })] }), _jsxs("div", { className: "space-y-4", children: [_jsx(Field, { label: "Name", children: _jsx(Input, { value: form.name, onChange: (e) => setForm({ ...form, name: e.target.value }), placeholder: "Handwoven Basket" }) }), _jsx(Field, { label: "Description", children: _jsx(Input, { value: form.description, onChange: (e) => setForm({ ...form, description: e.target.value }), placeholder: "Short product description" }) }), _jsxs("div", { className: "grid grid-cols-2 gap-4", children: [_jsx(Field, { label: "Price (\u20B9 minor units)", children: _jsx(Input, { type: "number", value: form.priceMinor, onChange: (e) => setForm({ ...form, priceMinor: e.target.value }), placeholder: "12000" }) }), _jsx(Field, { label: "Quantity", children: _jsx(Input, { type: "number", value: form.quantity, onChange: (e) => setForm({ ...form, quantity: e.target.value }), placeholder: "25" }) })] }), _jsx(Field, { label: "Image URLs (comma separated)", children: _jsx(Input, { value: form.imageUrls, onChange: (e) => setForm({ ...form, imageUrls: e.target.value }), placeholder: "https://.../a.jpg, https://.../b.jpg" }) })] }), _jsxs("div", { className: "flex flex-col-reverse gap-2 sm:flex-row sm:justify-end sm:space-x-2", children: [_jsx(Button, { variant: "outline", onClick: closeForm, children: "Cancel" }), _jsx(Button, { onClick: submit, children: editing ? 'Save Changes' : 'Create' })] })] })] })] }), isLoading ? (_jsx("div", { className: "grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3", children: Array.from({ length: 6 }).map((_, i) => (_jsx(Skeleton, { className: "h-72 w-full rounded-card" }, i))) })) : products && products.length > 0 ? (_jsx("div", { className: "grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3", children: _jsx(AnimatePresence, { children: products.map((p) => (_jsx(motion.div, { layout: true, initial: { opacity: 0, y: 12 }, animate: { opacity: 1, y: 0 }, exit: { opacity: 0 }, children: _jsxs(Card, { className: "overflow-hidden shadow-sm", children: [_jsx("div", { className: "relative h-40 w-full bg-accent-light", children: p.imageUrls[0] ? (_jsx("img", { src: p.imageUrls[0], alt: p.name, className: "h-full w-full object-cover" })) : (_jsx("div", { className: "flex h-full w-full items-center justify-center text-accent", children: _jsx(ImageOff, { className: "h-8 w-8" }) })) }), _jsxs(CardContent, { className: "space-y-2 p-4", children: [_jsxs("div", { className: "flex items-start justify-between gap-2", children: [_jsx("h3", { className: "font-display text-xl font-semibold text-foreground", children: p.name }), _jsx(Badge, { variant: p.quantity > 0 ? 'accent' : 'error', children: p.quantity > 0 ? `${p.quantity} left` : 'Out' })] }), _jsx("p", { className: "font-body text-sm text-foreground-secondary", children: p.description }), _jsx("p", { className: "font-display text-2xl font-semibold text-accent", children: formatPrice(p.priceMinor) }), _jsxs("div", { className: "flex gap-2 pt-2", children: [_jsxs(Button, { size: "sm", variant: "outline", onClick: () => openEdit(p), children: [_jsx(Pencil, { className: "h-4 w-4" }), " Edit"] }), _jsxs(Button, { size: "sm", variant: "ghost", onClick: () => deleteMutation.mutate(p.id), children: [_jsx(Trash2, { className: "h-4 w-4" }), " Delete"] })] })] })] }) }, p.id))) }) })) : (_jsx("div", { className: "rounded-card border border-dashed border-border p-12 text-center", children: _jsx("p", { className: "font-body text-foreground-secondary", children: "No products yet. Click \u201CAdd Product\u201D to get started." }) }))] }));
}
function Field({ label, children, }) {
    return (_jsxs("label", { className: "block space-y-1", children: [_jsx("span", { className: "font-body text-sm text-foreground-secondary", children: label }), children] }));
}
