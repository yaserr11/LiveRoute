/** Botones − / + para elegir un número entre "min" y "max". */
export default function SelectorNumero({ etiqueta, valor, min, max, onCambiar }) {
  return (
    <div className="selector-numero">
      <span className="etiqueta" id="selector-numero-etiqueta">
        {etiqueta}
      </span>
      <div className="selector-numero__controles" role="group" aria-labelledby="selector-numero-etiqueta">
        <button
          type="button"
          className="selector-numero__boton"
          onClick={() => onCambiar(valor - 1)}
          disabled={valor <= min}
          aria-label="Quitar uno"
        >
          −
        </button>
        <output className="selector-numero__valor" aria-live="polite">
          {valor}
        </output>
        <button
          type="button"
          className="selector-numero__boton"
          onClick={() => onCambiar(valor + 1)}
          disabled={valor >= max}
          aria-label="Añadir uno"
        >
          +
        </button>
      </div>
    </div>
  )
}
