import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],

    server: {
      port: 5173, // frontend (React/Vite)
      proxy: {
        '/api': {
          target: 'https://localhost:8443',
          changeOrigin: true,
          secure: false, //dev cert
        },
      },
    },
})
