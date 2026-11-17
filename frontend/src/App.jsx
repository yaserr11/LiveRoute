import { Navigate, Route, Routes } from 'react-router-dom'
import BarraNavegacion from './components/BarraNavegacion'
import Generar from './pantallas/Generar'
import Inicio from './pantallas/Inicio'
import Jugar from './pantallas/Jugar'

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
        <Route path="/" element={<Inicio />} />
        <Route path="/generar" element={<Generar />} />
        <Route path="/jugar/:id" element={<Jugar />} />
        <Route path="/gincanas" element={<Proximamente titulo="Mis gincanas" />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <BarraNavegacion />
    </div>
  )
}
