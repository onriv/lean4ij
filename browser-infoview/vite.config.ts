import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5830,
    proxy: {
      '/api': {
        target: 'http://localhost:9093',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
