import { nombreDificultad } from '../util/formato'

const formatoFecha = new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short', year: 'numeric' })

function fecha(texto) {
  const valor = texto ? new Date(texto) : null
  return valor && !Number.isNaN(valor.getTime()) ? formatoFecha.format(valor) : null
}

export default function TarjetaGincana({ gincana, onJugar }) {
  const creada = fecha(gincana.fechaCreacion)

  return (
    <li className="tarjeta tarjeta-gincana">
      <div className="tarjeta-gincana__texto">
        <h2>{gincana.titulo}</h2>
        <p className="texto-suave">{gincana.lugar}</p>
        <p className="tarjeta-gincana__datos">
          <span className={`insignia insignia--${gincana.dificultad ?? 'media'}`}>
            {nombreDificultad(gincana.dificultad)}
          </span>
          {creada && <span className="texto-suave">{creada}</span>}
        </p>
      </div>
      <button type="button" className="boton boton--primario" onClick={() => onJugar(gincana)}>
        Jugar
      </button>
    </li>
  )
}
