import type { ClienteStatus } from '@/types'
import { clienteStatusLabel } from '@/utils/format'

const dotStyles: Record<ClienteStatus, string> = {
  PROSPECT: 'bg-lilac',
  ATIVO: 'bg-sage',
  INATIVO: 'bg-ink-soft',
}

const labelStyles: Record<ClienteStatus, string> = {
  PROSPECT: 'text-purple',
  ATIVO: 'text-sage-dark',
  INATIVO: 'text-ink-soft',
}

export function ClienteStatusBadge({ status }: { status: ClienteStatus }) {
  return (
    <span className="inline-flex items-center gap-2">
      <span className={`h-1.5 w-1.5 ${dotStyles[status]}`} />
      <span className={`text-[11px] font-medium uppercase tracking-[0.14em] ${labelStyles[status]}`}>
        {clienteStatusLabel(status)}
      </span>
    </span>
  )
}