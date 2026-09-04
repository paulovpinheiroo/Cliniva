interface EmptyStateProps {
  message: string
}

export function EmptyState({ message }: EmptyStateProps) {
  return (
    <div className="rounded-xl border border-dashed border-borderline p-10 text-center text-sm text-slate-400">
      {message}
    </div>
  )
}