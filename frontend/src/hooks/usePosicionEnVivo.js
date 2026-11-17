import { useEffect, useState } from 'react'

/** Sigue la posición del jugador mientras la pantalla está abierta. posicion es null hasta tenerla. */
export default function usePosicionEnVivo() {
  const [estado, setEstado] = useState({ posicion: null, sinPermiso: false })

  useEffect(() => {
    if (!('geolocation' in navigator)) {
      setEstado({ posicion: null, sinPermiso: true })
      return
    }
    const id = navigator.geolocation.watchPosition(
      ({ coords }) =>
        setEstado({ posicion: { latitud: coords.latitude, longitud: coords.longitude }, sinPermiso: false }),
      (error) => {
        if (error.code === error.PERMISSION_DENIED) setEstado((e) => ({ ...e, sinPermiso: true }))
      },
      { enableHighAccuracy: true, maximumAge: 5000, timeout: 20000 },
    )
    return () => navigator.geolocation.clearWatch(id)
  }, [])

  return estado
}
