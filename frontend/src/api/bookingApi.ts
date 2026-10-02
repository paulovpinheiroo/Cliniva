import { http } from './http'
import type { BookingInput, BookingResult, DisponibilidadeDia, ServicoPublico } from '@/types'

export const bookingApi = {
  listarServicos: (slug: string) => http.get<ServicoPublico[]>(`/public/booking/${slug}/servicos`),
  disponibilidade: (slug: string, data: string, servicoId: string) =>
    http.get<DisponibilidadeDia>(
      `/public/booking/${slug}/disponibilidade?data=${data}&servicoId=${servicoId}`,
    ),
  agendar: (slug: string, input: BookingInput) => http.post<BookingResult>(`/public/booking/${slug}`, input),
}