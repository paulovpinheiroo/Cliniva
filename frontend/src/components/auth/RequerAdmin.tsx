import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { Spinner } from '@/components/ui/Spinner'
import { useAuth } from '@/hooks/useAuth'

export function RequerAdmin({ children }: { children: ReactNode }) {
  const { usuario, carregando } = useAuth()

  if (carregando) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-ivory">
        <Spinner />
      </div>
    )
  }

  if (!usuario) return <Navigate to="/login" replace />
  if (usuario.papel !== 'ADMIN') return <Navigate to="/" replace />

  return children
}