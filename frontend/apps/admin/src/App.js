import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Routes, Route, Navigate } from 'react-router-dom';
import { RoleGuard } from '@loqal/auth';
import { Sidebar } from './components/Sidebar';
import { Merchants } from './pages/Merchants';
import { AuditTrail } from './pages/AuditTrail';
import { Metrics } from './pages/Metrics';
export default function App() {
    return (_jsx(RoleGuard, { role: "ROLE_ADMIN", children: _jsxs("div", { className: "flex min-h-screen bg-background font-body text-foreground", children: [_jsx(Sidebar, {}), _jsx("main", { className: "flex-1 px-4 py-6 sm:px-8 sm:py-10", children: _jsx("div", { className: "mx-auto w-full max-w-6xl", children: _jsxs(Routes, { children: [_jsx(Route, { path: "/", element: _jsx(Navigate, { to: "/metrics", replace: true }) }), _jsx(Route, { path: "/merchants", element: _jsx(Merchants, {}) }), _jsx(Route, { path: "/audit", element: _jsx(AuditTrail, {}) }), _jsx(Route, { path: "/metrics", element: _jsx(Metrics, {}) })] }) }) })] }) }));
}
