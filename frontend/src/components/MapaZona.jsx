import { useEffect, useMemo } from 'react'
import L from 'leaflet'
import { Circle, MapContainer, Marker, TileLayer, useMap, useMapEvents } from 'react-leaflet'
import useDebounce from '../hooks/useDebounce'
import { iconoCentro } from './iconosMapa'

export const CAPA_OSM = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png'
export const ATRIBUCION = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'

/** Ajusta el zoom para que se vea todo el círculo, dejando sitio al panel de abajo. */
function EncuadrarZona({ centro, radio: radioEnVivo, version }) {
  const map = useMap()
  // Se espera a que el slider se quede quieto para no mover el mapa en cada paso.
  const radio = useDebounce(radioEnVivo, 300)
  useEffect(() => {
    const limites = L.latLng(centro.latitud, centro.longitud).toBounds(radio * 2)
    map.fitBounds(limites, { paddingTopLeft: [16, 16], paddingBottomRight: [16, 230], animate: true })
    // Solo al cambiar el radio o al recibir la ubicación, no cada vez que se arrastra el marcador.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, radio, version])
  return null
}

/** Tocar el mapa también mueve el centro. */
function TocarParaMover({ onMover }) {
  useMapEvents({ click: (e) => onMover({ latitud: e.latlng.lat, longitud: e.latlng.lng }) })
  return null
}

/**
 * Mapa con el centro de la gincana (marcador que se puede arrastrar) y el radio como círculo.
 * "version" cambia cuando el centro llega desde fuera (geolocalización) para volver a encuadrar.
 */
export default function MapaZona({ centro, radio, onMoverCentro, version }) {
  const posicion = [centro.latitud, centro.longitud]

  const eventos = useMemo(
    () => ({
      dragend: (e) => {
        const { lat, lng } = e.target.getLatLng()
        onMoverCentro({ latitud: lat, longitud: lng })
      },
    }),
    [onMoverCentro],
  )

  return (
    <MapContainer center={posicion} zoom={15} zoomControl={false} className="mapa-zona">
      <TileLayer url={CAPA_OSM} attribution={ATRIBUCION} />
      <Circle
        center={posicion}
        radius={radio}
        pathOptions={{ color: '#0f766e', weight: 2, fillColor: '#14b8a6', fillOpacity: 0.15 }}
      />
      <Marker position={posicion} draggable icon={iconoCentro} eventHandlers={eventos} />
      <EncuadrarZona centro={centro} radio={radio} version={version} />
      <TocarParaMover onMover={onMoverCentro} />
    </MapContainer>
  )
}
