import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/Button'

export function FinalCtaSection() {
  return (
    <section>
      <div className="mx-auto w-full max-w-6xl px-4 py-16 md:px-6 lg:px-10 lg:py-24">
        <div className="border border-hairline bg-paper p-8 lg:p-14">
          <p className="font-mono text-[11px] font-medium uppercase tracking-[0.2em] text-accent-strong">
            Comece agora
          </p>
          <h2 className="mt-4 max-w-2xl font-display text-3xl font-medium leading-[1.1] text-ink sm:text-4xl lg:text-5xl">
            Seja uma das primeiras clínicas na plataforma.
          </h2>
          <p className="mt-5 max-w-xl text-sm leading-relaxed text-ink-soft">
            Crie sua clínica em menos de um minuto e veja agenda, clientes e caixa
            conversarem entre si — antes que o caderno dê conta de outra semana.
          </p>
          <div className="mt-8">
            <Link to="/cadastro">
              <Button size="md">Criar minha clínica</Button>
            </Link>
          </div>
        </div>
      </div>
    </section>
  )
}