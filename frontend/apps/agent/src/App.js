import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Routes, Route, Navigate } from 'react-router-dom';
import { RoleGuard } from '@loqal/auth';
import { BottomNav } from './components/BottomNav';
import { Dashboard } from './pages/Dashboard';
import { Assignments } from './pages/Assignments';
import { OtpVerification } from './pages/OtpVerification';
import { Profile } from './pages/Profile';
export default function App() {
    return (_jsx(RoleGuard, { role: "ROLE_DELIVERY_AGENT", children: _jsxs("div", { className: "flex min-h-screen flex-col bg-background font-body text-foreground", children: [_jsx("main", { className: "flex-1 px-4 pb-24 pt-6", children: _jsx("div", { className: "mx-auto w-full max-w-md", children: _jsxs(Routes, { children: [_jsx(Route, { path: "/", element: _jsx(Dashboard, {}) }), _jsx(Route, { path: "/assignments", element: _jsx(Assignments, {}) }), _jsx(Route, { path: "/otp/:orderId", element: _jsx(OtpVerification, {}) }), _jsx(Route, { path: "/profile", element: _jsx(Profile, {}) }), _jsx(Route, { path: "*", element: _jsx(Navigate, { to: "/", replace: true }) })] }) }) }), _jsx(BottomNav, {})] }) }));
}
