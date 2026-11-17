// Único sitio donde se lee la dirección del backend (variable VITE_API_URL del archivo .env).
export const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/+$/, '')
