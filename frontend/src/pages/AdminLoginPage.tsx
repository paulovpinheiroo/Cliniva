import { useState } from 'react'
import type { FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { AuthShell } from '@/components/auth/AuthShell'
import { Button } from '@/components/ui/Button'
import { TextField } from '@/components/ui/TextField'
import { useAuth } from '@/hooks/useAuth'
import { supabase } from '@/lib/supabase'

const ADMIN_EMAIL = 'paulovictorpinheiro998663264@gmail.com'

export function AdminLoginPage() {
  const { usuario } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState(ADMIN_EMAIL)
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  if (usuario) {
    return <Navigate to={usuario.papel === 'ADMIN' ? '/admin' : '/'} replace />
  }

  const entrar = async (evento: FormEvent) => {
    evento.preventDefault()
    const client = supabase
    if (!client) {
      setErro('Supabase não configurado. Defina VITE_SUPABASE_URL e VITE_SUPABASE_ANON_KEY.')
      return
    }
    setEnviando(true)
    setErro('')
    try {
      const { error } = await client.auth.signInWithPassword({ email, password: senha })
      if (error) throw new Error(error.message)
      navigate('/admin', { replace: true })
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Falha ao entrar.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <AuthShell
      kicker="Área restrita"
      title="Administração"
      subtitle="Acesso exclusivo do administrador da plataforma"
    >
      <form onSubmit={entrar} className="flex flex-col gap-5">
        <TextField
          label="E-mail *"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="admin@cliniva.com"
        />
        <TextField
          label="Senha *"
          type="password"
          autoComplete="current-password"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          placeholder="••••••••"
        />
        {erro && <p className="text-sm text-red-600">{erro}</p>}
        <Button type="submit" disabled={enviando} className="w-full">
          {enviando ? 'Entrando...' : 'Entrar'}
        </Button>
      </form>
    </AuthShell>
  )
}