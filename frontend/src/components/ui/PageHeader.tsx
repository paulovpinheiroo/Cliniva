import type { ReactNode } from 'react'

interface PageHeaderProps {
  title: string
  subtitle?: string
  kicker?: string
  action?: ReactNode
}

export function PageHeader({ title, subtitle, kicker, action }: PageHeaderProps) {
  return (
    <div className="mb-12 border-b border-hairline pb-10">
      <div className="grid grid-cols-12 items-end gap-6">
        <div className="col-span-12 lg:col-span-8">
          {kicker && (
            <p className="mb-3 text-[11px] font-medium uppercase tracking-[0.2em] text-accent-strong">
              {kicker}
            </p>
          )}
          <h1 className="font-display text-5xl font-medium leading-[1.05] text-ink">{title}</h1>
          {subtitle && (
            <p className="mt-4 max-w-xl text-sm leading-relaxed text-ink-soft">{subtitle}</p>
          )}
        </div>
        {action && (
          <div className="col-span-12 flex justify-end pb-1 lg:col-span-4">{action}</div>
        )}
      </div>
    </div>
  )
}