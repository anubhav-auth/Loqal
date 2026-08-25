import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import Navbar from './components/Navbar';
import Hero from './sections/Hero';
import Features from './sections/Features';
import HowItWorks from './sections/HowItWorks';
import Testimonials from './sections/Testimonials';
import CTA from './sections/CTA';
import Footer from './sections/Footer';
export default function App() {
    return (_jsxs("div", { className: "min-h-screen bg-background font-body text-foreground", children: [_jsx(Navbar, {}), _jsxs("main", { children: [_jsx(Hero, {}), _jsx(Features, {}), _jsx(HowItWorks, {}), _jsx(Testimonials, {}), _jsx(CTA, {})] }), _jsx(Footer, {})] }));
}
