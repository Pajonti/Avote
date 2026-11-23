import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'

// En dev, /api est relayé vers le back (VITE_API_PROXY, 8080 par défaut = « mvn spring-boot:run »).
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': process.env.VITE_API_PROXY ?? 'http://localhost:8080',
    },
  },
  test: {
    environment: 'happy-dom',
  },
})
