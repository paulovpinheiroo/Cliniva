import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { http } from '@/api/http'
import { AuthShell } from '@/components/auth/AuthShell'
import { Button } from '@/components/ui/Button'
import { TextField } from '@/components/ui/TextField'
import { useAuth } from '@/hooks/useAuth'
import { supabase } from '@/lib/supabase'

interface OnboardingResposta {
  clinicaId: string
  clinicaNome: string
  responsavelId: string
}

export function CadastroPage() {
  const { usuario } = useAuth()
  const navigate = useNavigate()

  const [nomeClinica, setNomeClinica] = useState('')
  const [nomeResponsavel, setNomeResponsavel] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [confirmaSenha, setConfirmaSenha] = useState('')
  const [erro, setErro] = useState('')
  const [aguardandoConfirmacao, setAguardandoConfirmacao] = useState(false)
  const [enviando, setEnviando] = useState(false)

  if (usuario) return <Navigate to="/" replace />

  const cadastrar = async (evento: FormEvent) => {
    evento.preventDefault()
    const client = supabase
    if (!client) {
      setErro('Supabase não configurado. Defina VITE_SUPABASE_URL e VITE_SUPABASE_ANON_KEY.')
      return
    }
    if (senha.length < 6) {
      setErro('A senha deve ter pelo menos 6 caracteres.')
      return
    }
    if (senha !== confirmaSenha) {
      setErro('As senhas não coincidem.')
      return
    }
    setEnviando(true)
    setErro('')
    try {
      const { data, error } = await client.auth.signUp({ email, password: senha })
      if (error) throw new Error(error.message)

      await http.post<OnboardingResposta>('/public/onboarding', {
        nomeClinica,
        nomeResponsavel,
        email,
      })

      if (data.session) {
        navigate('/', { replace: true })
      } else {
        setAguardandoConfirmacao(true)
      }
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Falha ao cadastrar a clínica.')
    } finally {
      setEnviando(false)
    }
  }

  if (aguardandoConfirmacao) {
    return (
      <AuthShell
        kicker="Cadastro"
        title="Quase lá"
        subtitle={`Enviamos um link de confirmação para ${email}. Confirme seu e-mail e depois entre para começar a usar a Clíniva.`}
      >
        <Button variant="secondary" onClick={() => navigate('/login')} className="w-full">
          Ir para o login
        </Button>
      </AuthShell>
    )
  }

  return (
    <AuthShell
      kicker="Cadastro"
      title="Criar clínica"
      subtitle="Cadastre sua clínica e comece a organizar clientes, serviços e atendimentos"
    >
      <form onSubmit={cadastrar} className="flex flex-col gap-5">
        <TextField
          label="Nome da clínica *"
          value={nomeClinica}
          onChange={(e) => setNomeClinica(e.target.value)}
          placeholder="Ex.: Clínica Vida"
        />
        <TextField
          label="Nome do responsável *"
          value={nomeResponsavel}
          onChange={(e) => setNomeResponsavel(e.target.value)}
          placeholder="Seu nome completo"
        />
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
          autoComplete="new-password"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          placeholder="Mínimo 6 caracteres"
        />
        <TextField
          label="Confirmar senha *"
          type="password"
          autoComplete="new-password"
          value={confirmaSenha}
          onChange={(e) => setConfirmaSenha(e.target.value)}
          placeholder="Repita a senha"
        />
        {erro && <p className="text-sm text-red-600">{erro}</p>}
        <Button type="submit" disabled={enviando} className="w-full">
          {enviando ? 'Cadastrando...' : 'Criar clínica'}
        </Button>
      </form>
      <p className="mt-6 text-sm text-ink-soft">
        Já possui uma conta?{' '}
        <Link to="/login" className="text-accent-strong underline-offset-4 hover:underline">
          Entrar
        </Link>
      </p>
    </AuthShell>
  )
}