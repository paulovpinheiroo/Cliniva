import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { useTheme } from '@/hooks/useTheme'

const monoLink =
  'cursor-pointer font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft underline-offset-4 transition-colors duration-150 ease-in-out hover:text-ink hover:underline'

export function LandingHeader() {
  const { theme, toggleTheme } = useTheme()

  return (
    <header className="border-b border-hairline">
      <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-4 py-4 md:px-6 lg:px-10">
        <Link to="/" className="flex shrink-0 items-center gap-2.5">
          <img src="/Cliniva-Simbolo.png" alt="Símbolo Clíniva" className="h-8 w-8 object-contain" />
          <span className="font-display text-2xl text-ink">Clíniva</span>
        </Link>
        <nav className="flex items-center gap-4 lg:gap-6">
          <button
            onClick={toggleTheme}
            aria-label={theme === 'dark' ? 'Ativar tema claro' : 'Ativar tema escuro'}
            className={`cursor-pointer ${monoLink}`}
          >
            [ {theme === 'dark' ? 'claro' : 'escuro'} ]
          </button>
          <Link to="/login" className={monoLink}>
            [ entrar ]
          </Link>
          <Link to="/cadastro" className="shrink-0">
            <Button size="sm">Criar clínica</Button>
          </Link>
        </nav>
      </div>
    </header>
  )
}