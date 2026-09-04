import { http } from './http'
import type { Servico, ServicoInput } from '@/types'

export const servicosApi = {
  listar: () => http.get<Servico[]>('/servicos'),
  criar: (input: ServicoInput) => http.post<Servico>('/servicos', input),
  atualizar: (id: string, input: ServicoInput) => http.put<Servico>(`/servicos/${id}`, input),
  deletar: (id: string) => http.delete<void>(`/servicos/${id}`),
}