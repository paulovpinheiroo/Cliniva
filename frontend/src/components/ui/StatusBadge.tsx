import type { StatusAtendimento } from '@/types'
import { statusLabel } from '@/utils/format'

const styles: Record<StatusAtendimento, string> = {
  AGENDADO: 'bg-purple text-lilac',
  CONCLUIDO: 'bg-sage/20 text-sage',
  CANCELADO: 'bg-red-500/20 text-red-400',
}

export function StatusBadge({ status }: { status: StatusAtendimento }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${styles[status]}`}>
      {statusLabel(status)}
    </span>
  )
}