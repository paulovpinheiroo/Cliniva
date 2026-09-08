import type { ReactNode } from 'react'

interface CardListProps {
  children: ReactNode
  className?: string
}

export function CardList({ children }: CardListProps) {
  return <div className="flex flex-col divide-y divide-hairline border-y border-hairline md:hidden">{children}</div>
}

export function CardItem({
  children,
  className = '',
  onClick,
}: {
  children: ReactNode
  className?: string
  onClick?: () => void
}) {
  return (
    <div className={`px-4 py-4 ${onClick ? 'cursor-pointer' : ''} ${className}`} onClick={onClick}>
      {children}
    </div>
  )
}

export function CardActions({ children }: { children: ReactNode }) {
  return <div className="mt-3 flex flex-wrap justify-end gap-x-4 gap-y-1">{children}</div>
}

export function CardLabel({ children }: { children: ReactNode }) {
  return <span className="block truncate text-sm font-medium text-ink">{children}</span>
}

export function CardDetail({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <span
      className={`mt-0.5 block truncate font-mono text-[11px] uppercase tracking-[0.14em] text-ink-soft ${className}`}
    >
      {children}
    </span>
  )
}