import { Button } from './Button'
import { Modal } from './Modal'

interface ConfirmDialogProps {
  open: boolean
  title: string
  message: string
  confirmLabel?: string
  onConfirm: () => void
  onCancel: () => void
  /** Mensagem de erro exibida DENTRO do modal (visível acima do overlay). */
  error?: string
  /** Estado de espera: desabilita o botão e bloqueia o fechamento. */
  pending?: boolean
  pendingLabel?: string
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Confirmar',
  onConfirm,
  onCancel,
  error,
  pending = false,
  pendingLabel = 'Processando...',
}: ConfirmDialogProps) {
  return (
    <Modal open={open} title={title} onClose={onCancel} bloqueiaFechamento={pending}>
      <p className="mb-8 text-sm leading-relaxed text-ink-soft">{message}</p>
      {error && (
        <p className="mb-6 border border-red-200 bg-red-50/60 px-4 py-3 text-sm text-red-700">{error}</p>
      )}
      <div className="flex justify-end gap-3">
        <Button variant="ghost" onClick={onCancel} disabled={pending}>
          Cancelar
        </Button>
        <Button variant="danger" onClick={onConfirm} disabled={pending}>
          {pending ? pendingLabel : confirmLabel}
        </Button>
      </div>
    </Modal>
  )
}
