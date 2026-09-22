import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { bookingApi } from '@/api/bookingApi'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Spinner } from '@/components/ui/Spinner'
import { TextField } from '@/components/ui/TextField'
import { useApi } from '@/hooks/useApi'
import type { BookingResult, ServicoPublico } from '@/types'
import { formatDataHora, formatDataLonga, formatMoeda } from '@/utils/format'

const diasDisponiveis = 30

function toISODate(d: Date): string {
  const mes = String(d.getMonth() + 1).padStart(2, '0')
  const dia = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mes}-${dia}`
}

function hojeISO(): string {
  const agora = new Date()
  return toISODate(agora)
}

function dataMaximaISO(): string {
  const dt = new Date()
  dt.setDate(dt.getDate() + diasDisponiveis)
  return toISODate(dt)
}

function formatDataLongaISO(iso: string): string {
  const [a, m, d] = iso.split('-').map(Number)
  return formatDataLonga(new Date(a, m - 1, d).toISOString())
}

export function BookingPage() {
  const { slug = '' } = useParams<{ slug: string }>()

  const { data: servicos, loading, error } = useApi<ServicoPublico[]>(
    () => bookingApi.listarServicos(slug),
    [slug],
  )

  const [servicoId, setServicoId] = useState('')
  const [data, setData] = useState(hojeISO())
  const [horarioSelecionado, setHorarioSelecionado] = useState('')

  const [nome, setNome] = useState('')
  const [telefone, setTelefone] = useState('')
  const [email, setEmail] = useState('')
  const [formErro, setFormErro] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [sucesso, setSucesso] = useState<BookingResult | null>(null)

  const dispPronto = Boolean(servicoId)
  const { data: disponibilidade, loading: loadingDisp, error: dispErro } = useApi(
    () =>
      dispPronto
        ? bookingApi.disponibilidade(slug, data, servicoId)
        : Promise.resolve(null as never),
    [slug, data, servicoId],
  )

  const clinicaNome = servicos?.[0]?.clinica ?? 'Clíniva'
  const servicoEscolhido = servicos?.find((s) => s.id === servicoId)

  const confirmar = async () => {
    if (!servicoId) {
      setFormErro('Selecione um serviço.')
      return
    }
    if (!horarioSelecionado) {
      setFormErro('Escolha um horário disponível.')
      return
    }
    if (!nome.trim() || !telefone.trim()) {
      setFormErro('Informe nome e telefone para o contato.')
      return
    }
    setSalvando(true)
    setFormErro('')
    try {
      const resultado = await bookingApi.agendar(slug, {
        servicoId,
        dataHora: `${data}T${horarioSelecionado}:00`,
        nome: nome.trim(),
        telefone: telefone.trim(),
        email: email.trim() || undefined,
      })
      setSucesso(resultado)
    } catch (err) {
      setFormErro(err instanceof Error ? err.message : 'Falha ao agendar. Tente novamente.')
    } finally {
      setSalvando(false)
    }
  }

  if (sucesso) {
    return (
      <div className="min-h-screen bg-ivory">
        <main className="mx-auto flex w-full max-w-3xl flex-col gap-10 px-5 py-16 lg:py-24">
          <div className="border border-hairline bg-paper p-6 md:p-10">
            <p className="mb-3 text-[11px] font-medium uppercase tracking-[0.2em] text-accent-strong">
              Confirmação
            </p>
            <h1 className="font-display text-3xl font-medium text-ink md:text-4xl">
              Agendamento confirmado
            </h1>
            <p className="mt-4 text-sm leading-relaxed text-ink-soft">
              Sua visita em <span className="font-medium text-ink">{sucesso.clinica}</span> está marcada.
              Caso precise remarcar ou desmarcar, entre em contato com a clínica pelo WhatsApp.
            </p>
            <div className="mt-8 grid grid-cols-1 gap-6 border-t border-hairline pt-6 sm:grid-cols-2">
              <div>
                <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">Serviço</p>
                <p className="mt-1 font-medium text-ink">{sucesso.servico}</p>
              </div>
              <div>
                <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">Quando</p>
                <p className="mt-1 font-medium text-ink">{formatDataHora(sucesso.dataAtendimento)}</p>
              </div>
              <div>
                <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">Duração</p>
                <p className="mt-1 font-medium text-ink">{sucesso.duracaoMinutos} minutos</p>
              </div>
              <div>
                <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">Quem</p>
                <p className="mt-1 font-medium text-ink">{sucesso.cliente}</p>
              </div>
            </div>
            <div className="mt-8">
              <Button variant="secondary" onClick={() => setSucesso(null)}>
                Fazer novo agendamento
              </Button>
            </div>
          </div>
        </main>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-ivory">
      <header className="mx-auto flex w-full max-w-3xl items-center justify-between px-5 py-8">
        <span className="font-display text-xl font-medium text-ink">Cliniva</span>
        <span className="font-mono text-[11px] uppercase tracking-[0.2em] text-ink-soft">
          {clinicaNome}
        </span>
      </header>

      <main className="mx-auto flex w-full max-w-3xl flex-col gap-8 px-5 pb-16 lg:pb-24">
        <div>
          <p className="mb-2 text-[11px] font-medium uppercase tracking-[0.2em] text-accent-strong">
            Agendamento online
          </p>
          <h1 className="font-display text-3xl font-medium text-ink md:text-4xl">
            Escolha seu horário
          </h1>
          <p className="mt-3 max-w-xl text-sm leading-relaxed text-ink-soft">
            Selecione o serviço, o dia e o horário. O atendimento é agendado na hora — é só chegar.
          </p>
        </div>

        {error && <ErrorBanner message={error} />}

        {loading ? (
          <Spinner />
        ) : !servicos || servicos.length === 0 ? (
          <EmptyState message="A clínica não possui serviços disponíveis para agendamento online no momento." />
        ) : (
          <div className="flex flex-col gap-8">
            <div className="border border-hairline bg-paper p-6 md:p-8">
              <label className="flex flex-col">
                <span className="mb-3 text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">
                  Serviço *
                </span>
                <div className="flex flex-col gap-3">
                  {servicos.map((servico) => (
                    <button
                      key={servico.id}
                      type="button"
                      onClick={() => {
                        setServicoId(servico.id)
                        setHorarioSelecionado('')
                      }}
                      className={`cursor-pointer border p-4 text-left transition-colors duration-150 ease-in-out ${
                        servico.id === servicoId
                          ? 'border-accent bg-ivory'
                          : 'border-hairline bg-transparent hover:border-ink/40'
                      }`}
                    >
                      <div className="flex items-start justify-between gap-4">
                        <div>
                          <p className="font-medium text-ink">{servico.nome}</p>
                          {servico.descricao && (
                            <p className="mt-0.5 text-sm text-ink-soft">{servico.descricao}</p>
                          )}
                        </div>
                        <div className="text-right">
                          <p className="font-mono text-[13px] text-accent-strong">
                            {formatMoeda(servico.valor)}
                          </p>
                          <p className="mt-0.5 text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                            {servico.duracaoMinutos} min
                          </p>
                        </div>
                      </div>
                    </button>
                  ))}
                </div>
              </label>
            </div>

            <div className="border border-hairline bg-paper p-6 md:p-8">
              <label className="flex flex-col">
                <span className="mb-3 text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">
                  Dia *
                </span>
                <TextField
                  label=""
                  type="date"
                  className="w-full sm:w-56"
                  min={hojeISO()}
                  max={dataMaximaISO()}
                  value={data}
                  onChange={(e) => {
                    setData(e.target.value || hojeISO())
                    setHorarioSelecionado('')
                  }}
                />
              </label>

              <div className="mt-6">
                <p className="mb-3 text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">
                  Horários disponíveis ({formatDataLongaISO(data)})
                </p>
                {loadingDisp ? (
                  <Spinner />
                ) : dispErro ? (
                  <ErrorBanner message={dispErro} />
                ) : !servicoId ? (
                  <p className="text-sm text-ink-soft">
                    Selecione um serviço acima para ver os horários disponíveis.
                  </p>
                ) : !disponibilidade || disponibilidade.horarios.length === 0 ? (
                  <p className="text-sm text-ink-soft">Nenhum horário livre neste dia.</p>
                ) : (
                  <div className="flex flex-wrap gap-2">
                    {disponibilidade.horarios.map((hora) => {
                      const valor = hora.slice(0, 5)
                      const ativo = valor === horarioSelecionado
                      return (
                        <button
                          key={valor}
                          type="button"
                          onClick={() => setHorarioSelecionado(valor)}
                          className={`cursor-pointer border px-4 py-2 font-mono text-[12px] transition-colors duration-150 ease-in-out ${
                            ativo
                              ? 'border-accent bg-accent text-carbon'
                              : 'border-hairline text-ink hover:border-accent hover:text-accent-strong'
                          }`}
                        >
                          {valor}
                        </button>
                      )
                    })}
                  </div>
                )}
              </div>
            </div>

            <div className="border border-hairline bg-paper p-6 md:p-8">
              <p className="mb-4 text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">
                Seus dados *
              </p>
              <div className="flex flex-col gap-5">
                <TextField
                  label="Nome *"
                  value={nome}
                  onChange={(e) => setNome(e.target.value)}
                  placeholder="Como podemos te chamar?"
                />
                <TextField
                  label="WhatsApp *"
                  type="tel"
                  value={telefone}
                  onChange={(e) => setTelefone(e.target.value)}
                  placeholder="(11) 99999-9999"
                />
                <TextField
                  label="E-mail (opcional)"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="voce@email.com"
                />
                {formErro && <p className="text-sm text-red-600">{formErro}</p>}
                <div className="mt-2 flex justify-end">
                  <Button onClick={confirmar} disabled={salvando || !servicoEscolhido}>
                    {salvando ? 'Agendando...' : 'Confirmar agendamento'}
                  </Button>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  )
}