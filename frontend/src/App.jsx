import { Navigate, Route, Routes } from 'react-router-dom'
import BarraNavegacion from './components/BarraNavegacion'

function Proximamente({ titulo }) {
  return (
    <main className="pantalla pantalla--centrada">
      <h1>{titulo}</h1>
      <p className="texto-suave">Próximamente</p>
    </main>
  )
}

export default function App() {
  return (
    <div className="app">
      <Routes>
        <Route path="/" element={<Proximamente titulo="Inicio" />} />
        <Route path="/generar" element={<Proximamente titulo="Generar" />} />
        <Route path="/jugar/:id" element={<Proximamente titulo="Jugar" />} />
        <Route path="/gincanas" element={<Proximamente titulo="Mis gincanas" />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <BarraNavegacion />
    </div>
  )
}
