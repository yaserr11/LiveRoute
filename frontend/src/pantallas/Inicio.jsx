import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { previsualizarZona } from '../api/cliente'
import ContadorLugares from '../components/ContadorLugares'
import MapaZona from '../components/MapaZona'
import SelectorRadio from '../components/SelectorRadio'
import useDebounce from '../hooks/useDebounce'
import useGeolocalizacion, { PUERTO_DE_SAGUNTO } from '../hooks/useGeolocalizacion'

const RADIO_INICIAL = 1000

export default function Inicio() {
  const navigate = useNavigate()
  const ubicacion = useGeolocalizacion()
  const [centro, setCentro] = useState(null)
  const [radio, setRadio] = useState(RADIO_INICIAL)
  const [version, setVersion] = useState(0)
  const [vista, setVista] = useState({ lugares: null, error: null, zona: null })

  // Cuando llega la ubicación (o el respaldo), se usa como centro inicial.
  useEffect(() => {
    if (ubicacion.posicion && !centro) {
      setCentro(ubicacion.posicion)
      setVersion((v) => v + 1)
    }
  }, [ubicacion.posicion, centro])

  // Mismo objeto mientras no cambien centro ni radio (si no, el debounce nunca se quedaría quieto).
  const zonaActual = useMemo(() => centro && { ...centro, radioMetros: radio }, [centro, radio])

  // Solo se llama a la API cuando el jugador deja quieto el marcador y el slider ~600 ms.
  const zona = useDebounce(zonaActual, 600)

  useEffect(() => {
    if (!zona) return
    const control = new AbortController()
    previsualizarZona(zona, control.signal)
      .then((datos) => setVista({ lugares: datos.lugaresDisponibles, error: null, zona }))
      .catch((e) => {
        if (e.name !== 'AbortError') setVista({ lugares: null, error: e.message, zona })
      })
    // Si cambia la zona antes de que responda, se cancela: así nunca se muestra un dato viejo.
    return () => control.abort()
  }, [zona])

  // Calculando: aún sin centro, con cambios sin "asentar" o esperando la respuesta de esta zona.
  const cargando = !zonaActual || zona !== zonaActual || vista.zona !== zona
  const puedeContinuar = !cargando && vista.lugares > 0

  return (
    <main className="pantalla pantalla--mapa">
      <MapaZona
        centro={centro ?? PUERTO_DE_SAGUNTO}
        radio={radio}
        onMoverCentro={setCentro}
        version={version}
      />

      {ubicacion.estado === 'buscando' && <p className="chip chip--arriba">Buscando tu ubicación…</p>}
      {ubicacion.estado === 'respaldo' && (
        <p className="chip chip--arriba">Sin acceso a tu ubicación: mueve el marcador a tu zona</p>
      )}

      <section className="hoja" aria-label="Elegir zona">
        <div className="cabecera">
          <h1>Elige tu zona</h1>
          <p className="texto-suave">Arrastra el marcador o toca el mapa para mover el centro.</p>
        </div>
        <SelectorRadio valor={radio} onCambiar={setRadio} />
        <ContadorLugares cargando={cargando} lugares={vista.lugares} error={vista.error} />
        <button
          type="button"
          className="boton boton--primario boton--bloque"
          disabled={!puedeContinuar}
          onClick={() => navigate('/generar', { state: { zona } })}
        >
          Continuar
        </button>
      </section>
    </main>
  )
}
