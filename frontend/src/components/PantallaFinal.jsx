import { Link } from 'react-router-dom'

export default function PantallaFinal({ titulo, totalRetos, onJugarOtraVez }) {
  return (
    <main className="pantalla pantalla--centrada pantalla-final">
      <div className="pantalla-final__trofeo" aria-hidden="true">
        🏆
      </div>
      <h1>¡Gincana completada!</h1>
      <p className="texto-suave">
        Has resuelto {totalRetos === 1 ? 'el reto' : `los ${totalRetos} retos`}
        {titulo && (
          <>
            {' '}
            de <strong>{titulo}</strong>
          </>
        )}
        .
      </p>
      <div className="pie pie--estrecho">
        <Link to="/" className="boton boton--primario boton--bloque">
          Crear otra gincana
        </Link>
        <button type="button" className="boton boton--secundario boton--bloque" onClick={onJugarOtraVez}>
          Jugar otra vez
        </button>
        <Link to="/gincanas" className="boton boton--secundario boton--bloque">
          Mis gincanas
        </Link>
      </div>
    </main>
  )
}
