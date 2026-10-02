import { http } from './http'

export type ResumoOrigem = 'IA' | 'TEMPLATE'

export interface ResumoDoDia {
  atendimentosHoje: number
  receitaPrevista: number
  receitaRealizada: number
  novosMes: number
  recorrentesMes: number
  aniversariantesHoje: number
  itensAbaixoMinimo: string[]
  texto: string
  origem: ResumoOrigem
  geradoEm: string | null
  tentativasRestantes: number
}

export const resumoApi = {
  obter: () => http.get<ResumoDoDia>('/resumo-do-dia'),
  regenerar: () => http.post<ResumoDoDia>('/resumo-do-dia/regenerar', {}),
}