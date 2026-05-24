import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath } from 'url';
import { dirname, resolve } from 'path';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const address = "localhost";
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    https: {
      key: fs.readFileSync(resolve(__dirname, 'certs/key.pem')),
      cert: fs.readFileSync(resolve(__dirname, 'certs/cert.pem')),
    },
    proxy: {
      '/api': {
        target: 'https://'+address+':8443',
        changeOrigin: true,
        secure: false,
      },
      '/oauth2': {
        target: 'https://'+address+':8443',
        changeOrigin: true,
        secure: false,
      },
      '/images': {
        target: 'https://'+address+':8443',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
