import { API_URL } from './config'

const SIN_CONEXION = 'No se puede conectar con el servidor. Comprueba que el backend está arrancado.'

/**
 * Llama a la API y devuelve el JSON. Si el backend responde {"error": "..."},
 * lanza un Error con ese mensaje para poder enseñarlo tal cual al jugador.
 */
async function pedir(ruta, { metodo = 'GET', cuerpo, signal } = {}) {
  let respuesta
  try {
    respuesta = await fetch(API_URL + ruta, {
      method: metodo,
      headers: cuerpo ? { 'Content-Type': 'application/json' } : undefined,
      body: cuerpo ? JSON.stringify(cuerpo) : undefined,
      signal,
    })
  } catch (e) {
    if (e.name === 'AbortError') throw e
    throw new Error(SIN_CONEXION)
  }

  const datos = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    const error = new Error(datos?.error || `Algo ha fallado (error ${respuesta.status}). Inténtalo de nuevo.`)
    error.status = respuesta.status
    throw error
  }
  return datos
}

export function previsualizarZona({ latitud, longitud, radioMetros }, signal) {
  const params = new URLSearchParams({ latitud, longitud, radioMetros })
  return pedir(`/api/gincanas/zona/previsualizar?${params}`, { signal })
}

export function generarGincana(peticion) {
  return pedir('/api/gincanas/generar', { metodo: 'POST', cuerpo: peticion })
}

export function listarGincanas() {
  return pedir('/api/gincanas')
}

export function listarRetos(gincanaId) {
  return pedir(`/api/gincanas/${gincanaId}/retos`)
}

export function comprobarRespuesta(gincanaId, retoId, respuesta) {
  return pedir(`/api/gincanas/${gincanaId}/retos/${retoId}/comprobar`, {
    metodo: 'POST',
    cuerpo: { respuesta },
  })
}
