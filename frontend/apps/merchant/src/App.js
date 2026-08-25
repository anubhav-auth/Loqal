import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Routes, Route, Navigate } from 'react-router-dom';
import { RoleGuard } from '@loqal/auth';
import { Sidebar } from './components/Sidebar';
import { Dashboard } from './pages/Dashboard';
import { Catalog } from './pages/Catalog';
import { Orders } from './pages/Orders';
import { Coupons } from './pages/Coupons';
import { Profile } from './pages/Profile';
export default function App() {
    return (_jsx(RoleGuard, { role: "ROLE_MERCHANT", children: _jsxs("div", { className: "flex min-h-screen bg-background font-body text-foreground", children: [_jsx(Sidebar, {}), _jsx("main", { className: "flex-1 px-4 py-6 sm:px-8 sm:py-10", children: _jsx("div", { className: "mx-auto w-full max-w-6xl", children: _jsxs(Routes, { children: [_jsx(Route, { path: "/", element: _jsx(Navigate, { to: "/dashboard", replace: true }) }), _jsx(Route, { path: "/dashboard", element: _jsx(Dashboard, {}) }), _jsx(Route, { path: "/catalog", element: _jsx(Catalog, {}) }), _jsx(Route, { path: "/orders", element: _jsx(Orders, {}) }), _jsx(Route, { path: "/coupons", element: _jsx(Coupons, {}) }), _jsx(Route, { path: "/profile", element: _jsx(Profile, {}) })] }) }) })] }) }));
}
