interface RecursoProps {
  titulo: string
  descricao: string
}

const recursos: RecursoProps[] = [
  {
    titulo: 'Dashboard',
    descricao:
      'Clientes, serviços, estoque e atendimentos do dia em números — com alerta de estoque baixo.',
  },
  {
    titulo: 'CRM de clientes',
    descricao:
      'Perfil completo, histórico de procedimentos, notas e follow-up pelo WhatsApp. Aniversariantes do mês lembrados para você.',
  },
  {
    titulo: 'Agenda de atendimentos',
    descricao: 'Agendados, concluídos e cancelados — com valor e histórico de cada cliente.',
  },
  {
    titulo: 'Controle de estoque',
    descricao: 'Cada item acompanhado, com alerta de baixa no dashboard.',
  },
  {
    titulo: 'Isolamento por clínica',
    descricao:
      'Multi-tenant: cada clínica enxerga somente os próprios dados. Sua informação não se mistura com a de ninguém.',
  },
]

export function FeaturesSection() {
  return (
    <section className="border-b border-hairline">
      <div className="mx-auto w-full max-w-6xl px-4 py-16 md:px-6 lg:px-10 lg:py-24">
        <p className="font-mono text-[11px] font-medium uppercase tracking-[0.2em] text-accent-strong">
          O que já existe hoje
        </p>
        <h2 className="mt-4 max-w-2xl font-display text-3xl font-medium leading-[1.1] text-ink sm:text-4xl lg:text-5xl">
          Nada de roadmap prometido — tudo o que está no ar.
        </h2>
        <ul className="mt-12 divide-y divide-hairline border-y border-hairline">
          {recursos.map((recurso) => (
            <li
              key={recurso.titulo}
              className="grid grid-cols-1 gap-3 py-6 md:grid-cols-12 md:items-baseline md:gap-6"
            >
              <div className="flex items-baseline gap-3 md:col-span-3">
                <span className="shrink-0 font-mono text-[10px] uppercase tracking-[0.18em] text-accent-strong">
                  [ ok ]
                </span>
                <h3 className="font-display text-xl font-medium text-ink">{recurso.titulo}</h3>
              </div>
              <p className="text-sm leading-relaxed text-ink-soft md:col-span-8">
                {recurso.descricao}
              </p>
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}