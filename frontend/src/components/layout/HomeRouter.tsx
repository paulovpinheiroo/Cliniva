import { Navigate } from 'react-router-dom'
import { Spinner } from '@/components/ui/Spinner'
import { useAuth } from '@/hooks/useAuth'
import { LandingPage } from '@/pages/LandingPage'

export function HomeRouter() {
  const { usuario, carregando } = useAuth()

  if (carregando) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-ivory">
        <Spinner />
      </div>
    )
  }

  if (usuario) {
    return <Navigate to="/dashboard" replace />
  }

  return <LandingPage />
}