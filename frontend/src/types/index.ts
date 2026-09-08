export type ClienteStatus = 'PROSPECT' | 'ATIVO' | 'INATIVO'

export type OrigemCliente = 'INDICACAO' | 'INSTAGRAM' | 'GOOGLE' | 'PASSOU_NA_RUA'

export type CanalPreferido = 'WHATSAPP' | 'INSTAGRAM' | 'EMAIL' | 'LIGACAO'

export interface Cliente {
  id: string
  nome: string
  email: string | null
  telefone: string
  dataNascimento: string | null
  status: ClienteStatus
  origem: OrigemCliente | null
  canalPreferido: CanalPreferido | null
  preferencias: string | null
  observacoes: string | null
}

export interface ClienteInput {
  nome: string
  email: string
  telefone: string
  dataNascimento?: string
  status?: ClienteStatus | ''
  origem?: OrigemCliente | ''
  canalPreferido?: CanalPreferido | ''
  preferencias?: string
  observacoes?: string
}

export interface ClienteNota {
  id: string
  texto: string
  criadaEm: string
}

export interface ClienteHistorico {
  cliente: Cliente
  totalAtendimentos: number
  atendimentosConcluidos: number
  gastoTotal: number
  ticketMedio: number | null
  ultimaVisita: string | null
  atendimentos: Atendimento[]
}

export interface Servico {
  id: string
  nome: string
  descricao: string | null
  valor: number
}

export interface Item {
  id: string
  nome: string
  quantidadeEmEstoque: number
}

export type StatusAtendimento = 'AGENDADO' | 'CONCLUIDO' | 'CANCELADO'

export interface AtendimentoResumo {
  id: string
  clienteId: string
  nomeCliente: string
  telefoneCliente: string
  dataAtendimento: string
  status: StatusAtendimento
  valorTotal: number
}

export interface AtendimentoItem {
  itemId: string
  nomeItem: string
  quantidadeUsada: number
}

export interface AtendimentoServico {
  servicoId: string
  nomeServico: string
  valorCobrado: number
  itensUsados: AtendimentoItem[]
}

export interface Atendimento {
  id: string
  clienteId: string
  nomeCliente: string
  dataAtendimento: string
  dataCriacao: string
  status: StatusAtendimento
  valorTotal: number
  servicos: AtendimentoServico[]
}

export interface ServicoInput {
  nome: string
  descricao: string
  valor: number
}

export interface ItemInput {
  nome: string
  quantidadeEmEstoque: number
}

export type TipoMovimentacao = 'ENTRADA' | 'SAIDA'

export interface MovimentacaoEstoqueInput {
  tipo: TipoMovimentacao
  quantidade: number
}

export interface AtendimentoFiltros {
  status?: StatusAtendimento
  clienteId?: string
  dataInicio?: string
  dataFim?: string
}

export interface AtendimentoItemExtraInput {
  itemId: string
  quantidade: number
}

export interface AtendimentoServicoInput {
  servicoId: string
  itensExtras: AtendimentoItemExtraInput[]
}

export interface AtendimentoInput {
  clienteId: string
  dataAtendimento: string
  servicos: AtendimentoServicoInput[]
}