import { http } from './http'
import type { AgendaItem, AgendaLink, DisponibilidadeDia, HorarioAtendimento } from '@/types'

export const agendaApi = {
  listarDia: (data: string) => http.get<AgendaItem[]>(`/agenda?data=${data}`),
  linkPublico: () => http.get<AgendaLink>('/agenda/link'),
  disponibilidade: (data: string, servicoId: string) =>
    http.get<DisponibilidadeDia>(`/agenda/disponibilidade?data=${data}&servicoId=${servicoId}`),
  listarHorarios: () => http.get<HorarioAtendimento[]>('/agenda/horarios'),
  atualizarHorarios: (horarios: HorarioAtendimento[]) =>
    http.put<HorarioAtendimento[]>('/agenda/horarios', horarios),
}