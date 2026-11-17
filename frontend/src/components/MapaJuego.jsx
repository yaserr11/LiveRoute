import { useEffect, useRef } from 'react'
import L from 'leaflet'
import { MapContainer, Marker, TileLayer, Tooltip, useMap } from 'react-leaflet'
import { iconoAnterior, iconoJugador } from './iconosMapa'
import { ATRIBUCION, CAPA_OSM } from './MapaZona'

const aLatLng = (p) => [p.latitud, p.longitud]

/** Encuadra al jugador y al punto anterior cuando cambia el reto o llega la primera posición. */
function Encuadrar({ jugador, anterior }) {
  const map = useMap()
  const tieneJugador = jugador != null
  const ultimoEncuadre = useRef(null)

  useEffect(() => {
    const motivo = `${anterior?.latitud},${anterior?.longitud},${tieneJugador}`
    if (ultimoEncuadre.current === motivo) return
    ultimoEncuadre.current = motivo

    const puntos = [jugador, anterior].filter(Boolean).map(aLatLng)
    if (puntos.length === 2) {
      map.fitBounds(L.latLngBounds(puntos), { padding: [40, 40], maxZoom: 17 })
    } else if (puntos.length === 1) {
      map.setView(puntos[0], 16)
    }
    // Solo se vuelve a encuadrar al cambiar de reto o al tener posición por primera vez,
    // no con cada pequeño movimiento del jugador.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, anterior, tieneJugador])

  return null
}

/**
 * Mapa del juego: la posición del jugador y el punto de donde viene (salida o reto anterior).
 * Nunca recibe ni dibuja el lugar del reto actual, porque eso sería dar la respuesta.
 */
export default function MapaJuego({ jugador, anterior, nombreAnterior }) {
  const inicial = aLatLng(jugador ?? anterior)

  return (
    <MapContainer center={inicial} zoom={16} zoomControl={false} className="mapa-juego">
      <TileLayer url={CAPA_OSM} attribution={ATRIBUCION} />
      {anterior && (
        <Marker position={aLatLng(anterior)} icon={iconoAnterior}>
          <Tooltip direction="top" offset={[0, -10]}>
            {nombreAnterior}
          </Tooltip>
        </Marker>
      )}
      {jugador && <Marker position={aLatLng(jugador)} icon={iconoJugador} />}
      <Encuadrar jugador={jugador} anterior={anterior} />
    </MapContainer>
  )
}
