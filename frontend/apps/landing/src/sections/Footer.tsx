import { Link } from 'react-router-dom'

const columns = [
  {
    title: 'Product',
    links: [
      { label: 'Features', href: '#features' },
      { label: 'How It Works', href: '#how-it-works' },
      { label: 'Pricing', href: '#' },
    ],
  },
  {
    title: 'Company',
    links: [
      { label: 'About', href: '#' },
      { label: 'Merchants', href: '#testimonials' },
      { label: 'Careers', href: '#' },
    ],
  },
  {
    title: 'Resources',
    links: [
      { label: 'Help Center', href: '#' },
      { label: 'Blog', href: '#' },
      { label: 'Contact', href: '#' },
    ],
  },
]

export default function Footer() {
  return (
    <footer className="border-t border-border bg-surface/60">
      <div className="mx-auto grid max-w-6xl gap-12 px-4 py-16 sm:px-6 md:grid-cols-[1.5fr_repeat(3,1fr)]">
        <div>
          <p className="font-display text-3xl font-semibold text-foreground">Loqal</p>
          <p className="mt-4 max-w-xs text-foreground-secondary">
            Commerce for local merchants, beautifully simple.
          </p>
        </div>
        {columns.map((col) => (
          <div key={col.title}>
            <h3 className="font-medium text-foreground">{col.title}</h3>
            <ul className="mt-4 space-y-3">
              {col.links.map((link) => (
                <li key={link.label}>
                  <a
                    href={link.href}
                    className="text-foreground-secondary transition-colors hover:text-foreground"
                  >
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
      <div className="border-t border-border">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 px-4 py-6 text-sm text-foreground-secondary sm:flex-row sm:px-6">
          <p>© {new Date().getFullYear()} Loqal. All rights reserved.</p>
          <div className="flex gap-6">
            <Link to="/login" className="transition-colors hover:text-foreground">
              Merchant Login
            </Link>
            <a href="#" className="transition-colors hover:text-foreground">
              Privacy
            </a>
            <a href="#" className="transition-colors hover:text-foreground">
              Terms
            </a>
          </div>
        </div>
      </div>
    </footer>
  )
}
