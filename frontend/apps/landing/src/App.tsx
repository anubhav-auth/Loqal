import Navbar from './components/Navbar'
import Hero from './sections/Hero'
import Features from './sections/Features'
import HowItWorks from './sections/HowItWorks'
import Testimonials from './sections/Testimonials'
import CTA from './sections/CTA'
import Footer from './sections/Footer'

export default function App() {
  return (
    <div className="min-h-screen bg-background font-body text-foreground">
      <Navbar />
      <main>
        <Hero />
        <Features />
        <HowItWorks />
        <Testimonials />
        <CTA />
      </main>
      <Footer />
    </div>
  )
}
