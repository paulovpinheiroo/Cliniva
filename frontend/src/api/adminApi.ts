import { http } from '@/api/http'
import type {
  AdicionarUsuarioRequestDTO,
  AdicionarUsuarioResponseDTO,
  AtualizarClinicaRequestDTO,
  ClinicaDetalheResponseDTO,
  CriarClinicaRequestDTO,
  CriarClinicaResponseDTO,
  ListarClinicaResponseDTO,
  MetricasAdminResponseDTO,
  ResetarSenhaResponseDTO,
} from '@/types/admin'

export const adminApi = {
  listarClinicas: () => http.get<ListarClinicaResponseDTO[]>('/admin/clinicas'),
  criarClinica: (body: CriarClinicaRequestDTO) =>
    http.post<CriarClinicaResponseDTO>('/admin/clinicas', body),
  detalharClinica: (id: string) => http.get<ClinicaDetalheResponseDTO>(`/admin/clinicas/${id}`),
  atualizarClinica: (id: string, body: AtualizarClinicaRequestDTO) =>
    http.patch<ClinicaDetalheResponseDTO>(`/admin/clinicas/${id}`, body),
  adicionarResponsavel: (id: string, body: AdicionarUsuarioRequestDTO) =>
    http.post<AdicionarUsuarioResponseDTO>(`/admin/clinicas/${id}/usuarios`, body),
  resetarSenha: (usuarioId: string) =>
    http.post<ResetarSenhaResponseDTO>(`/admin/usuarios/${usuarioId}/reset-senha`, {}),
  metricas: () => http.get<MetricasAdminResponseDTO>('/admin/metricas'),
}