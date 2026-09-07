import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useTheme } from '@/hooks/useTheme'

const navItems = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/clientes', label: 'Clientes' },
  { to: '/servicos', label: 'Serviços' },
  { to: '/estoque', label: 'Estoque' },
  { to: '/atendimentos', label: 'Atendimentos' },
]

function PageTransition() {
  const location = useLocation()
  return (
    <div key={location.pathname} className="animate-rise">
      <Outlet />
    </div>
  )
}

export function AppLayout() {
  const { theme, toggleTheme } = useTheme()
  const [menuAberto, setMenuAberto] = useState(false)

  useEffect(() => {
    const fechar = () => setMenuAberto(false)
    window.addEventListener('popstate', fechar)
    return () => window.removeEventListener('popstate', fechar)
  }, [])

  useEffect(() => {
    if (!menuAberto) return
    const handler = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setMenuAberto(false)
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [menuAberto])

  const hoje = new Date()
  const dataLonga = hoje.toLocaleDateString('pt-BR', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  })
  const dataCurta = hoje.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
  })

  const mono =
    'font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft underline-offset-4 transition-colors duration-150 ease-in-out hover:text-ink hover:underline'

  return (
    <div className="flex min-h-screen bg-ivory text-ink">
      {menuAberto && (
        <div
          className="fixed inset-0 z-40 animate-fade bg-carbon/40 backdrop-blur-sm lg:hidden"
          onClick={() => setMenuAberto(false)}
        />
      )}

      <aside
        className={[
          'fixed inset-y-0 left-0 z-50 flex w-60 flex-col border-r border-white/10 bg-carbon',
          'transition-transform duration-150 ease-in-out',
          menuAberto ? 'translate-x-0' : '-translate-x-full',
          'lg:translate-x-0',
        ].join(' ')}
      >
        <NavLink to="/" end className="flex h-20 items-center gap-3 border-b border-white/10 px-5">
          <img src="/Cliniva-Simbolo.png" alt="Símbolo Clíniva" className="h-8 w-8 object-contain" />
          <span className="font-display text-2xl text-bone">Clíniva</span>
        </NavLink>
        <nav className="flex-1 py-6">
          {navItems.map((item, index) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={() => setMenuAberto(false)}
              className={({ isActive }) =>
                `group relative flex items-center gap-4 py-2.5 pl-5 pr-3 text-[11px] font-medium uppercase tracking-[0.18em] transition-colors duration-150 ease-in-out ${
                  isActive ? 'text-accent' : 'text-bone/45 hover:text-bone'
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <span
                    className={`absolute inset-y-0 left-0 w-0.5 transition-colors duration-150 ease-in-out ${
                      isActive ? 'bg-accent' : 'bg-transparent group-hover:bg-bone/30'
                    }`}
                  />
                  <span
                    className={`font-mono text-[10px] tracking-[0.2em] transition-colors duration-150 ease-in-out ${
                      isActive ? 'text-accent' : 'text-bone/30 group-hover:text-bone/50'
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
          <p className="font-mono text-[10px] uppercase leading-relaxed tracking-[0.18em] text-bone/30">
            Clíniva — Gestão
          </p>
          <p className="font-mono text-[10px] uppercase leading-relaxed tracking-[0.18em] text-bone/30">
            MVP · 2026
          </p>
        </div>
      </aside>

      <div className="flex min-h-screen flex-1 flex-col lg:ml-60">
        <header className="flex items-center justify-between gap-4 border-b border-hairline px-4 py-2.5 lg:px-10">
          <div className="flex min-w-0 items-center gap-4">
            <button
              onClick={() => setMenuAberto(true)}
              aria-label="Abrir menu"
              className={`cursor-pointer lg:hidden ${mono}`}
            >
              [ menu ]
            </button>
            <span className="hidden truncate font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft lg:inline">
              Clíniva — Sistema de gestão
            </span>
          </div>
          <div className="flex shrink-0 items-center gap-4 lg:gap-6">
            <button
              onClick={toggleTheme}
              aria-label={theme === 'dark' ? 'Ativar tema claro' : 'Ativar tema escuro'}
              className={`cursor-pointer ${mono}`}
            >
              [ {theme === 'dark' ? 'claro' : 'escuro'} ]
            </button>
            <span className="hidden font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft md:inline">
              {dataLonga}
            </span>
            <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft md:hidden">
              {dataCurta}
            </span>
          </div>
        </header>
        <main className="flex-1 px-4 py-8 md:px-6 lg:px-10 lg:py-12">
          <div className="mx-auto w-full max-w-6xl">
            <PageTransition />
          </div>
        </main>
        <footer className="border-t border-hairline px-4 py-4 lg:px-10">
          <div className="mx-auto flex w-full max-w-6xl flex-col items-start justify-between gap-1 md:flex-row md:items-center md:gap-4">
            <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft">
              Clíniva · MVP 2026
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