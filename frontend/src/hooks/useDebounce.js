import { useEffect, useState } from 'react'

/** Devuelve el valor solo cuando lleva "espera" ms sin cambiar (p. ej. al soltar el marcador o el slider). */
export default function useDebounce(valor, espera = 600) {
  const [valorEstable, setValorEstable] = useState(valor)

  useEffect(() => {
    const temporizador = setTimeout(() => setValorEstable(valor), espera)
    return () => clearTimeout(temporizador)
  }, [valor, espera])

  return valorEstable
}
