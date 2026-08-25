import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { NavLink, useLocation } from 'react-router-dom';
import { Home, Package, User } from 'lucide-react';
import { cn } from '@loqal/ui';
const tabs = [
    { to: '/', label: 'Home', icon: Home },
    { to: '/assignments', label: 'Tasks', icon: Package },
    { to: '/profile', label: 'Profile', icon: User },
];
export function BottomNav() {
    const { pathname } = useLocation();
    return (_jsx("nav", { className: "fixed inset-x-0 bottom-0 z-20 border-t border-border bg-surface/95 backdrop-blur", children: _jsx("ul", { className: "mx-auto flex max-w-md items-stretch justify-around", children: tabs.map(({ to, label, icon: Icon }) => {
                const active = pathname === to;
                return (_jsx("li", { className: "flex-1", children: _jsxs(NavLink, { to: to, className: cn('flex min-h-[64px] flex-col items-center justify-center gap-1 py-2 text-xs font-medium transition-colors', active ? 'text-accent' : 'text-foreground-secondary'), children: [_jsx(Icon, { size: 24, strokeWidth: active ? 2.4 : 1.8 }), label] }) }, to));
            }) }) }));
}
