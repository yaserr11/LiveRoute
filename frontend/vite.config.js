import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// El puerto 5173 es el que permite el backend en liveroute.cors.origenes.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true },
})
