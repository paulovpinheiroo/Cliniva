import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { ApiError, http } from '@/api/http'
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

interface ErroCarregamento {
  status: number | null
  mensagem: string
}

interface AuthContextValue {
  usuario: UsuarioLogado | null
  carregando: boolean
  erroCarregamento: ErroCarregamento | null
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
  const [erroCarregamento, setErroCarregamento] = useState<ErroCarregamento | null>(null)
  const sequenciaRef = useRef(0)

  const carregarUsuario = useCallback(async (): Promise<UsuarioLogado | null> => {
    const sequencia = ++sequenciaRef.current
    setCarregando(true)
    setErroCarregamento(null)
    try {
      const dados = await http.get<MeResponseDTO>('/me')
      if (sequencia !== sequenciaRef.current) return null
      const logado: UsuarioLogado = dados
      setUsuario(logado)
      sessionStore.setClinicaId(logado.clinicaId)
      return logado
    } catch (err) {
      if (sequencia !== sequenciaRef.current) return null
      console.error('Falha ao carregar o usuário logado:', err)
      setUsuario(null)
      setErroCarregamento({
        status: err instanceof ApiError ? err.status : null,
        mensagem: err instanceof Error ? err.message : 'Falha ao carregar o usuário.',
      })
      return null
    } finally {
      if (sequencia === sequenciaRef.current) setCarregando(false)
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
      } else {
        setCarregando(false)
      }
    })

    const { data: inscricao } = supabase.auth.onAuthStateChange((evento, sessao) => {
      if (evento === 'SIGNED_IN' || evento === 'TOKEN_REFRESHED') {
        sessionStore.setToken(sessao?.access_token ?? null)
        carregarUsuario()
      } else if (evento === 'SIGNED_OUT') {
        sequenciaRef.current += 1
        aplicarNoLocalStorage(null, null)
        setUsuario(null)
        setErroCarregamento(null)
        if (ativo) setCarregando(false)
      }
    })

    return () => {
      ativo = false
      sequenciaRef.current += 1
      inscricao.subscription.unsubscribe()
    }
  }, [carregarUsuario])

  const sair = useCallback(async () => {
    if (supabase) {
      await supabase.auth.signOut()
    }
    sequenciaRef.current += 1
    aplicarNoLocalStorage(null, null)
    setUsuario(null)
    setErroCarregamento(null)
  }, [])

  const valor = useMemo(
    () => ({ usuario, carregando, erroCarregamento, sair }),
    [usuario, carregando, erroCarregamento, sair],
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