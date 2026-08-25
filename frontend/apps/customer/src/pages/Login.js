import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button, Input, Card, CardContent, CardHeader, CardTitle } from '@loqal/ui';
import { useAuth } from '@loqal/auth';
export function Login() {
    const { login } = useAuth();
    const navigate = useNavigate();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);
    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            await login(email, password);
            navigate('/');
        }
        catch (err) {
            setError(err instanceof Error ? err.message : 'Login failed');
        }
        finally {
            setLoading(false);
        }
    };
    return (_jsx("div", { className: "mx-auto flex min-h-[80vh] max-w-md flex-col justify-center px-4", children: _jsxs(Card, { children: [_jsxs(CardHeader, { children: [_jsx(CardTitle, { className: "text-center font-display text-3xl", children: "Welcome back" }), _jsx("p", { className: "text-center font-body text-sm text-foreground-secondary", children: "Sign in to your Loqal account" })] }), _jsxs(CardContent, { children: [_jsxs("form", { onSubmit: handleSubmit, className: "flex flex-col gap-4", children: [_jsx(Input, { type: "email", placeholder: "Email", value: email, onChange: (e) => setEmail(e.target.value), required: true }), _jsx(Input, { type: "password", placeholder: "Password", value: password, onChange: (e) => setPassword(e.target.value), required: true }), error && _jsx("p", { className: "font-body text-sm text-error", children: error }), _jsx(Button, { type: "submit", disabled: loading, className: "w-full", children: loading ? 'Signing in…' : 'Sign in' })] }), _jsxs("p", { className: "mt-4 text-center font-body text-sm text-foreground-secondary", children: ["New here?", ' ', _jsx(Link, { to: "/register", className: "text-accent underline-offset-4 hover:underline", children: "Create an account" })] })] })] }) }));
}
