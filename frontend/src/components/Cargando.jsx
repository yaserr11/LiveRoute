/** Spinner grande con un mensaje, para esperas largas. */
export default function Cargando({ mensaje, detalle }) {
  return (
    <div className="cargando" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      <p className="cargando__mensaje">{mensaje}</p>
      {detalle && <p className="texto-suave">{detalle}</p>}
    </div>
  )
}
