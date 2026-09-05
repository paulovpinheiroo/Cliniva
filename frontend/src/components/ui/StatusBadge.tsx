import type { StatusAtendimento } from '@/types'
import { statusLabel } from '@/utils/format'

const dotStyles: Record<StatusAtendimento, string> = {
  AGENDADO: 'bg-lilac',
  CONCLUIDO: 'bg-sage',
  CANCELADO: 'bg-ink-soft',
}

const labelStyles: Record<StatusAtendimento, string> = {
  AGENDADO: 'text-purple',
  CONCLUIDO: 'text-sage-dark',
  CANCELADO: 'text-ink-soft line-through decoration-ink-soft/60',
}

export function StatusBadge({ status }: { status: StatusAtendimento }) {
  return (
    <span className="inline-flex items-center gap-2">
      <span className={`h-1.5 w-1.5 ${dotStyles[status]}`} />
      <span className={`text-[11px] font-medium uppercase tracking-[0.14em] ${labelStyles[status]}`}>
        {statusLabel(status)}
      </span>
    </span>
  )
}