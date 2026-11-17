// Por qué reto va el jugador en cada gincana, guardado en el navegador para no perderlo al recargar.
// localStorage puede fallar (modo privado, almacenamiento bloqueado): en ese caso se empieza de 0.
const clave = (gincanaId) => `liveroute.progreso.${gincanaId}`

export function leerProgreso(gincanaId) {
  try {
    const valor = Number(localStorage.getItem(clave(gincanaId)))
    return Number.isInteger(valor) && valor > 0 ? valor : 0
  } catch {
    return 0
  }
}

export function guardarProgreso(gincanaId, indice) {
  try {
    localStorage.setItem(clave(gincanaId), String(indice))
  } catch {
    // Sin almacenamiento: el progreso solo dura mientras la pantalla esté abierta.
  }
}

export function borrarProgreso(gincanaId) {
  try {
    localStorage.removeItem(clave(gincanaId))
  } catch {
    // Nada que borrar.
  }
}
