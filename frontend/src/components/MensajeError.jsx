/** Error explicado al jugador, con un botón opcional para volver a intentarlo. */
export default function MensajeError({ titulo = 'Vaya, algo ha fallado', mensaje, onReintentar, children }) {
  return (
    <div className="aviso aviso--error" role="alert">
      <strong>{titulo}</strong>
      <p>{mensaje}</p>
      {(onReintentar || children) && (
        <div className="aviso__acciones">
          {onReintentar && (
            <button type="button" className="boton boton--secundario" onClick={onReintentar}>
              Reintentar
            </button>
          )}
          {children}
        </div>
      )}
    </div>
  )
}
