import { useEffect, useRef, useState } from 'react'

const PAUSA_TRAS_ACIERTO_MS = 1200

/**
 * Pista del reto, caja de respuesta y botón "Comprobar".
 * onComprobar(respuesta) devuelve true/false; onAcertado se llama tras enseñar "¡Correcto!".
 * Se monta de nuevo para cada reto (key = id del reto), así el texto empieza vacío.
 */
export default function TarjetaReto({ reto, onComprobar, onAcertado }) {
  const [respuesta, setRespuesta] = useState('')
  const [estado, setEstado] = useState('escribiendo') // escribiendo | comprobando | incorrecto | correcto
  const [error, setError] = useState(null)
  const temporizador = useRef(null)

  useEffect(() => () => clearTimeout(temporizador.current), [])

  async function comprobar(e) {
    e.preventDefault()
    if (!respuesta.trim() || estado === 'comprobando' || estado === 'correcto') return
    setEstado('comprobando')
    setError(null)
    try {
      const correcto = await onComprobar(respuesta)
      if (correcto) {
        setEstado('correcto')
        temporizador.current = setTimeout(onAcertado, PAUSA_TRAS_ACIERTO_MS)
      } else {
        setEstado('incorrecto')
      }
    } catch (err) {
      setError(err.message)
      setEstado('escribiendo')
    }
  }

  const bloqueado = estado === 'comprobando' || estado === 'correcto'

  return (
    <form className="tarjeta tarjeta-reto" onSubmit={comprobar}>
      <span className="etiqueta">Pista</span>
      <p className="tarjeta-reto__pista">{reto.pista}</p>

      <label className="campo">
        <span className="visualmente-oculto">Tu respuesta</span>
        <input
          type="text"
          value={respuesta}
          onChange={(e) => {
            setRespuesta(e.target.value)
            if (estado === 'incorrecto') setEstado('escribiendo')
          }}
          placeholder="¿Qué lugar es?"
          autoComplete="off"
          autoCapitalize="sentences"
          enterKeyHint="send"
          disabled={bloqueado}
          aria-invalid={estado === 'incorrecto'}
        />
      </label>

      {estado === 'incorrecto' && (
        <p className="aviso aviso--error" role="alert">
          No es correcto, inténtalo otra vez.
        </p>
      )}
      {estado === 'correcto' && (
        <p className="aviso aviso--exito" role="status">
          ¡Correcto! 🎉
        </p>
      )}
      {error && (
        <p className="aviso aviso--error" role="alert">
          {error}
        </p>
      )}

      <button type="submit" className="boton boton--primario boton--bloque" disabled={!respuesta.trim() || bloqueado}>
        {estado === 'comprobando' ? (
          <>
            <span className="spinner spinner--pequeno" aria-hidden="true" /> Comprobando…
          </>
        ) : (
          'Comprobar'
        )}
      </button>
    </form>
  )
}
