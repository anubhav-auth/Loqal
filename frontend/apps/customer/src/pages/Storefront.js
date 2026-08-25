import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useQuery } from '@tanstack/react-query';
import { productsApi } from '@loqal/api-client';
import { Skeleton } from '@loqal/ui';
import { ProductCard } from '@/components/ProductCard';
import { MERCHANT_ID } from '@/lib/constants';
export function Storefront() {
    const { data: products, isLoading, isError } = useQuery({
        queryKey: ['products', MERCHANT_ID],
        queryFn: () => productsApi.getByMerchant(MERCHANT_ID),
    });
    return (_jsxs("div", { className: "mx-auto max-w-6xl px-4 py-10 sm:px-6", children: [_jsxs("header", { className: "mb-10 text-center", children: [_jsx("p", { className: "font-body text-sm uppercase tracking-widest text-accent", children: "Local \u00B7 Fresh \u00B7 Delivered" }), _jsx("h1", { className: "mt-2 font-display text-5xl font-semibold text-foreground", children: "The Neighbourhood Store" }), _jsx("p", { className: "mx-auto mt-3 max-w-xl font-body text-foreground-secondary", children: "Handpicked everyday essentials from merchants around you \u2014 quality you can taste, prices you'll love." })] }), isLoading && (_jsx("div", { className: "grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4", children: Array.from({ length: 8 }).map((_, i) => (_jsx(Skeleton, { className: "aspect-[4/3] w-full rounded-card" }, i))) })), isError && (_jsx("p", { className: "text-center font-body text-error", children: "Could not load products. Please try again later." })), products && products.length === 0 && (_jsx("p", { className: "text-center font-body text-foreground-secondary", children: "No products available right now." })), products && products.length > 0 && (_jsx("div", { className: "grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4", children: products.map((product) => (_jsx(ProductCard, { product: product }, product.id))) }))] }));
}
