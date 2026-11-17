import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { comprobarRespuesta, listarGincanas, listarRetos } from '../api/cliente'
import Cargando from '../components/Cargando'
import MapaJuego from '../components/MapaJuego'
import MensajeError from '../components/MensajeError'
import PantallaFinal from '../components/PantallaFinal'
import TarjetaReto from '../components/TarjetaReto'
import usePosicionEnVivo from '../hooks/usePosicionEnVivo'
import { borrarProgreso, guardarProgreso, leerProgreso } from '../util/progreso'

/**
 * Datos de la gincana (título y punto de salida). Llegan desde Generar o Mis gincanas;
 * si se abre la URL directamente, se buscan en la lista.
 */
async function cargarGincana(id, desdeOtraPantalla) {
  if (desdeOtraPantalla && String(desdeOtraPantalla.id) === id) return desdeOtraPantalla
  const todas = await listarGincanas()
  return todas.find((g) => String(g.id) === id) ?? null
}

export default function Jugar() {
  const { id } = useParams()
  const desdeOtraPantalla = useLocation().state?.gincana
  const { posicion, sinPermiso } = usePosicionEnVivo()

  const [datos, setDatos] = useState({ estado: 'cargando', gincana: null, retos: [], error: null })
  const [indice, setIndice] = useState(() => leerProgreso(id))
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let activo = true
    setDatos({ estado: 'cargando', gincana: null, retos: [], error: null })
    Promise.all([listarRetos(id), cargarGincana(id, desdeOtraPantalla).catch(() => null)])
      .then(([retos, gincana]) => {
        if (activo) setDatos({ estado: 'listo', gincana, retos, error: null })
      })
      .catch((e) => {
        if (activo) setDatos({ estado: 'error', gincana: null, retos: [], error: e })
      })
    return () => {
      activo = false
    }
    // desdeOtraPantalla solo se usa como atajo la primera vez.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, intento])

  const reto = datos.retos[indice]

  const comprobar = useCallback(
    async (respuesta) => (await comprobarRespuesta(id, reto.id, respuesta)).correcto,
    [id, reto],
  )

  const avanzar = useCallback(() => {
    setIndice((i) => {
      guardarProgreso(id, i + 1)
      return i + 1
    })
  }, [id])

  function jugarOtraVez() {
    borrarProgreso(id)
    setIndice(0)
  }

  if (datos.estado === 'cargando') {
    return (
      <main className="pantalla pantalla--centrada">
        <Cargando mensaje="Cargando la gincana…" />
      </main>
    )
  }

  if (datos.estado === 'error') {
    const noExiste = datos.error.status === 404
    return (
      <main className="pantalla pantalla--centrada">
        <MensajeError
          titulo={noExiste ? 'Esta gincana no existe' : 'No se ha podido cargar la gincana'}
          mensaje={noExiste ? 'Puede que se haya borrado. Elige otra en Mis gincanas.' : datos.error.message}
          onReintentar={noExiste ? undefined : () => setIntento((n) => n + 1)}
        >
          <Link to="/gincanas" className="boton boton--secundario">
            Mis gincanas
          </Link>
        </MensajeError>
      </main>
    )
  }

  const { gincana, retos } = datos

  if (retos.length === 0) {
    return (
      <main className="pantalla pantalla--centrada">
        <h1>Esta gincana no tiene retos</h1>
        <p className="texto-suave">No hay nada que jugar todavía.</p>
        <Link to="/gincanas" className="boton boton--primario">
          Volver a Mis gincanas
        </Link>
      </main>
    )
  }

  if (indice >= retos.length) {
    return <PantallaFinal titulo={gincana?.titulo} totalRetos={retos.length} onJugarOtraVez={jugarOtraVez} />
  }

  // Punto de referencia: la salida para el primer reto, el reto ya resuelto para los demás.
  const anterior = indice === 0 ? gincana : retos[indice - 1]
  const nombreAnterior = indice === 0 ? 'Salida' : `Reto ${indice}`

  return (
    <main className="pantalla pantalla--juego">
      <header className="progreso">
        <div className="progreso__fila">
          <span className="etiqueta">
            Reto {indice + 1} de {retos.length}
          </span>
          {gincana?.titulo && <span className="progreso__titulo">{gincana.titulo}</span>}
        </div>
        <div
          className="progreso__barra"
          role="progressbar"
          aria-valuemin={0}
          aria-valuemax={retos.length}
          aria-valuenow={indice}
        >
          <span style={{ width: `${(indice / retos.length) * 100}%` }} />
        </div>
      </header>

      <div className="juego__mapa">
        {posicion || anterior ? (
          <MapaJuego jugador={posicion} anterior={anterior} nombreAnterior={nombreAnterior} />
        ) : null}
        {sinPermiso && <p className="chip chip--arriba">Activa la ubicación para verte en el mapa</p>}
      </div>

      <TarjetaReto key={reto.id} reto={reto} onComprobar={comprobar} onAcertado={avanzar} />
    </main>
  )
}
