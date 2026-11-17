import L from 'leaflet'

// Marcadores dibujados con CSS (clase .marcador): el icono por defecto de Leaflet usa imágenes
// que Vite no encuentra.
function punto(tipo, tamano) {
  return L.divIcon({
    className: `marcador marcador--${tipo}`,
    iconSize: [tamano, tamano],
    iconAnchor: [tamano / 2, tamano / 2],
  })
}

export const iconoCentro = punto('centro', 28)
export const iconoJugador = punto('jugador', 20)
export const iconoAnterior = punto('anterior', 20)
