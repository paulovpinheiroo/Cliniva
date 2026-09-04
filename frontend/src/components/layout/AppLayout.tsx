import { NavLink, Outlet } from 'react-router-dom'

const navItems = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/clientes', label: 'Clientes' },
  { to: '/servicos', label: 'Serviços' },
  { to: '/estoque', label: 'Estoque' },
  { to: '/atendimentos', label: 'Atendimentos' },
]

export function AppLayout() {
  return (
    <div className="flex min-h-screen bg-carbon text-slate-200">
      <aside className="fixed inset-y-0 left-0 z-40 flex w-56 flex-col border-r border-borderline bg-carbon">
        <NavLink to="/" end className="flex h-20 items-center gap-3 border-b border-borderline px-5">
          <img src="/Cliniva-Logo.png" alt="Cliniva" className="h-10 w-10 rounded-lg object-cover" />
          <span className="text-lg font-semibold text-lilac">Cliniva</span>
        </NavLink>
        <nav className="flex-1 space-y-1 p-3">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `block rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-darkpurple text-lilac'
                    : 'text-slate-400 hover:bg-darkpurple/50 hover:text-white'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <main className="ml-56 flex-1 p-8">
        <Outlet />
      </main>
    </div>
  )
}