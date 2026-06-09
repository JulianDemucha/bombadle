import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath } from 'url';
import { dirname, resolve } from 'path';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    server: {
      port: 5173,
      https: {
        key: fs.readFileSync(resolve(__dirname, 'certs/key.pem')),
        cert: fs.readFileSync(resolve(__dirname, 'certs/cert.pem')),
      },
      proxy: {
        '/api': {
          target: env.BACKEND_BASE_URL,
          changeOrigin: true,
          secure: false,
        },
        '/oauth2': {
          target: env.BACKEND_BASE_URL,
          changeOrigin: true,
          secure: false,
        },
        '/images': {
          target: env.BACKEND_BASE_URL,
          changeOrigin: true,
          secure: false,
        },
      },
    },
  };
});