import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Vite configuration for the OAuth2 frontend.
// The dev server runs on port 5173 and proxies API calls to the backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // Proxy API requests to the Spring Boot backend
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/oauth2': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
