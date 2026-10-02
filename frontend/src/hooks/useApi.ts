import { useCallback, useEffect, useRef, useState } from 'react'

interface UseApiResult<T> {
  data: T | null
  loading: boolean
  error: string | null
  refetch: () => Promise<void>
}

/**
 * Busca de dados com proteção contra respostas fora de ordem: cada execução
 * recebe um id e só a mais recente pode aplicar `data`/`error`/`loading`.
 * Isso evita que uma resposta antiga (ex.: trocar de serviço/data
 * rapidamente no booking) sobrescreva a tela com o resultado errado.
 *
 * Ao mudar as dependências os dados anteriores são limpos, para nunca
 * misturar "horários do dia anterior" com a tela atual. Já no `refetch()`
 * manual (após criar/editar) os dados são preservados para não piscar a lista.
 */
export function useApi<T>(fetcher: () => Promise<T>, deps: unknown[] = []): UseApiResult<T> {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetcherRef = useRef(fetcher)
  fetcherRef.current = fetcher
  const requestIdRef = useRef(0)

  const run = useCallback(async (limparDados: boolean) => {
    const requestId = ++requestIdRef.current
    setLoading(true)
    setError(null)
    if (limparDados) {
      setData(null)
    }
    try {
      const resultado = await fetcherRef.current()
      if (requestId === requestIdRef.current) {
        setData(resultado)
      }
    } catch (err) {
      if (requestId === requestIdRef.current) {
        setError(err instanceof Error ? err.message : 'Erro inesperado')
      }
    } finally {
      if (requestId === requestIdRef.current) {
        setLoading(false)
      }
    }
  }, [])

  useEffect(() => {
    void run(true)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  const refetch = useCallback(() => run(false), [run])

  return { data, loading, error, refetch }
}
