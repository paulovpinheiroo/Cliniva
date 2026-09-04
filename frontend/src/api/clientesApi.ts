import { http } from './http'
import type { Cliente, ClienteInput } from '@/types'

export const clientesApi = {
  listar: (nome?: string) => {
    const query = nome ? `?nome=${encodeURIComponent(nome)}` : ''
    return http.get<Cliente[]>(`/clientes${query}`)
  },
  criar: (input: ClienteInput) => http.post<Cliente>('/clientes', input),
  atualizar: (id: string, input: ClienteInput) => http.put<Cliente>(`/clientes/${id}`, input),
  deletar: (id: string) => http.delete<void>(`/clientes/${id}`),
}