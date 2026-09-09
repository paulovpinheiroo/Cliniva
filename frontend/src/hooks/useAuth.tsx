import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { http } from '@/api/http'
import { supabase } from '@/lib/supabase'
import { sessionStore } from '@/lib/session'

export interface UsuarioLogado {
  id: string
  nome: string | null
  email: string
  papel: 'ADMIN' | 'OWNER'
  clinicaId: string | null
  clinicaNome: string | null
}

interface AuthContextValue {
  usuario: UsuarioLogado | null
  carregando: boolean
  sair: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

interface MeResponseDTO {
  id: string
  nome: string | null
  email: string
  papel: 'ADMIN' | 'OWNER'
  clinicaId: string | null
  clinicaNome: string | null
}

function aplicarNoLocalStorage(token: string | null, clinicaId: string | null) {
  sessionStore.setToken(token)
  sessionStore.setClinicaId(clinicaId)
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioLogado | null>(null)
  const [carregando, setCarregando] = useState(true)

  const carregarUsuario = useCallback(async (): Promise<UsuarioLogado | null> => {
    try {
      const dados = await http.get<MeResponseDTO>('/me')
      const logado: UsuarioLogado = dados
      setUsuario(logado)
      sessionStore.setClinicaId(logado.clinicaId)
      return logado
    } catch {
      setUsuario(null)
      return null
    }
  }, [])

  useEffect(() => {
    if (!supabase) {
      setCarregando(false)
      return
    }

    let ativo = true

    supabase.auth.getSession().then(({ data }) => {
      if (!ativo) return
      if (data.session) {
        sessionStore.setToken(data.session.access_token)
        carregarUsuario()
      }
      setCarregando(false)
    })

    const { data: inscricao } = supabase.auth.onAuthStateChange((evento, sessao) => {
      if (evento === 'SIGNED_IN' || evento === 'TOKEN_REFRESHED') {
        sessionStore.setToken(sessao?.access_token ?? null)
        carregarUsuario()
      } else if (evento === 'SIGNED_OUT') {
        aplicarNoLocalStorage(null, null)
        setUsuario(null)
      }
    })

    return () => {
      ativo = false
      inscricao.subscription.unsubscribe()
    }
  }, [carregarUsuario])

  const sair = useCallback(async () => {
    if (supabase) {
      await supabase.auth.signOut()
    }
    aplicarNoLocalStorage(null, null)
    setUsuario(null)
  }, [])

  const valor = useMemo(
    () => ({ usuario, carregando, sair }),
    [usuario, carregando, sair],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const contexto = useContext(AuthContext)
  if (!contexto) {
    throw new Error('useAuth deve ser usado dentro de AuthProvider')
  }
  return contexto
}