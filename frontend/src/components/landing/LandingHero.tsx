import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { ProductMockup } from '@/components/landing/ProductMockup'

export function LandingHero() {
  return (
    <section className="border-b border-hairline">
      <div className="mx-auto grid w-full max-w-6xl grid-cols-1 items-center gap-12 px-4 py-16 md:px-6 lg:grid-cols-12 lg:gap-16 lg:px-10 lg:py-24">
        <div className="lg:col-span-6">
          <p className="animate-rise font-mono text-[11px] font-medium uppercase tracking-[0.2em] text-accent-strong">
            Sistema de gestão · Clínicas de estética
          </p>
          <h1
            className="mt-4 animate-rise font-display text-4xl font-medium leading-[1.05] text-ink sm:text-5xl lg:text-6xl"
            style={{ animationDelay: '40ms' }}
          >
            Agenda, clientes e caixa da sua clínica, em um só lugar.
          </h1>
          <p className="mt-5 max-w-xl animate-rise text-base leading-relaxed text-ink-soft" style={{ animationDelay: '80ms' }}>
            O caderno marca, o WhatsApp lembra e a planilha soma — até falhar no dia
            que mais importa. A Clíniva junta agenda, CRM, financeiro e estoque numa
            tela só, feita para a rotina da clínica de estética pequena.
          </p>
          <div
            className="mt-8 flex animate-rise flex-col gap-3 sm:flex-row sm:items-center"
            style={{ animationDelay: '120ms' }}
          >
            <Link to="/cadastro">
              <Button size="md" className="w-full sm:w-auto">
                Criar minha clínica
              </Button>
            </Link>
            <Link to="/login">
              <Button size="md" variant="secondary" className="w-full sm:w-auto">
                Entrar
              </Button>
            </Link>
          </div>
          <p className="mt-4 animate-rise font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft" style={{ animationDelay: '160ms' }}>
            Cadastro gratuito · leva menos de um minuto
          </p>
        </div>
        <div className="animate-rise lg:col-span-6" style={{ animationDelay: '120ms' }}>
          <ProductMockup />
        </div>
      </div>
    </section>
  )
}