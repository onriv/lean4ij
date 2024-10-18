import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import fs from 'fs'; // Node.js file system module

// ... moving it to a standalone json file make vite's auto reload failed...
let hostConfig;
const hostPath = './host-config.json';
try {
  hostConfig = JSON.parse(fs.readFileSync('./host-config.json', 'utf8'));
} catch (e: any) {
  console.error("Error reading host config:", e.message);
  // a default fallback
  hostConfig = { host: "localhost:8080" };
}
const { host } = hostConfig;

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(),
    {
      name: 'watch-host-config',
      configureServer(server) {
        fs.watchFile(hostPath, () => {
          server.restart().then(r => {})
        });
      }
    }
  ],
  server: {
    port: 5830,
    proxy: {
      // https://vitejs.dev/config/server-options
      '/ws': {
        target: 'ws://'+host,
        ws: true,
        changeOrigin: true,
      },
      '/theme.css': {
        target: 'http://'+host,
        changeOrigin: true,
        secure: false,
        // check https://github.com/vitejs/vite/issues/12157
        // timeout: 0, // here
      },
      '/imports': {
        target: 'http://'+host,
        changeOrigin: true,
        secure: false,
        // check https://github.com/vitejs/vite/issues/12157
        // timeout: 0, // here
      },
      '/api': {
        target: 'http://'+host,
        changeOrigin: true,
        secure: false,
        // check https://github.com/vitejs/vite/issues/12157
        // timeout: 0, // here
      },
    },
  },
})
