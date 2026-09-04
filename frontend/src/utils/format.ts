const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
const data = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' })

export function formatMoeda(valor: number): string {
  return moeda.format(valor)
}

export function formatDataHora(iso: string): string {
  return dataHora.format(new Date(iso))
}

export function formatData(iso: string): string {
  return data.format(new Date(iso))
}

const STATUS_LABEL: Record<string, string> = {
  AGENDADO: 'Agendado',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
}

export function statusLabel(status: string): string {
  return STATUS_LABEL[status] ?? status
}