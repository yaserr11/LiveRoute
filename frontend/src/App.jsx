import { Navigate, Route, Routes } from 'react-router-dom'
import BarraNavegacion from './components/BarraNavegacion'
import Generar from './pantallas/Generar'
import Inicio from './pantallas/Inicio'
import Jugar from './pantallas/Jugar'
import MisGincanas from './pantallas/MisGincanas'

export default function App() {
  return (
    <div className="app">
      <Routes>
        <Route path="/" element={<Inicio />} />
        <Route path="/generar" element={<Generar />} />
        <Route path="/jugar/:id" element={<Jugar />} />
        <Route path="/gincanas" element={<MisGincanas />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <BarraNavegacion />
    </div>
  )
}
