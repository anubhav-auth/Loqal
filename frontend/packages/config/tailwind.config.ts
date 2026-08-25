import type { Config } from 'tailwindcss'

export default {
  content: [],
  theme: {
    extend: {
      colors: {
        background: '#fdfcfa',
        surface: '#ffffff',
        border: '#f0ebe6',
        foreground: {
          DEFAULT: '#2d2520',
          secondary: '#8a7d72',
          muted: '#999999',
        },
        accent: {
          DEFAULT: '#c4956a',
          light: '#fdf0e6',
        },
        success: '#22c55e',
        error: '#ef4444',
      },
      fontFamily: {
        display: ['Cormorant Garamond', 'serif'],
        body: ['Plus Jakarta Sans', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      borderRadius: {
        card: '12px',
        button: '50px',
      },
      boxShadow: {
        sm: '0 1px 3px rgba(0,0,0,0.04)',
        md: '0 4px 16px rgba(0,0,0,0.06)',
        lg: '0 8px 32px rgba(0,0,0,0.08)',
        hover: '0 8px 24px rgba(0,0,0,0.1)',
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
} satisfies Config
