import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthShell } from '@/components/auth/AuthShell'
import { Button } from '@/components/ui/Button'
import { TextField } from '@/components/ui/TextField'
import { supabase } from '@/lib/supabase'

export function TrocarSenhaPage() {
  const navigate = useNavigate()
  const [senha, setSenha] = useState('')
  const [confirmaSenha, setConfirmaSenha] = useState('')
  const [erro, setErro] = useState('')
  const [alterada, setAlterada] = useState(false)
  const [enviando, setEnviando] = useState(false)

  const salvar = async (evento: FormEvent) => {
    evento.preventDefault()
    const client = supabase
    if (!client) {
      setErro('Supabase não configurado. Defina VITE_SUPABASE_URL e VITE_SUPABASE_ANON_KEY.')
      return
    }
    if (senha.length < 6) {
      setErro('A nova senha deve ter pelo menos 6 caracteres.')
      return
    }
    if (senha !== confirmaSenha) {
      setErro('As senhas não coincidem.')
      return
    }
    setEnviando(true)
    setErro('')
    try {
      const { error } = await client.auth.updateUser({ password: senha })
      if (error) throw new Error(error.message)
      setAlterada(true)
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Falha ao alterar a senha.')
    } finally {
      setEnviando(false)
    }
  }

  if (alterada) {
    return (
      <AuthShell
        kicker="Segurança"
        title="Senha alterada"
        subtitle="Sua senha foi atualizada com sucesso. Use a nova senha no próximo acesso."
      >
        <Button onClick={() => navigate('/', { replace: true })} className="w-full">
          Voltar ao painel
        </Button>
      </AuthShell>
    )
  }

  return (
    <AuthShell
      kicker="Segurança"
      title="Trocar senha"
      subtitle="Defina uma nova senha para a sua conta"
    >
      <form onSubmit={salvar} className="flex flex-col gap-5">
        <TextField
          label="Nova senha *"
          type="password"
          autoComplete="new-password"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          placeholder="Mínimo 6 caracteres"
        />
        <TextField
          label="Confirmar nova senha *"
          type="password"
          autoComplete="new-password"
          value={confirmaSenha}
          onChange={(e) => setConfirmaSenha(e.target.value)}
          placeholder="Repita a nova senha"
        />
        {erro && <p className="text-sm text-red-600">{erro}</p>}
        <Button type="submit" disabled={enviando} className="w-full">
          {enviando ? 'Salvando...' : 'Salvar senha'}
        </Button>
      </form>
      <p className="mt-6 text-sm text-ink-soft">
        Prefere continuar depois?{' '}
        <Link to="/" className="text-accent-strong underline-offset-4 hover:underline">
          Ir para o painel
        </Link>
      </p>
    </AuthShell>
  )
}