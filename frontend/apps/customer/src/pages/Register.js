import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button, Input, Card, CardContent, CardHeader, CardTitle } from '@loqal/ui';
import { useAuth } from '@loqal/auth';
export function Register() {
    const { register } = useAuth();
    const navigate = useNavigate();
    const [form, setForm] = useState({
        fullName: '',
        email: '',
        phoneNumber: '',
        password: '',
    });
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);
    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            await register({
                fullName: form.fullName,
                email: form.email,
                phoneNumber: form.phoneNumber || undefined,
                password: form.password,
            });
            navigate('/');
        }
        catch (err) {
            setError(err instanceof Error ? err.message : 'Registration failed');
        }
        finally {
            setLoading(false);
        }
    };
    return (_jsx("div", { className: "mx-auto flex min-h-[80vh] max-w-md flex-col justify-center px-4", children: _jsxs(Card, { children: [_jsxs(CardHeader, { children: [_jsx(CardTitle, { className: "text-center font-display text-3xl", children: "Join Loqal" }), _jsx("p", { className: "text-center font-body text-sm text-foreground-secondary", children: "Create your account to start shopping local" })] }), _jsxs(CardContent, { children: [_jsxs("form", { onSubmit: handleSubmit, className: "flex flex-col gap-4", children: [_jsx(Input, { placeholder: "Full name", value: form.fullName, onChange: (e) => setForm({ ...form, fullName: e.target.value }), required: true }), _jsx(Input, { type: "email", placeholder: "Email", value: form.email, onChange: (e) => setForm({ ...form, email: e.target.value }), required: true }), _jsx(Input, { type: "tel", placeholder: "Phone number", value: form.phoneNumber, onChange: (e) => setForm({ ...form, phoneNumber: e.target.value }) }), _jsx(Input, { type: "password", placeholder: "Password", value: form.password, onChange: (e) => setForm({ ...form, password: e.target.value }), required: true }), error && _jsx("p", { className: "font-body text-sm text-error", children: error }), _jsx(Button, { type: "submit", disabled: loading, className: "w-full", children: loading ? 'Creating account…' : 'Create account' })] }), _jsxs("p", { className: "mt-4 text-center font-body text-sm text-foreground-secondary", children: ["Already have an account?", ' ', _jsx(Link, { to: "/login", className: "text-accent underline-offset-4 hover:underline", children: "Sign in" })] })] })] }) }));
}
