import { useState } from 'react'
import { agendaApi } from '@/api/agendaApi'
import { atendimentosApi } from '@/api/atendimentosApi'
import { clientesApi } from '@/api/clientesApi'
import { itensApi } from '@/api/itensApi'
import { servicosApi } from '@/api/servicosApi'
import { Button } from '@/components/ui/Button'
import { CardActions, CardDetail, CardItem, CardLabel, CardList } from '@/components/ui/CardList'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Modal } from '@/components/ui/Modal'
import { PageHeader } from '@/components/ui/PageHeader'
import { Select } from '@/components/ui/Select'
import { Spinner } from '@/components/ui/Spinner'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { TextField } from '@/components/ui/TextField'
import { useApi } from '@/hooks/useApi'
import type {
  AgendaItem,
  AtendimentoInput,
  Cliente,
  DisponibilidadeDia,
  HorarioAtendimento,
  Item,
  Servico,
  StatusAtendimento,
} from '@/types'
import { formatDataHora, formatDataLonga, formatMoeda, whatsappLink } from '@/utils/format'

const DIAS = ['Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado', 'Domingo']

function toISODate(d: Date): string {
  const mes = String(d.getMonth() + 1).padStart(2, '0')
  const dia = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mes}-${dia}`
}

function addDias(iso: string, delta: number): string {
  const [a, m, d] = iso.split('-').map(Number)
  const dt = new Date(a, m - 1, d + delta)
  return toISODate(dt)
}

function hojeISO(): string {
  const agora = new Date()
  return toISODate(agora)
}

function formatDataLongaISO(iso: string): string {
  const [a, m, d] = iso.split('-').map(Number)
  return formatDataLonga(new Date(a, m - 1, d).toISOString())
}

function normalizeDatetimeLocal(value: string): string {
  return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value) ? `${value}:00` : value
}

interface ItemExtraRow {
  itemId: string
  quantidade: number
}

interface ServicoRow {
  servicoId: string
  itensExtras: ItemExtraRow[]
}

const emptyServicoRow: ServicoRow = { servicoId: '', itensExtras: [] }

const microLabel = 'text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft'
const lembrarLink =
  'inline-flex cursor-pointer items-center justify-center gap-2 text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft underline-offset-4 transition duration-150 ease-in-out hover:text-ink hover:underline'

function LembrarLink({ inicio, nome, telefone }: { inicio: string; nome: string; telefone: string }) {
  const mensagem = `Olá ${nome}! Passando para lembrar do seu atendimento na Clíniva em ${formatDataHora(inicio)}.`
  return (
    <a href={whatsappLink(telefone, mensagem)} target="_blank" rel="noreferrer" className={lembrarLink}>
      Lembrar
    </a>
  )
}

export function AgendaPage() {
  const { data: clientes, loading: loadingClientes } = useApi<Cliente[]>(() => clientesApi.listar())
  const { data: servicos, loading: loadingServicos } = useApi<Servico[]>(() => servicosApi.listar())
  const { data: itensDisponiveis } = useApi<Item[]>(() => itensApi.listar())
  const { data: link } = useApi(() => agendaApi.linkPublico())

  const [data, setData] = useState(hojeISO())
  const { data: itens, loading, error, refetch } = useApi(() => agendaApi.listarDia(data), [data])

  const [copiado, setCopiado] = useState(false)

  const [criando, setCriando] = useState(false)
  const [clienteId, setClienteId] = useState('')
  const [dataAtendimento, setDataAtendimento] = useState('')
  const [servicosForm, setServicosForm] = useState<ServicoRow[]>([emptyServicoRow])
  const [criarErro, setCriarErro] = useState('')
  const [salvando, setSalvando] = useState(false)

  const [disponibilidade, setDisponibilidade] = useState<DisponibilidadeDia | null>(null)
  const [buscaServico, setBuscaServico] = useState('')
  const [buscaData, setBuscaData] = useState(hojeISO())
  const [buscando, setBuscando] = useState(false)
  const [buscarErro, setBuscarErro] = useState('')

  const [horarios, setHorarios] = useState<HorarioAtendimento[]>([])
  const [horariosAberto, setHorariosAberto] = useState(false)
  const [horariosErro, setHorariosErro] = useState('')
  const [salvandoHorarios, setSalvandoHorarios] = useState(false)

  const [remarcando, setRemarcando] = useState<AgendaItem | null>(null)
  const [novaData, setNovaData] = useState('')
  const [remarcarErro, setRemarcarErro] = useState('')

  const [acaoStatus, setAcaoStatus] = useState<{ atendimento: AgendaItem; acao: 'concluir' | 'cancelar' } | null>(null)
  const [acaoErro, setAcaoErro] = useState('')

  const abrirCriar = () => {
    setClienteId('')
    setDataAtendimento('')
    setServicosForm([emptyServicoRow])
    setCriarErro('')
    setDisponibilidade(null)
    setBuscaServico('')
    setBuscaData(hojeISO())
    setBuscarErro('')
    setCriando(true)
  }

  const atualizarServico = (index: number, servicoId: string) => {
    setServicosForm(servicosForm.map((row, i) => (i === index ? { ...row, servicoId } : row)))
  }

  const adicionarServico = () => setServicosForm([...servicosForm, emptyServicoRow])

  const removerServico = (index: number) => {
    setServicosForm(servicosForm.filter((_, i) => i !== index))
  }

  const adicionarItemExtra = (index: number) => {
    setServicosForm(
      servicosForm.map((row, i) =>
        i === index ? { ...row, itensExtras: [...row.itensExtras, { itemId: '', quantidade: 1 }] } : row,
      ),
    )
  }

  const atualizarItemExtra = (rowIndex: number, extraIndex: number, patch: Partial<ItemExtraRow>) => {
    setServicosForm(
      servicosForm.map((row, i) =>
        i === rowIndex
          ? {
              ...row,
              itensExtras: row.itensExtras.map((extra, j) =>
                j === extraIndex ? { ...extra, ...patch } : extra,
              ),
            }
          : row,
      ),
    )
  }

  const removerItemExtra = (rowIndex: number, extraIndex: number) => {
    setServicosForm(
      servicosForm.map((row, i) =>
        i === rowIndex ? { ...row, itensExtras: row.itensExtras.filter((_, j) => j !== extraIndex) } : row,
      ),
    )
  }

  const buscarDisponibilidade = async () => {
    if (!buscaServico) {
      setBuscarErro('Selecione o serviço e o dia para ver os horários livres.')
      return
    }
    setBuscando(true)
    setBuscarErro('')
    setDisponibilidade(null)
    try {
      const resposta = await agendaApi.disponibilidade(buscaData, buscaServico)
      setDisponibilidade(resposta)
    } catch (err) {
      setBuscarErro(err instanceof Error ? err.message : 'Falha ao buscar horários livres.')
    } finally {
      setBuscando(false)
    }
  }

  const salvarAtendimento = async () => {
    if (!clienteId) {
      setCriarErro('Selecione o cliente.')
      return
    }
    if (!dataAtendimento) {
      setCriarErro('Informe a data e horário do atendimento.')
      return
    }
    const servicosValidos = servicosForm.filter((row) => row.servicoId)
    if (servicosValidos.length === 0) {
      setCriarErro('Adicione pelo menos um serviço.')
      return
    }

    const payload: AtendimentoInput = {
      clienteId,
      dataAtendimento: normalizeDatetimeLocal(dataAtendimento),
      servicos: servicosValidos.map((row) => ({
        servicoId: row.servicoId,
        itensExtras: row.itensExtras
          .filter((extra) => extra.itemId && extra.quantidade > 0)
          .map((extra) => ({ itemId: extra.itemId, quantidade: extra.quantidade })),
      })),
    }

    setSalvando(true)
    setCriarErro('')
    try {
      await atendimentosApi.criar(payload)
      setCriando(false)
      refetch()
    } catch (err) {
      setCriarErro(err instanceof Error ? err.message : 'Falha ao criar atendimento.')
    } finally {
      setSalvando(false)
    }
  }

  const abrirRemarcar = (atendimento: AgendaItem) => {
    setRemarcando(atendimento)
    setNovaData(atendimento.inicio.slice(0, 16))
    setRemarcarErro('')
  }

  const confirmarRemarcar = async () => {
    if (!remarcando || !novaData) {
      setRemarcarErro('Informe a nova data e horário.')
      return
    }
    setSalvando(true)
    setRemarcarErro('')
    try {
      await atendimentosApi.atualizar(remarcando.id, {
        clienteId: remarcando.clienteId,
        dataAtendimento: normalizeDatetimeLocal(novaData),
      })
      setRemarcando(null)
      refetch()
    } catch (err) {
      setRemarcarErro(err instanceof Error ? err.message : 'Falha ao remarcar.')
    } finally {
      setSalvando(false)
    }
  }

  const confirmarAcao = async () => {
    if (!acaoStatus) return
    setSalvando(true)
    setAcaoErro('')
    try {
      const novoStatus: StatusAtendimento = acaoStatus.acao === 'concluir' ? 'CONCLUIDO' : 'CANCELADO'
      await atendimentosApi.alterarStatus(acaoStatus.atendimento.id, novoStatus)
      setAcaoStatus(null)
      refetch()
    } catch (err) {
      setAcaoErro(err instanceof Error ? err.message : 'Falha ao alterar status.')
    } finally {
      setSalvando(false)
    }
  }

  const abrirHorarios = async () => {
    setHorariosAberto(true)
    setHorariosErro('')
    setSalvandoHorarios(true)
    try {
      const atuais = await agendaApi.listarHorarios()
      const mapa = new Map(atuais.map((h) => [h.diaSemana, h]))
      setHorarios(
        DIAS.map((_, i) => {
          const dia = i + 1
          const existente = mapa.get(dia)
          return (
            existente ?? { diaSemana: dia, abertura: '08:00', fechamento: '18:00', ativo: false }
          )
        }),
      )
    } catch (err) {
      setHorariosErro(err instanceof Error ? err.message : 'Falha ao carregar horários de atendimento.')
    } finally {
      setSalvandoHorarios(false)
    }
  }

  const salvarHorarios = async () => {
    setSalvandoHorarios(true)
    setHorariosErro('')
    try {
      await agendaApi.atualizarHorarios(horarios)
      setHorariosAberto(false)
    } catch (err) {
      setHorariosErro(err instanceof Error ? err.message : 'Falha ao salvar horários.')
    } finally {
      setSalvandoHorarios(false)
    }
  }

  const copiarLink = async () => {
    if (!link) return
    try {
      await navigator.clipboard.writeText(`${window.location.origin}${link.caminho}`)
      setCopiado(true)
      setTimeout(() => setCopiado(false), 2000)
    } catch {
      setCopiado(false)
    }
  }

  const acaoConcluindo = acaoStatus?.acao === 'concluir'
  const acaoMensagem = acaoStatus
    ? acaoConcluindo
      ? `Confirmar conclusão do atendimento de ${acaoStatus.atendimento.clienteNome}?`
      : `Confirmar cancelamento do atendimento de ${acaoStatus.atendimento.clienteNome}?`
    : ''

  return (
    <>
      <PageHeader
        kicker="Agenda"
        title="Agenda"
        subtitle={`Expediente de ${formatDataLongaISO(data)}`}
        action={
          <div className="flex w-full flex-col gap-3 sm:flex-row lg:justify-end">
            <Button variant="secondary" onClick={abrirHorarios}>
              Horários
            </Button>
            <Button variant="ghost" onClick={copiarLink} disabled={!link}>
              {copiado ? 'Link copiado!' : 'Link para clientes'}
            </Button>
            <Button onClick={abrirCriar} className="w-full sm:w-auto">
              Novo agendamento
            </Button>
          </div>
        }
      />

      <div className="mb-8 flex flex-wrap items-center justify-between gap-4 border-b border-hairline pb-6">
        <div className="flex items-center gap-3">
          <Button variant="ghost" onClick={() => setData(addDias(data, -1))}>
            ← anterior
          </Button>
          <span className="font-mono text-[13px] uppercase tracking-[0.14em] text-ink-soft">{data}</span>
          <Button variant="ghost" onClick={() => setData(addDias(data, 1))}>
            próximo →
          </Button>
        </div>
        <Button variant="secondary" size="sm" onClick={() => setData(hojeISO())}>
          Hoje
        </Button>
      </div>

      {error && <ErrorBanner message={error} />}

      {loading ? (
        <Spinner />
      ) : !itens || itens.length === 0 ? (
        <EmptyState message="Nenhum atendimento para este dia. Use o botão Novo agendamento para marcar." />
      ) : (
        <>
          <CardList>
            {itens.map((atendimento) => (
              <CardItem key={atendimento.id}>
                <div className="flex items-start justify-between gap-3">
                  <CardLabel>{atendimento.clienteNome}</CardLabel>
                  <div className="shrink-0">
                    <StatusBadge status={atendimento.status} />
                  </div>
                </div>
                <CardDetail>
                  {formatDataHora(atendimento.inicio)} ·{' '}
                  {atendimento.duracaoMinutos} min
                </CardDetail>
                <CardDetail className="truncate">
                  {atendimento.servicos.join(', ') || '—'}
                </CardDetail>
                <CardDetail>
                  {atendimento.status === 'AGENDADO' && (
                    <LembrarLink
                      inicio={atendimento.inicio}
                      nome={atendimento.clienteNome}
                      telefone={atendimento.clienteTelefone}
                    />
                  )}
                </CardDetail>
                <CardActions>
                  {atendimento.status === 'AGENDADO' && (
                    <>
                      <Button variant="ghost" size="sm" onClick={() => abrirRemarcar(atendimento)}>
                        Remarcar
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setAcaoStatus({ atendimento, acao: 'concluir' })}
                      >
                        Concluir
                      </Button>
                    </>
                  )}
                  {(atendimento.status === 'AGENDADO' || atendimento.status === 'CONCLUIDO') && (
                    <Button
                      variant="dangerText"
                      size="sm"
                      onClick={() => setAcaoStatus({ atendimento, acao: 'cancelar' })}
                    >
                      Cancelar
                    </Button>
                  )}
                </CardActions>
              </CardItem>
            ))}
          </CardList>
          <div className="hidden border-t border-hairline md:block">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">
                  <th className="py-3 pr-8 font-medium">Horário</th>
                  <th className="py-3 pr-8 font-medium">Cliente</th>
                  <th className="py-3 pr-8 font-medium">Serviços</th>
                  <th className="py-3 pr-8 font-medium">Status</th>
                  <th className="py-3 pr-8 font-medium">Total</th>
                  <th className="py-3 text-right font-medium">Ações</th>
                </tr>
              </thead>
              <tbody>
                {itens.map((atendimento) => (
                  <tr
                    key={atendimento.id}
                    className="border-t border-hairline transition-colors duration-150 ease-in-out hover:bg-paper"
                  >
                    <td className="py-4 pr-8 font-mono text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                      {atendimento.inicio.slice(11, 16)}–{atendimento.fim.slice(11, 16)}
                    </td>
                    <td className="py-4 pr-8 font-medium text-ink">{atendimento.clienteNome}</td>
                    <td className="max-w-xs truncate py-4 pr-8 text-ink-soft">
                      {atendimento.servicos.join(', ') || '—'}
                    </td>
                    <td className="py-4 pr-8">
                      <StatusBadge status={atendimento.status} />
                    </td>
                    <td className="py-4 pr-8 font-mono text-[13px] text-accent-strong">
                      {formatMoeda(atendimento.valorTotal)}
                    </td>
                    <td className="py-4 text-right whitespace-nowrap">
                      <LembrarLink
                        inicio={atendimento.inicio}
                        nome={atendimento.clienteNome}
                        telefone={atendimento.clienteTelefone}
                      />
                      {atendimento.status === 'AGENDADO' && (
                        <>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="ml-4"
                            onClick={() => abrirRemarcar(atendimento)}
                          >
                            Remarcar
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="ml-4"
                            onClick={() => setAcaoStatus({ atendimento, acao: 'concluir' })}
                          >
                            Concluir
                          </Button>
                        </>
                      )}
                      {(atendimento.status === 'AGENDADO' || atendimento.status === 'CONCLUIDO') && (
                        <Button
                          variant="dangerText"
                          size="sm"
                          className="ml-4"
                          onClick={() => setAcaoStatus({ atendimento, acao: 'cancelar' })}
                        >
                          Cancelar
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      <Modal open={criando} title="Novo agendamento" onClose={() => setCriando(false)}>
        <div className="flex max-h-[70vh] flex-col gap-5 overflow-y-auto pr-1">
          <Select label="Cliente *" value={clienteId} onChange={(e) => setClienteId(e.target.value)}>
            <option value="">Selecione o cliente...</option>
            {loadingClientes ? (
              <option disabled>Carregando clientes...</option>
            ) : (
              clientes?.map((cliente) => (
                <option key={cliente.id} value={cliente.id}>
                  {cliente.nome}
                </option>
              ))
            )}
          </Select>

          <TextField
            label="Data e horário *"
            type="datetime-local"
            value={dataAtendimento}
            onChange={(e) => setDataAtendimento(e.target.value)}
          />

          <div className="border border-hairline bg-ivory p-4">
            <span className={`mb-3 block ${microLabel}`}>Buscar horários livres</span>
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
              <Select
                label="Serviço"
                value={buscaServico}
                onChange={(e) => setBuscaServico(e.target.value)}
                className="flex-1"
              >
                <option value="">Selecione...</option>
                {loadingServicos ? (
                  <option disabled>Carregando serviços...</option>
                ) : (
                  servicos?.map((servico) => (
                    <option key={servico.id} value={servico.id}>
                      {servico.nome}
                    </option>
                  ))
                )}
              </Select>
              <TextField
                label="Dia"
                type="date"
                min={hojeISO()}
                value={buscaData}
                onChange={(e) => setBuscaData(e.target.value)}
                className="w-full sm:w-44"
              />
              <Button variant="secondary" size="sm" className="self-start sm:mb-1" onClick={buscarDisponibilidade} disabled={buscando}>
                {buscando ? 'Buscando...' : 'Buscar'}
              </Button>
            </div>
            {buscarErro && <p className="mt-3 text-sm text-red-600">{buscarErro}</p>}
            {disponibilidade &&
              (disponibilidade.horarios.length === 0 ? (
                <p className="mt-3 text-sm text-ink-soft">Nenhum horário livre no dia.</p>
              ) : (
                <div className="mt-3 flex flex-wrap gap-2">
                  {disponibilidade.horarios.map((hora) => (
                    <button
                      key={hora}
                      type="button"
                      className="cursor-pointer border border-hairline px-3 py-1.5 font-mono text-[12px] text-ink transition-colors duration-150 ease-in-out hover:border-accent hover:text-accent-strong"
                      onClick={() => setDataAtendimento(`${disponibilidade.data.slice(0, 10)}T${hora.slice(0, 5)}`)}
                    >
                      {hora.slice(0, 5)}
                    </button>
                  ))}
                </div>
              ))}
          </div>

          <div>
            <span className={`mb-2 block ${microLabel}`}>Serviços *</span>
            <div className="flex flex-col gap-3">
              {servicosForm.map((row, rowIndex) => (
                <div key={rowIndex} className="border border-hairline bg-ivory p-4">
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
                    <Select
                      label="Serviço"
                      value={row.servicoId}
                      onChange={(e) => atualizarServico(rowIndex, e.target.value)}
                      className="flex-1"
                    >
                      <option value="">Selecione...</option>
                      {loadingServicos ? (
                        <option disabled>Carregando serviços...</option>
                      ) : (
                        servicos?.map((servico) => (
                          <option key={servico.id} value={servico.id}>
                            {servico.nome}
                          </option>
                        ))
                      )}
                    </Select>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="self-start sm:mt-5"
                      onClick={() => removerServico(rowIndex)}
                      disabled={servicosForm.length === 1}
                    >
                      Remover
                    </Button>
                  </div>

                  {row.itensExtras.length > 0 && (
                    <div className="mt-4 flex flex-col gap-3">
                      {row.itensExtras.map((extra, extraIndex) => (
                        <div key={extraIndex} className="flex flex-col gap-2 sm:flex-row sm:items-end">
                          <Select
                            label="Item extra"
                            value={extra.itemId}
                            onChange={(e) =>
                              atualizarItemExtra(rowIndex, extraIndex, { itemId: e.target.value })
                            }
                            className="flex-1"
                          >
                            <option value="">Selecione...</option>
                            {itensDisponiveis?.map((item) => (
                              <option key={item.id} value={item.id}>
                                {item.nome}
                              </option>
                            ))}
                          </Select>
                          <TextField
                            label="Qtd"
                            type="number"
                            min={0}
                            step="0.01"
                            value={extra.quantidade}
                            onChange={(e) =>
                              atualizarItemExtra(rowIndex, extraIndex, { quantidade: Number(e.target.value) })
                            }
                            className="w-full sm:w-24"
                          />
                          <Button
                            variant="ghost"
                            size="sm"
                            className="self-start sm:self-auto"
                            onClick={() => removerItemExtra(rowIndex, extraIndex)}
                          >
                            remover
                          </Button>
                        </div>
                      ))}
                    </div>
                  )}
                  <Button
                    variant="ghost"
                    size="sm"
                    className="mt-3"
                    onClick={() => adicionarItemExtra(rowIndex)}
                  >
                    + Item extra
                  </Button>
                </div>
              ))}
            </div>
            <Button variant="secondary" size="sm" className="mt-3" onClick={adicionarServico}>
              + Adicionar serviço
            </Button>
          </div>

          {criarErro && <p className="text-sm text-red-600">{criarErro}</p>}

          <div className="mt-2 flex justify-end gap-3">
            <Button variant="ghost" onClick={() => setCriando(false)}>
              Cancelar
            </Button>
            <Button onClick={salvarAtendimento} disabled={salvando}>
              {salvando ? 'Salvando...' : 'Agendar'}
            </Button>
          </div>
        </div>
      </Modal>

      <Modal open={remarcando !== null} title="Remarcar atendimento" onClose={() => setRemarcando(null)}>
        <div className="flex flex-col gap-5">
          <p className="text-sm text-ink-soft">
            Cliente: <span className="font-medium text-ink">{remarcando?.clienteNome}</span>
          </p>
          <TextField
            label="Nova data e horário *"
            type="datetime-local"
            value={novaData}
            onChange={(e) => setNovaData(e.target.value)}
          />
          {remarcarErro && <p className="text-sm text-red-600">{remarcarErro}</p>}
          <div className="mt-2 flex justify-end gap-3">
            <Button variant="ghost" onClick={() => setRemarcando(null)}>
              Cancelar
            </Button>
            <Button onClick={confirmarRemarcar} disabled={salvando}>
              {salvando ? 'Salvando...' : 'Confirmar remarcação'}
            </Button>
          </div>
        </div>
      </Modal>

      <Modal open={horariosAberto} title="Horário de atendimento" onClose={() => setHorariosAberto(false)}>
        <div className="flex flex-col gap-4">
          <p className="text-sm text-ink-soft">
            Defina o expediente usado para sugerir horários livres. Agendamentos fora do expediente serão
            recusados.
          </p>
          {salvandoHorarios ? (
            <Spinner />
          ) : (
            horarios.map((horario) => (
              <div key={horario.diaSemana} className="flex flex-col gap-2 border border-hairline bg-ivory p-4 sm:flex-row sm:items-end">
                <div className="flex-1">
                  <p className={`${microLabel} ${horario.ativo ? '' : 'opacity-50'}`}>{DIAS[horario.diaSemana - 1]}</p>
                </div>
                <div className="flex flex-col gap-2 sm:flex-row sm:items-end">
                  <TextField
                    label="Abertura"
                    type="time"
                    value={horario.abertura}
                    onChange={(e) =>
                      setHorarios(
                        horarios.map((h) =>
                          h.diaSemana === horario.diaSemana ? { ...h, abertura: e.target.value } : h,
                        ),
                      )
                    }
                    className="w-full sm:w-32"
                  />
                  <TextField
                    label="Fechamento"
                    type="time"
                    value={horario.fechamento}
                    onChange={(e) =>
                      setHorarios(
                        horarios.map((h) =>
                          h.diaSemana === horario.diaSemana ? { ...h, fechamento: e.target.value } : h,
                        ),
                      )
                    }
                    className="w-full sm:w-32"
                  />
                  <Button
                    variant={horario.ativo ? 'secondary' : 'ghost'}
                    size="sm"
                    className="self-start sm:mb-1"
                    onClick={() =>
                      setHorarios(
                        horarios.map((h) =>
                          h.diaSemana === horario.diaSemana ? { ...h, ativo: !h.ativo } : h,
                        ),
                      )
                    }
                  >
                    {horario.ativo ? 'Ativo' : 'Fechado'}
                  </Button>
                </div>
              </div>
            ))
          )}
          {horariosErro && <p className="text-sm text-red-600">{horariosErro}</p>}
          <div className="mt-2 flex justify-end gap-3">
            <Button variant="ghost" onClick={() => setHorariosAberto(false)}>
              Cancelar
            </Button>
            <Button onClick={salvarHorarios} disabled={salvandoHorarios || horarios.length === 0}>
              {salvandoHorarios ? 'Salvando...' : 'Salvar horários'}
            </Button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        open={acaoStatus !== null}
        title={acaoConcluindo ? 'Concluir atendimento' : 'Cancelar atendimento'}
        message={acaoMensagem}
        confirmLabel={acaoConcluindo ? 'Concluir' : 'Cancelar atendimento'}
        onConfirm={confirmarAcao}
        onCancel={() => {
          setAcaoStatus(null)
          setAcaoErro('')
        }}
      />
      {acaoErro && <ErrorBanner message={acaoErro} />}
    </>
  )
}