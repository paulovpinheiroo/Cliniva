import { useCallback, useEffect, useRef, useState } from 'react'

interface UseApiResult<T> {
  data: T | null
  loading: boolean
  error: string | null
  refetch: () => Promise<void>
}

export function useApi<T>(fetcher: () => Promise<T>, deps: unknown[] = []): UseApiResult<T> {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetcherRef = useRef(fetcher)
  fetcherRef.current = fetcher

  const execute = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setData(await fetcherRef.current())
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro inesperado')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    execute()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  return { data, loading, error, refetch: execute }
}