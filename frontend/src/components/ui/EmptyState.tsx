interface EmptyStateProps {
  message: string
}

export function EmptyState({ message }: EmptyStateProps) {
  return (
    <div className="border-t border-hairline py-12">
      <p className="font-display text-2xl italic text-ink-soft/80">em branco</p>
      <p className="mt-2 max-w-sm text-sm leading-relaxed text-ink-soft">{message}</p>
    </div>
  )
}