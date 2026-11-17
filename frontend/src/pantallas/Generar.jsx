import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { generarGincana } from '../api/cliente'
import Cargando from '../components/Cargando'
import MensajeError from '../components/MensajeError'
import SelectorDificultad from '../components/SelectorDificultad'
import SelectorNumero from '../components/SelectorNumero'
import { formatearDistancia } from '../util/formato'

const RETOS_MIN = 1
const RETOS_MAX = 10

export default function Generar() {
  const navigate = useNavigate()
  const { zona, lugares } = useLocation().state ?? {}
  const [numeroRetos, setNumeroRetos] = useState(5)
  const [dificultad, setDificultad] = useState('media')
  const [generando, setGenerando] = useState(false)
  const [error, setError] = useState(null)

  // Se llega aquí desde Inicio con la zona elegida; si no (p. ej. abriendo la URL), hay que elegirla.
  if (!zona) {
    return (
      <main className="pantalla pantalla--centrada">
        <h1>Primero elige una zona</h1>
        <p className="texto-suave">La gincana se crea con los lugares que hay alrededor del punto que elijas.</p>
        <Link to="/" className="boton boton--primario">
          Elegir zona
        </Link>
      </main>
    )
  }

  async function generar() {
    setGenerando(true)
    setError(null)
    try {
      const gincana = await generarGincana({ ...zona, numeroRetos, dificultad })
      // "replace": al volver atrás desde el juego se va a Inicio, no a este formulario.
      navigate(`/jugar/${gincana.id}`, { replace: true, state: { gincana } })
    } catch (e) {
      setError(e.message)
      setGenerando(false)
    }
  }

  if (generando) {
    return (
      <main className="pantalla pantalla--centrada">
        <Cargando
          mensaje="Buscando lugares y creando tu gincana…"
          detalle="Puede tardar hasta 30 segundos. No cierres la pantalla."
        />
      </main>
    )
  }

  return (
    <main className="pantalla">
      <header className="cabecera">
        <h1>Crea tu gincana</h1>
        <p className="texto-suave">
          Zona de {formatearDistancia(zona.radioMetros)} de radio
          {lugares != null && ` · ${lugares} ${lugares === 1 ? 'lugar' : 'lugares'}`}
        </p>
      </header>

      <section className="tarjeta">
        <SelectorNumero
          etiqueta="Número de retos"
          valor={numeroRetos}
          min={RETOS_MIN}
          max={RETOS_MAX}
          onCambiar={setNumeroRetos}
        />
        {lugares != null && numeroRetos > lugares && (
          <p className="texto-suave">
            Solo hay {lugares} {lugares === 1 ? 'lugar' : 'lugares'} en la zona: la gincana tendrá{' '}
            {lugares} {lugares === 1 ? 'reto' : 'retos'}.
          </p>
        )}
      </section>

      <section className="tarjeta">
        <SelectorDificultad valor={dificultad} onCambiar={setDificultad} />
      </section>

      {error && (
        <MensajeError titulo="No se ha podido crear la gincana" mensaje={error} onReintentar={generar}>
          <Link to="/" className="boton boton--secundario">
            Cambiar zona
          </Link>
        </MensajeError>
      )}

      <div className="pie">
        <button type="button" className="boton boton--primario boton--bloque" onClick={generar}>
          Generar gincana
        </button>
        <Link to="/" className="boton boton--secundario boton--bloque">
          Volver al mapa
        </Link>
      </div>
    </main>
  )
}
