interface StatCellProps {
  label: string
  value: number
}

function StatCell({ label, value }: StatCellProps) {
  return (
    <div className="bg-paper px-5 py-5">
      <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">{label}</p>
      <p className="mt-1.5 font-display text-3xl font-medium text-ink">{value}</p>
    </div>
  )
}

interface AtendimentoRowProps {
  nome: string
  hora: string
  valor: string
}

function AtendimentoRow({ nome, hora, valor }: AtendimentoRowProps) {
  return (
    <li className="flex items-center justify-between gap-4 px-5 py-3.5">
      <div>
        <p className="text-sm font-medium text-ink">{nome}</p>
        <p className="mt-0.5 font-mono text-[11px] uppercase tracking-[0.14em] text-ink-soft">
          {hora}
        </p>
      </div>
      <span className="shrink-0 font-mono text-sm text-accent-strong">{valor}</span>
    </li>
  )
}

const stats = [
  { label: 'Clientes', value: 128 },
  { label: 'Serviços', value: 12 },
  { label: 'Itens no estoque', value: 47 },
  { label: 'Atendimentos hoje', value: 6 },
]

const atendimentos = [
  { nome: 'Mariana Costa', hora: '14:30 — Peeling', valor: 'R$ 420' },
  { nome: 'Juliana Prado', hora: '15:00 — Limpeza', valor: 'R$ 250' },
  { nome: 'Ana Beatriz', hora: '16:00 — Hidratação', valor: 'R$ 380' },
]

export function ProductMockup() {
  const hoje = new Date().toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })

  return (
    <div className="animate-fade border border-hairline bg-paper">
      <div className="flex items-center justify-between gap-4 border-b border-hairline px-5 py-3">
        <p className="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft">
          Clíniva — Dashboard
        </p>
        <p className="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft">
          hoje {hoje}
        </p>
      </div>
      <div className="grid grid-cols-2 gap-px border-b border-hairline bg-hairline lg:grid-cols-4">
        {stats.map((stat) => (
          <StatCell key={stat.label} label={stat.label} value={stat.value} />
        ))}
      </div>
      <ul className="divide-y divide-hairline">
        {atendimentos.map((atendimento) => (
          <AtendimentoRow key={atendimento.nome} {...atendimento} />
        ))}
      </ul>
    </div>
  )
}