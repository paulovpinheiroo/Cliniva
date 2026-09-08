import { http } from './http'
import type { Cliente, ClienteHistorico, ClienteInput, ClienteNota, ClienteStatus } from '@/types'

export const clientesApi = {
  listar: (nome?: string, status?: ClienteStatus) => {
    const params = new URLSearchParams()
    if (nome) params.set('nome', nome)
    if (status) params.set('status', status)
    const query = params.toString() ? `?${params}` : ''
    return http.get<Cliente[]>(`/clientes${query}`)
  },
  criar: (input: ClienteInput) => http.post<Cliente>('/clientes', input),
  atualizar: (id: string, input: ClienteInput) => http.put<Cliente>(`/clientes/${id}`, input),
  deletar: (id: string) => http.delete<void>(`/clientes/${id}`),
  historico: (id: string) => http.get<ClienteHistorico>(`/clientes/${id}/historico`),
  aniversariantes: (mes?: number) => {
    const query = mes ? `?mes=${mes}` : ''
    return http.get<Cliente[]>(`/clientes/aniversariantes${query}`)
  },
  listarNotas: (id: string) => http.get<ClienteNota[]>(`/clientes/${id}/notas`),
  criarNota: (id: string, texto: string) =>
    http.post<ClienteNota>(`/clientes/${id}/notas`, { texto }),
  deletarNota: (id: string, notaId: string) => http.delete<void>(`/clientes/${id}/notas/${notaId}`),
}