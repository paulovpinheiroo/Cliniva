export interface ListarClinicaResponseDTO {
  id: string
  nome: string
  ativa: boolean
  criadaEm: string
  totalResponsaveis: number
}

export interface CriarClinicaRequestDTO {
  nome: string
  emailResponsavel: string
  nomeResponsavel: string
}

export interface CriarClinicaResponseDTO {
  clinicaId: string
  clinicaNome: string
  emailResponsavel: string | null
  senhaTemporaria: string | null
}

export interface AtualizarClinicaRequestDTO {
  nome?: string
  ativa?: boolean
}

export interface AdicionarUsuarioRequestDTO {
  email: string
  nome: string
}

export interface AdicionarUsuarioResponseDTO {
  id: string
  email: string
  nome: string
  papel: string
  senhaTemporaria: string | null
}

export interface UsuarioResponseDTO {
  id: string
  nome: string
  email: string
  papel: string
  ativo: boolean
}

export interface ResetarSenhaResponseDTO {
  email: string
  senhaTemporaria: string
}

export interface ClinicaDetalheResponseDTO {
  id: string
  nome: string
  ativa: boolean
  criadaEm: string
  totalClientes: number
  totalAtendimentos: number
  receita: number
  responsaveis: UsuarioResponseDTO[]
}

export interface MetricaClinicaDTO {
  clinicaId: string
  clinicaNome: string
  clientes: number
  atendimentos: number
  receita: number
}

export interface MetricasAdminResponseDTO {
  totalClinicas: number
  totalClientes: number
  totalAtendimentos: number
  porClinica: MetricaClinicaDTO[]
}