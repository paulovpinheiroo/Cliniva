import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import type { ReactNode } from 'react'

interface ModalProps {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
  index?: number
}

export function Modal({ open, title, onClose, children, index }: ModalProps) {
  useEffect(() => {
    if (!open) return
    const handler = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [open, onClose])

  useEffect(() => {
    if (!open) return
    document.documentElement.classList.add('cdk-blur-open')
    return () => document.documentElement.classList.remove('cdk-blur-open')
  }, [open])

  if (!open) return null

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true">
      <div className="absolute inset-0" onClick={onClose} />
      <div className="relative w-full max-w-lg animate-rise border border-hairline bg-paper p-8">
        <div className="mb-6 flex items-start justify-between gap-4 border-b border-hairline pb-4">
          <div>
            {index !== undefined && (
              <p className="mb-1 font-mono text-[11px] tracking-[0.2em] text-ink-soft">
                № {String(index).padStart(2, '0')}
              </p>
            )}
            <h2 className="font-display text-2xl font-medium text-ink">{title}</h2>
          </div>
          <button
            onClick={onClose}
            aria-label="Fechar"
            className="cursor-pointer text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft underline-offset-4 transition-colors duration-150 ease-in-out hover:text-ink hover:underline"
          >
            fechar
          </button>
        </div>
        {children}
      </div>
    </div>,
    document.body,
  )
}