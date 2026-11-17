/** Cuántos lugares hay en la zona elegida: calculando, número o error. */
export default function ContadorLugares({ cargando, lugares, error }) {
  if (cargando) {
    return (
      <p className="contador" aria-live="polite">
        <span className="spinner spinner--pequeno" aria-hidden="true" />
        Buscando lugares en la zona…
      </p>
    )
  }
  if (error) {
    return (
      <p className="contador contador--error" aria-live="polite">
        {error}
      </p>
    )
  }
  if (lugares == null) return null

  return (
    <p className={'contador' + (lugares === 0 ? ' contador--vacio' : '')} aria-live="polite">
      <strong className="contador__numero">{lugares}</strong>
      {lugares === 1 ? 'lugar disponible' : 'lugares disponibles'}
      {lugares === 0 && <span className="texto-suave"> · prueba a ampliar el radio</span>}
    </p>
  )
}
