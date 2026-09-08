import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import type { ReactNode } from 'react'
import { clientesApi } from '@/api/clientesApi'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { PageHeader } from '@/components/ui/PageHeader'
import { Spinner } from '@/components/ui/Spinner'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { TextField } from '@/components/ui/TextField'
import { useApi } from '@/hooks/useApi'
import {
  canalLabel,
  fidelidadeLabel,
  formatData,
  formatDataHora,
  formatDataLonga,
  formatMoeda,
  origemLabel,
  whatsappLink,
} from '@/utils/format'

const microLabel = 'text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft'

function Field({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div>
      <span className={`mb-1 block ${microLabel}`}>{label}</span>
      <span className="text-sm text-ink">{value}</span>
    </div>
  )
}

function StatCell({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="px-6 py-6">
      <p className={microLabel}>{label}</p>
      <p className="mt-2 font-display text-3xl font-medium text-ink">{value}</p>
    </div>
  )
}

function Section({ title, action, children }: { title: string; action?: ReactNode; children: ReactNode }) {
  return (
    <section>
      <div className="flex items-baseline justify-between gap-4 border-b border-hairline pb-3">
        <h2 className="font-display text-2xl font-medium text-ink">{title}</h2>
        {action}
      </div>
      <div className="pt-6">{children}</div>
    </section>
  )
}

export function ClienteDetalhePage() {
  const { id } = useParams<{ id: string }>()
  const {
    data: historico,
    loading,
    error,
  } = useApi(() => clientesApi.historico(id ?? ''), [id])
  const {
    data: notas,
    error: errorNotas,
    refetch: refetchNotas,
  } = useApi(() => clientesApi.listarNotas(id ?? ''), [id])

  const [novaNota, setNovaNota] = useState('')
  const [notaMensagem, setNotaMensagem] = useState('')
  const [salvandoNota, setSalvandoNota] = useState(false)

  if (loading) {
    return <Spinner />
  }

  if (error || !historico) {
    return (
      <>
        <ErrorBanner message={error ?? 'Cliente não encontrado.'} />
        <Link className={microLabel} to="/clientes">
          ← Voltar para clientes
        </Link>
      </>
    )
  }

  const cliente = historico.cliente
  const mensagemFollowUp = `Olá ${cliente.nome}! Tudo bem? Passando só pra saber se precisa agendar um horário na Clíniva.`

  const adicionarNota = async () => {
    if (!id || !novaNota.trim()) return
    setSalvandoNota(true)
    setNotaMensagem('')
    try {
      await clientesApi.criarNota(id, novaNota.trim())
      setNovaNota('')
      refetchNotas()
    } catch (err) {
      setNotaMensagem(err instanceof Error ? err.message : 'Falha ao salvar a nota.')
    } finally {
      setSalvandoNota(false)
    }
  }

  const excluirNota = async (notaId: string) => {
    if (!id) return
    setNotaMensagem('')
    try {
      await clientesApi.deletarNota(id, notaId)
      refetchNotas()
    } catch (err) {
      setNotaMensagem(err instanceof Error ? err.message : 'Falha ao excluir a nota.')
    }
  }

  return (
    <>
      <div className="mb-6">
        <Link
          className={`${microLabel} underline-offset-4 hover:text-ink hover:underline`}
          to="/clientes"
        >
          ← Voltar para clientes
        </Link>
      </div>

      <PageHeader
        kicker="Ficha do cliente"
        title={cliente.nome}
        subtitle={`${cliente.telefone}${cliente.email ? ` · ${cliente.email}` : ''}`}
        action={
          <a
            href={whatsappLink(cliente.telefone, mensagemFollowUp)}
            target="_blank"
            rel="noreferrer"
            className="inline-flex w-full cursor-pointer items-center justify-center gap-2 bg-accent px-5 py-3 text-[11px] font-medium uppercase tracking-[0.18em] text-carbon transition duration-150 ease-in-out hover:bg-accent-strong lg:w-auto"
          >
            Follow-up
          </a>
        }
      />

      <div className="mb-14 flex items-center gap-4">
        <span className={`text-[11px] font-medium uppercase tracking-[0.14em] text-sage-dark`}>
          {fidelidadeLabel(historico.atendimentosConcluidos)}
        </span>
        {cliente.dataNascimento && (
          <span className={`${microLabel} text-ink-soft`}>
            Aniversário: {formatDataLonga(cliente.dataNascimento)}
          </span>
        )}
      </div>

      {errorNotas && <ErrorBanner message={errorNotas} />}

      <div className="grid grid-cols-1 gap-x-14 gap-y-14 lg:grid-cols-3">
        <div className="grid grid-cols-1 gap-y-14 lg:col-span-2">
          <Section title="Financeiro">
            <div className="grid grid-cols-1 divide-y divide-hairline border-y border-hairline sm:grid-cols-2 sm:divide-y-0 lg:grid-cols-4 lg:divide-x lg:divide-y-0">
              <StatCell label="Atendimentos concluídos" value={historico.atendimentosConcluidos} />
              <StatCell label="Gasto total" value={formatMoeda(historico.gastoTotal)} />
              <StatCell label="Ticket médio" value={historico.ticketMedio === null ? '—' : formatMoeda(historico.ticketMedio)} />
              <StatCell label="Última visita" value={historico.ultimaVisita ? formatData(historico.ultimaVisita) : '—'} />
            </div>
          </Section>

          <Section title="Atendimentos">
            {historico.atendimentos.length === 0 ? (
              <EmptyState message={`Nenhum atendimento registrado para ${cliente.nome} ainda.`} />
            ) : (
              <ul>
                {historico.atendimentos.map((atendimento) => (
                  <li
                    key={atendimento.id}
                    className="flex flex-col gap-1 border-b border-hairline py-3.5 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <span className="font-mono text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                      {formatDataHora(atendimento.dataAtendimento)}
                    </span>
                    <div className="flex items-center justify-between gap-4 sm:justify-end">
                      <StatusBadge status={atendimento.status} />
                      <span className="font-mono text-sm text-accent-strong">
                        {formatMoeda(atendimento.valorTotal)}
                      </span>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </Section>

          <Section title="Anotações">
            {notas && notas.length > 0 ? (
              <ul className="mb-6">
                {notas.map((nota) => (
                  <li
                    key={nota.id}
                    className="flex flex-col gap-1 border-b border-hairline py-3.5 sm:flex-row sm:items-start sm:justify-between sm:gap-8"
                  >
                    <div className="min-w-0">
                      <p className="text-sm leading-relaxed text-ink">{nota.texto}</p>
                      <p className="mt-1 font-mono text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                        {formatDataHora(nota.criadaEm)}
                      </p>
                    </div>
                    <Button
                      variant="dangerText"
                      size="sm"
                      className="self-start sm:self-auto"
                      onClick={() => excluirNota(nota.id)}
                    >
                      Excluir
                    </Button>
                  </li>
                ))}
              </ul>
            ) : (
              <EmptyState message="Sem anotações ainda. Registre preferências e detalhes relevantes abaixo." />
            )}
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
              <TextField
                label="Nova anotação"
                value={novaNota}
                onChange={(e) => setNovaNota(e.target.value)}
                placeholder="Detalhe relevante sobre a cliente..."
                className="flex-1"
              />
              <Button onClick={adicionarNota} disabled={salvandoNota || !novaNota.trim()}>
                {salvandoNota ? 'Salvando...' : 'Adicionar'}
              </Button>
            </div>
            {notaMensagem && <p className="mt-3 text-sm text-red-600">{notaMensagem}</p>}
          </Section>
        </div>

        <div className="lg:mt-0">
          <Section title="Perfil">
            <div className="grid grid-cols-1 gap-6">
              <Field label="Telefone" value={<span className="font-mono text-[13px]">{cliente.telefone}</span>} />
              <Field label="E-mail" value={cliente.email || '—'} />
              <Field label="Data de nascimento" value={cliente.dataNascimento ? formatDataLonga(cliente.dataNascimento) : '—'} />
              <Field label="Status" value={cliente.status} />
              <Field label="Origem" value={cliente.origem ? origemLabel(cliente.origem) : '—'} />
              <Field label="Canal preferido" value={cliente.canalPreferido ? canalLabel(cliente.canalPreferido) : '—'} />
              <Field label="Preferências" value={cliente.preferencias || '—'} />
              <Field label="Observações" value={cliente.observacoes || '—'} />
            </div>
          </Section>
        </div>
      </div>
    </>
  )
}