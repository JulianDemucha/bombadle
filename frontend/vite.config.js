import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    server: {
      port: 5173,
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