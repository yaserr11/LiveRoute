import { useEffect, useState } from 'react'

// Si el jugador no da permiso (o el navegador no puede), empezamos aquí.
export const PUERTO_DE_SAGUNTO = { latitud: 39.6603, longitud: -0.2386 }

/**
 * Pide la ubicación del jugador una sola vez.
 * estado: 'buscando' mientras espera, 'ok' si la tiene, 'respaldo' si usa Puerto de Sagunto.
 */
export default function useGeolocalizacion() {
  const [resultado, setResultado] = useState({ posicion: null, estado: 'buscando' })

  useEffect(() => {
    if (!('geolocation' in navigator)) {
      setResultado({ posicion: PUERTO_DE_SAGUNTO, estado: 'respaldo' })
      return
    }
    let activo = true
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        if (activo) {
          setResultado({ posicion: { latitud: coords.latitude, longitud: coords.longitude }, estado: 'ok' })
        }
      },
      () => {
        if (activo) setResultado({ posicion: PUERTO_DE_SAGUNTO, estado: 'respaldo' })
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 },
    )
    return () => {
      activo = false
    }
  }, [])

  return resultado
}
