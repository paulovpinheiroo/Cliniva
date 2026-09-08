const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
const data = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' })
const dataLonga = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'long' })

export function formatMoeda(valor: number): string {
  return moeda.format(valor)
}

export function formatDataHora(iso: string): string {
  return dataHora.format(new Date(iso))
}

export function formatData(iso: string): string {
  return data.format(new Date(iso))
}

export function formatDataLonga(iso: string): string {
  return dataLonga.format(new Date(iso))
}

const STATUS_LABEL: Record<string, string> = {
  AGENDADO: 'Agendado',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
}

export function statusLabel(status: string): string {
  return STATUS_LABEL[status] ?? status
}

const CLIENTE_STATUS_LABEL: Record<string, string> = {
  PROSPECT: 'Prospect',
  ATIVO: 'Ativo',
  INATIVO: 'Inativo',
}

export function clienteStatusLabel(status: string): string {
  return CLIENTE_STATUS_LABEL[status] ?? status
}

const ORIGEM_LABEL: Record<string, string> = {
  INDICACAO: 'Indicação',
  INSTAGRAM: 'Instagram',
  GOOGLE: 'Google',
  PASSOU_NA_RUA: 'Passou na rua',
}

export function origemLabel(origem: string): string {
  return ORIGEM_LABEL[origem] ?? origem
}

const CANAL_LABEL: Record<string, string> = {
  WHATSAPP: 'WhatsApp',
  INSTAGRAM: 'Instagram',
  EMAIL: 'E-mail',
  LIGACAO: 'Ligação',
}

export function canalLabel(canal: string): string {
  return CANAL_LABEL[canal] ?? canal
}

export function whatsappLink(telefone: string, mensagem = ''): string {
  const digitos = telefone.replace(/\D/g, '')
  const comDDI = digitos.startsWith('55') ? digitos : `55${digitos}`
  const texto = mensagem ? `?text=${encodeURIComponent(mensagem)}` : ''
  return `https://wa.me/${comDDI}${texto}`
}

export function fidelidadeLabel(atendimentosConcluidos: number): string {
  if (atendimentosConcluidos >= 3) return 'Frequente'
  if (atendimentosConcluidos >= 1) return 'Recorrente'
  return 'Novo'
}

export function fidelidadeCor(atendimentosConcluidos: number): string {
  if (atendimentosConcluidos >= 3) return 'text-sage-dark'
  if (atendimentosConcluidos >= 1) return 'text-ink'
  return 'text-ink-soft'
}