import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5830,
    proxy: {
      '/api': {
        target: 'http://localhost:19090',
        changeOrigin: true,
        secure: false,
        // check https://github.com/vitejs/vite/issues/12157
        // timeout: 0, // here
      },
    },
  },
})
