import { http } from './http'
import type { Item, ItemInput, MovimentacaoEstoqueInput } from '@/types'

export const itensApi = {
  listar: () => http.get<Item[]>('/items'),
  criar: (input: ItemInput) => http.post<Item>('/items', input),
  atualizar: (id: string, input: { nome: string }) => http.put<Item>(`/items/${id}`, input),
  movimentarEstoque: (id: string, input: MovimentacaoEstoqueInput) => http.patch<Item>(`/items/${id}/estoque`, input),
  deletar: (id: string) => http.delete<void>(`/items/${id}`),
}