import { NavLink } from 'react-router-dom'

const claseEnlace = ({ isActive }) => 'barra__enlace' + (isActive ? ' barra__enlace--activo' : '')

export default function BarraNavegacion() {
  return (
    <nav className="barra" aria-label="Navegación principal">
      <NavLink to="/" end className={claseEnlace}>
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 2C8.1 2 5 5.1 5 9c0 5.2 7 13 7 13s7-7.8 7-13c0-3.9-3.1-7-7-7zm0 9.5a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5z" />
        </svg>
        Nueva
      </NavLink>
      <NavLink to="/gincanas" className={claseEnlace}>
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M4 6h16v2H4zm0 5h16v2H4zm0 5h10v2H4z" />
        </svg>
        Mis gincanas
      </NavLink>
    </nav>
  )
}
