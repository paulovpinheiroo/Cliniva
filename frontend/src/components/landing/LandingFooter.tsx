export function LandingFooter() {
  return (
    <footer className="border-t border-hairline">
      <div className="mx-auto flex w-full max-w-6xl flex-col items-start justify-between gap-3 px-4 py-6 md:flex-row md:items-center md:px-6 lg:px-10">
        <span className="font-display text-xl text-ink">Clíniva</span>
        <div className="flex flex-wrap items-center gap-x-5 gap-y-1">
          <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft">
            Sistema de gestão para clínicas de estética
          </span>
          <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft">
            © 2026
          </span>
        </div>
      </div>
    </footer>
  )
}