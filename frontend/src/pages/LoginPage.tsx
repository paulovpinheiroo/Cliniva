import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { AuthShell } from '@/components/auth/AuthShell'
import { Button } from '@/components/ui/Button'
import { TextField } from '@/components/ui/TextField'
import { useAuth } from '@/hooks/useAuth'
import { supabase } from '@/lib/supabase'

export function LoginPage() {
  const { usuario, carregando, erroCarregamento } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/'

  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    if (usuario && !carregando) navigate(from, { replace: true })
  }, [usuario, carregando, from, navigate])

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
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Falha ao entrar.')
    } finally {
      setEnviando(false)
    }
  }

  const erroFinal =
    erro ||
    (erroCarregamento?.status === 401 || erroCarregamento?.status === 403
      ? 'Sua conta não está vinculada a uma clínica cadastrada. Crie uma clínica em "Cadastre-se" ou entre em contato com o suporte.'
      : erroCarregamento?.mensagem) ||
    ''

  return (
    <AuthShell kicker="Acesso" title="Entrar" subtitle="Acesse sua conta para gerenciar a clínica">
      <form onSubmit={entrar} className="flex flex-col gap-5">
        <TextField
          label="E-mail *"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="voce@cliniva.com"
        />
        <TextField
          label="Senha *"
          type="password"
          autoComplete="current-password"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          placeholder="••••••••"
        />
        {erroFinal && <p className="text-sm text-red-600">{erroFinal}</p>}
        <Button type="submit" disabled={enviando} className="w-full">
          {enviando ? 'Entrando...' : 'Entrar'}
        </Button>
      </form>
      <p className="mt-6 text-sm text-ink-soft">
        Ainda não tem uma clínica?{' '}
        <Link to="/cadastro" className="text-accent-strong underline-offset-4 hover:underline">
          Cadastre-se
        </Link>
      </p>
    </AuthShell>
  )
}