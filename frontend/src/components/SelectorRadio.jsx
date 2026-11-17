import { formatearDistancia } from '../util/formato'

export const RADIO_MIN = 200
export const RADIO_MAX = 3000

export default function SelectorRadio({ valor, onCambiar }) {
  const porcentaje = ((valor - RADIO_MIN) / (RADIO_MAX - RADIO_MIN)) * 100

  return (
    <label className="selector-radio">
      <span className="selector-radio__fila">
        <span className="etiqueta">Radio de la zona</span>
        <strong>{formatearDistancia(valor)}</strong>
      </span>
      <input
        type="range"
        min={RADIO_MIN}
        max={RADIO_MAX}
        step={50}
        value={valor}
        onChange={(e) => onCambiar(Number(e.target.value))}
        style={{ '--relleno': `${porcentaje}%` }}
      />
    </label>
  )
}
