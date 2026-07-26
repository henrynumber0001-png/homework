import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('@tanstack/react-query-devtools')) {
            return 'query-devtools'
          }
          if (id.includes('qrcode.react')) return 'qrcode'
          if (id.includes('/axios/') || id.includes('/follow-redirects/')) {
            return 'http-vendor'
          }
          return undefined
        },
      },
    },
  },
})
