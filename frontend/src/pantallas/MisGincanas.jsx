import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { listarGincanas } from '../api/cliente'
import Cargando from '../components/Cargando'
import MensajeError from '../components/MensajeError'
import TarjetaGincana from '../components/TarjetaGincana'
import { borrarProgreso } from '../util/progreso'

const masRecientePrimero = (a, b) => String(b.fechaCreacion ?? '').localeCompare(String(a.fechaCreacion ?? ''))

export default function MisGincanas() {
  const navigate = useNavigate()
  const [datos, setDatos] = useState({ estado: 'cargando', gincanas: [], error: null })
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let activo = true
    setDatos({ estado: 'cargando', gincanas: [], error: null })
    listarGincanas()
      .then((gincanas) => {
        if (activo) setDatos({ estado: 'listo', gincanas: [...gincanas].sort(masRecientePrimero), error: null })
      })
      .catch((e) => {
        if (activo) setDatos({ estado: 'error', gincanas: [], error: e.message })
      })
    return () => {
      activo = false
    }
  }, [intento])

  // Volver a jugar empieza siempre desde el primer reto.
  function jugar(gincana) {
    borrarProgreso(gincana.id)
    navigate(`/jugar/${gincana.id}`, { state: { gincana } })
  }

  return (
    <main className="pantalla">
      <header className="cabecera">
        <h1>Mis gincanas</h1>
        <p className="texto-suave">Elige una para volver a jugarla.</p>
      </header>

      {datos.estado === 'cargando' && <Cargando mensaje="Cargando gincanas…" />}

      {datos.estado === 'error' && (
        <MensajeError
          titulo="No se han podido cargar las gincanas"
          mensaje={datos.error}
          onReintentar={() => setIntento((n) => n + 1)}
        />
      )}

      {datos.estado === 'listo' && datos.gincanas.length === 0 && (
        <div className="vacio">
          <p>Todavía no tienes ninguna gincana.</p>
          <Link to="/" className="boton boton--primario">
            Crear la primera
          </Link>
        </div>
      )}

      {datos.gincanas.length > 0 && (
        <ul className="lista">
          {datos.gincanas.map((gincana) => (
            <TarjetaGincana key={gincana.id} gincana={gincana} onJugar={jugar} />
          ))}
        </ul>
      )}
    </main>
  )
}
