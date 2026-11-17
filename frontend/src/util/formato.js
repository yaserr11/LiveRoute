const decimal = new Intl.NumberFormat('es-ES', { maximumFractionDigits: 1 })

/** 800 → "800 m", 1500 → "1,5 km" */
export function formatearDistancia(metros) {
  return metros < 1000 ? `${Math.round(metros)} m` : `${decimal.format(metros / 1000)} km`
}

const NOMBRES_DIFICULTAD = { facil: 'Fácil', media: 'Media', dificil: 'Difícil' }

export function nombreDificultad(dificultad) {
  return NOMBRES_DIFICULTAD[dificultad] ?? dificultad ?? '—'
}
