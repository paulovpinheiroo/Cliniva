import { NavLink, Outlet } from 'react-router-dom'
import { useTheme } from '@/hooks/useTheme'

const navItems = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/clientes', label: 'Clientes' },
  { to: '/servicos', label: 'Serviços' },
  { to: '/estoque', label: 'Estoque' },
  { to: '/atendimentos', label: 'Atendimentos' },
]

export function AppLayout() {
  const { theme, toggleTheme } = useTheme()
  const today = new Date().toLocaleDateString('pt-BR', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  })

  return (
    <div className="flex min-h-screen bg-ivory text-ink">
      <aside className="fixed inset-y-0 left-0 z-40 flex w-60 flex-col border-r border-white/10 bg-carbon">
        <NavLink to="/" end className="flex h-20 items-center gap-3 border-b border-white/10 px-5">
          <img src="/Cliniva-Simbolo.png" alt="Símbolo Cliniva" className="h-8 w-8 object-contain" />
          <span className="font-display text-2xl text-ivory">Cliniva</span>
        </NavLink>
        <nav className="flex-1 py-6">
          {navItems.map((item, index) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `group relative flex items-center gap-4 py-2.5 pl-5 pr-3 text-[11px] font-medium uppercase tracking-[0.18em] transition-colors duration-150 ease-in-out ${
                  isActive ? 'text-accent' : 'text-ivory/45 hover:text-ivory'
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <span
                    className={`absolute inset-y-0 left-0 w-0.5 transition-colors duration-150 ease-in-out ${
                      isActive ? 'bg-accent' : 'bg-transparent group-hover:bg-ivory/30'
                    }`}
                  />
                  <span
                    className={`font-mono text-[10px] tracking-[0.2em] transition-colors duration-150 ease-in-out ${
                      isActive ? 'text-accent' : 'text-ivory/30 group-hover:text-ivory/50'
                    }`}
                  >
                    {String(index + 1).padStart(2, '0')}
                  </span>
                  <span className="transition-transform duration-150 ease-in-out group-hover:translate-x-0.5">
                    {item.label}
                  </span>
                </>
              )}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-white/10 px-5 py-5">
          <p className="font-mono text-[10px] uppercase leading-relaxed tracking-[0.18em] text-ivory/30">
            Cliniva — Gestão
          </p>
          <p className="font-mono text-[10px] uppercase leading-relaxed tracking-[0.18em] text-ivory/30">
            MVP · 2026
          </p>
        </div>
      </aside>

      <div className="ml-60 flex min-h-screen flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-hairline px-10 py-2.5">
          <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft">
            Cliniva — Sistema de gestão
          </span>
          <div className="flex items-center gap-6">
            <button
              onClick={toggleTheme}
              aria-label={theme === 'dark' ? 'Ativar tema claro' : 'Ativar tema escuro'}
              className="cursor-pointer font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft underline-offset-4 transition-colors duration-150 ease-in-out hover:text-ink hover:underline"
            >
              [ {theme === 'dark' ? 'claro' : 'escuro'} ]
            </button>
            <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft">{today}</span>
          </div>
        </header>
        <main className="flex-1 px-10 py-12">
          <div className="mx-auto w-full max-w-6xl">
            <Outlet />
          </div>
        </main>
        <footer className="border-t border-hairline px-10 py-4">
          <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-4">
            <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft">
              Cliniva · MVP 2026
            </span>
            <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft">
              Cuidado em cada detalhe
            </span>
          </div>
        </footer>
      </div>
    </div>
  )
}