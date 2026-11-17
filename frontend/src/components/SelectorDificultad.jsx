const OPCIONES = [
  { valor: 'facil', nombre: 'Fácil', ayuda: 'Pista + distancia y dirección' },
  { valor: 'media', nombre: 'Media', ayuda: 'Pista + distancia' },
  { valor: 'dificil', nombre: 'Difícil', ayuda: 'Solo la pista' },
]

export default function SelectorDificultad({ valor, onCambiar }) {
  const ayuda = OPCIONES.find((o) => o.valor === valor)?.ayuda

  return (
    <fieldset className="selector-dificultad">
      <legend className="etiqueta">Dificultad</legend>
      <div className="segmentos" role="radiogroup">
        {OPCIONES.map((opcion) => (
          <button
            key={opcion.valor}
            type="button"
            role="radio"
            aria-checked={valor === opcion.valor}
            className={'segmentos__opcion' + (valor === opcion.valor ? ' segmentos__opcion--activa' : '')}
            onClick={() => onCambiar(opcion.valor)}
          >
            {opcion.nombre}
          </button>
        ))}
      </div>
      <p className="texto-suave selector-dificultad__ayuda">{ayuda}</p>
    </fieldset>
  )
}
