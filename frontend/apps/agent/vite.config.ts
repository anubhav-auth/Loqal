import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import VitePWA from 'vite-plugin-pwa'
import path from 'path'

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      manifest: {
        name: 'Loqal Agent',
        short_name: 'Loqal Agent',
        description: 'Delivery agent mobile app for Loqal',
        theme_color: '#c4956a',
        background_color: '#fdfcfa',
        display: 'standalone',
        start_url: '/',
        icons: [],
      },
    }),
  ],
  resolve: { alias: { '@': path.resolve(__dirname, './src') } },
})
