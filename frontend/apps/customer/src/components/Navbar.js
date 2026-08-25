import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
import { Link } from 'react-router-dom';
import { ShoppingBag } from 'lucide-react';
import { Button } from '@loqal/ui';
import { useCartStore } from '@/store/cartStore';
import { useAuth } from '@loqal/auth';
export function Navbar() {
    const count = useCartStore((s) => s.itemCount());
    const { isAuthenticated, logout, user } = useAuth();
    return (_jsx("header", { className: "sticky top-0 z-40 border-b border-border bg-background/80 backdrop-blur-md", children: _jsxs("nav", { className: "mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6", children: [_jsx(Link, { to: "/", className: "font-display text-2xl font-semibold tracking-tight text-foreground", children: "Loqal" }), _jsxs("div", { className: "flex items-center gap-2 sm:gap-4", children: [isAuthenticated ? (_jsx("span", { className: "hidden font-body text-sm text-foreground-secondary sm:inline", children: user?.user_id })) : (_jsxs(_Fragment, { children: [_jsx(Button, { asChild: true, variant: "ghost", size: "sm", children: _jsx(Link, { to: "/login", children: "Sign in" }) }), _jsx(Button, { asChild: true, variant: "default", size: "sm", children: _jsx(Link, { to: "/register", children: "Sign up" }) })] })), _jsx(Button, { asChild: true, variant: "outline", size: "icon", className: "relative", "aria-label": "Cart", children: _jsxs(Link, { to: "/cart", children: [_jsx(ShoppingBag, { className: "h-5 w-5" }), count > 0 && (_jsx("span", { className: "absolute -right-1.5 -top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-accent text-[11px] font-semibold text-white", children: count }))] }) }), isAuthenticated && (_jsx(Button, { variant: "ghost", size: "sm", onClick: logout, children: "Sign out" }))] })] }) }));
}
