import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { atendimentosApi } from '@/api/atendimentosApi'
import { clientesApi } from '@/api/clientesApi'
import { itensApi } from '@/api/itensApi'
import { servicosApi } from '@/api/servicosApi'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { PageHeader } from '@/components/ui/PageHeader'
import { Spinner } from '@/components/ui/Spinner'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { useApi } from '@/hooks/useApi'
import type { AtendimentoResumo, Item } from '@/types'
import { formatDataHora, formatMoeda } from '@/utils/format'

function StatCell({
  label,
  value,
  to,
  delay = 0,
}: {
  label: string
  value: number
  to: string
  delay?: number
}) {
  return (
    <Link
      to={to}
      style={{ animationDelay: `${delay}ms` }}
      className="group block animate-stagger px-6 py-6 transition-colors duration-150 ease-in-out hover:bg-paper"
    >
      <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">{label}</p>
      <p className="mt-2 font-display text-4xl font-medium text-ink transition-colors duration-150 ease-in-out group-hover:text-accent-strong">
        {value}
      </p>
    </Link>
  )
}

function Section({
  title,
  action,
  children,
}: {
  title: string
  action?: ReactNode
  children: ReactNode
}) {
  return (
    <section>
      <div className="flex items-baseline justify-between gap-4 border-b border-hairline pb-3">
        <h2 className="font-display text-2xl font-medium text-ink">{title}</h2>
        {action}
      </div>
      {children}
    </section>
  )
}

const actionLink = 'text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft underline-offset-4 hover:text-ink hover:underline'

function isHoje(iso: string): boolean {
  const data = new Date(iso)
  const agora = new Date()
  return (
    data.getFullYear() === agora.getFullYear() &&
    data.getMonth() === agora.getMonth() &&
    data.getDate() === agora.getDate()
  )
}

const ESTOQUE_BAIXO_LIMITE = 5

export function DashboardPage() {
  const { data: clientes, loading: loadingClientes, error: errorClientes } = useApi(() => clientesApi.listar())
  const { data: servicos, loading: loadingServicos, error: errorServicos } = useApi(() => servicosApi.listar())
  const { data: itens, loading: loadingItens, error: errorItens } = useApi(() => itensApi.listar())
  const { data: atendimentos, loading: loadingAtendimentos, error: errorAtendimentos } = useApi(() =>
    atendimentosApi.listar(),
  )

  const loading = loadingClientes || loadingServicos || loadingItens || loadingAtendimentos
  const error = errorClientes ?? errorServicos ?? errorItens ?? errorAtendimentos

  const atendimentosHoje = atendimentos?.filter((a) => isHoje(a.dataAtendimento) && a.status !== 'CANCELADO') ?? []
  const proximos =
    atendimentos
      ?.filter((a) => a.status === 'AGENDADO')
      .sort((a, b) => a.dataAtendimento.localeCompare(b.dataAtendimento))
      .slice(0, 5) ?? []

  const estoqueBaixo: Item[] = itens?.filter((item) => item.quantidadeEmEstoque < ESTOQUE_BAIXO_LIMITE) ?? []

  if (loading) {
    return <Spinner />
  }

  return (
    <>
      <PageHeader
        kicker="Visão geral"
        title="Dashboard"
        subtitle="O estado da casa, em números e próximos passos."
      />

      {error && <ErrorBanner message={error} />}

      <div className="mb-14 grid grid-cols-1 divide-y divide-hairline border-y border-hairline lg:grid-cols-4 lg:divide-x lg:divide-y-0">
        <StatCell label="Clientes" value={clientes?.length ?? 0} to="/clientes" delay={0} />
        <StatCell label="Serviços" value={servicos?.length ?? 0} to="/servicos" delay={60} />
        <StatCell label="Itens no estoque" value={itens?.length ?? 0} to="/estoque" delay={120} />
        <StatCell label="Atendimentos hoje" value={atendimentosHoje.length} to="/atendimentos" delay={180} />
      </div>

      <div className="grid grid-cols-1 gap-x-14 gap-y-14 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <Section
            title="Próximos atendimentos"
            action={
              <Link className={actionLink} to="/atendimentos">
                Ver todos
              </Link>
            }
          >
            {proximos.length === 0 ? (
              <p className="py-6 text-sm text-ink-soft">Nenhum atendimento agendado.</p>
            ) : (
              <ul>
                {proximos.map((atendimento: AtendimentoResumo) => (
                  <li
                    key={atendimento.id}
                    className="flex items-center justify-between gap-4 border-b border-hairline py-3.5"
                  >
                    <div>
                      <p className="text-sm font-medium text-ink">{atendimento.nomeCliente}</p>
                      <p className="mt-0.5 font-mono text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                        {formatDataHora(atendimento.dataAtendimento)}
                      </p>
                    </div>
                    <div className="flex items-center gap-4">
                      <span className="font-mono text-sm text-accent-strong">
                        {formatMoeda(atendimento.valorTotal)}
                      </span>
                      <StatusBadge status={atendimento.status} />
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </Section>
        </div>

        <div className="lg:mt-16">
          <Section
            title="Estoque baixo"
            action={
              <Link className={actionLink} to="/estoque">
                Ver estoque
              </Link>
            }
          >
            {estoqueBaixo.length === 0 ? (
              <p className="py-6 text-sm text-ink-soft">Nenhum item com estoque baixo. Tudo certo!</p>
            ) : (
              <ul>
                {estoqueBaixo.map((item) => (
                  <li
                    key={item.id}
                    className="flex items-center justify-between gap-4 border-b border-hairline py-3.5"
                  >
                    <span className="text-sm text-ink">{item.nome}</span>
                    <span className="flex shrink-0 items-center gap-2">
                      <span className="h-1.5 w-1.5 bg-yellow-700" />
                      <span className="font-mono text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                        {item.quantidadeEmEstoque}
                        {item.quantidadeEmEstoque === 1 ? ' restante' : ' restantes'}
                      </span>
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </Section>
        </div>
      </div>
    </>
  )
}