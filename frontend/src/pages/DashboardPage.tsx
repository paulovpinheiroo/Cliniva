import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { atendimentosApi } from '@/api/atendimentosApi'
import { clientesApi } from '@/api/clientesApi'
import { itensApi } from '@/api/itensApi'
import { servicosApi } from '@/api/servicosApi'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Spinner } from '@/components/ui/Spinner'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { useApi } from '@/hooks/useApi'
import type { AtendimentoResumo, Item } from '@/types'
import { formatDataHora, formatMoeda } from '@/utils/format'

function StatCard({ label, value, to }: { label: string; value: number; to: string }) {
  return (
    <Link
      to={to}
      className="rounded-xl border border-borderline bg-surface p-5 transition-colors hover:border-sage/50"
    >
      <p className="text-sm text-slate-400">{label}</p>
      <p className="mt-1 text-3xl font-semibold text-lilac">{value}</p>
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
    <section className="rounded-xl border border-borderline bg-surface">
      <div className="flex items-center justify-between border-b border-borderline px-5 py-3">
        <h2 className="text-sm font-semibold text-white">{title}</h2>
        {action}
      </div>
      <div className="p-5">{children}</div>
    </section>
  )
}

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
      <h1 className="mb-1 text-2xl font-semibold text-white">Dashboard</h1>
      <p className="mb-6 text-sm text-slate-400">Visão geral da clínica</p>

      {error && <ErrorBanner message={error} />}

      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Clientes" value={clientes?.length ?? 0} to="/clientes" />
        <StatCard label="Serviços" value={servicos?.length ?? 0} to="/servicos" />
        <StatCard label="Itens em estoque" value={itens?.length ?? 0} to="/estoque" />
        <StatCard label="Atendimentos hoje" value={atendimentosHoje.length} to="/atendimentos" />
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <Section title="Próximos atendimentos" action={<Link className="text-xs text-lilac hover:underline" to="/atendimentos">Ver todos</Link>}>
          {proximos.length === 0 ? (
            <p className="py-4 text-sm text-slate-400">Nenhum atendimento agendado.</p>
          ) : (
            <ul className="divide-y divide-borderline/50">
              {proximos.map((atendimento: AtendimentoResumo) => (
                <li key={atendimento.id} className="flex items-center justify-between gap-4 py-2.5">
                  <div>
                    <p className="font-medium text-white">{atendimento.nomeCliente}</p>
                    <p className="text-xs text-slate-400">{formatDataHora(atendimento.dataAtendimento)}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-sm font-medium text-sage">{formatMoeda(atendimento.valorTotal)}</span>
                    <StatusBadge status={atendimento.status} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Section>

        <Section title="Estoque baixo" action={<Link className="text-xs text-lilac hover:underline" to="/estoque">Ver estoque</Link>}>
          {estoqueBaixo.length === 0 ? (
            <p className="py-4 text-sm text-slate-400">Nenhum item com estoque baixo. Tudo certo!</p>
          ) : (
            <ul className="divide-y divide-borderline/50">
              {estoqueBaixo.map((item) => (
                <li key={item.id} className="flex items-center justify-between py-2.5">
                  <span className="text-white">{item.nome}</span>
                  <span className="rounded-full bg-yellow-500/20 px-2.5 py-0.5 text-xs font-medium text-yellow-400">
                    {item.quantidadeEmEstoque} restante
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Section>
      </div>
    </>
  )
}