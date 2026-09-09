import type { ReactNode } from 'react'

interface AuthShellProps {
  kicker: string
  title: string
  subtitle?: string
  children: ReactNode
}

export function AuthShell({ kicker, title, subtitle, children }: AuthShellProps) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-ivory px-4 py-12">
      <div className="w-full max-w-md">
        <div className="mb-8 flex items-center gap-3">
          <img src="/Cliniva-Simbolo.png" alt="Símbolo Clíniva" className="h-9 w-9 object-contain" />
          <span className="font-display text-3xl text-ink">Clíniva</span>
        </div>
        <p className="mb-3 text-[11px] font-medium uppercase tracking-[0.2em] text-accent-strong">{kicker}</p>
        <h1 className="font-display text-4xl font-medium leading-[1.05] text-ink lg:text-5xl">{title}</h1>
        {subtitle && <p className="mt-4 text-sm leading-relaxed text-ink-soft">{subtitle}</p>}
        <div className="mt-10 border border-hairline bg-paper p-6 lg:p-8">{children}</div>
        <p className="mt-6 font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft">
          Clíniva · MVP 2026
        </p>
      </div>
    </div>
  )
}