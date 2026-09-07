import { useState } from 'react'
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
  AtendimentoInput,
  AtendimentoResumo,
  Cliente,
  Item,
  Servico,
  StatusAtendimento,
} from '@/types'
import { formatDataHora, formatMoeda } from '@/utils/format'

interface ItemExtraRow {
  itemId: string
  quantidade: number
}

interface ServicoRow {
  servicoId: string
  itensExtras: ItemExtraRow[]
}

const emptyServicoRow: ServicoRow = { servicoId: '', itensExtras: [] }

function normalizeDatetimeLocal(value: string): string {
  return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value) ? `${value}:00` : value
}

const microLabel = 'text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft'

export function AtendimentosPage() {
  const { data: clientes, loading: loadingClientes } = useApi<Cliente[]>(() => clientesApi.listar())
  const { data: servicos, loading: loadingServicos } = useApi<Servico[]>(() => servicosApi.listar())
  const { data: itensDisponiveis } = useApi<Item[]>(() => itensApi.listar())

  const [filtroStatus, setFiltroStatus] = useState('')
  const [filtroCliente, setFiltroCliente] = useState('')
  const [filtroDataInicio, setFiltroDataInicio] = useState('')
  const [filtroDataFim, setFiltroDataFim] = useState('')

  const filtros = {
    status: (filtroStatus || undefined) as StatusAtendimento | undefined,
    clienteId: filtroCliente || undefined,
    dataInicio: filtroDataInicio ? normalizeDatetimeLocal(filtroDataInicio) : undefined,
    dataFim: filtroDataFim ? normalizeDatetimeLocal(filtroDataFim) : undefined,
  }

  const { data: atendimentos, loading, error, refetch } = useApi(
    () => atendimentosApi.listar(filtros),
    [filtroStatus, filtroCliente, filtroDataInicio, filtroDataFim],
  )

  const [criando, setCriando] = useState(false)
  const [clienteId, setClienteId] = useState('')
  const [dataAtendimento, setDataAtendimento] = useState('')
  const [servicosForm, setServicosForm] = useState<ServicoRow[]>([emptyServicoRow])
  const [criarErro, setCriarErro] = useState('')
  const [salvando, setSalvando] = useState(false)

  const [acaoStatus, setAcaoStatus] = useState<{
    atendimento: AtendimentoResumo
    acao: 'concluir' | 'cancelar'
  } | null>(null)
  const [acaoErro, setAcaoErro] = useState('')

  const abrirCriar = () => {
    setClienteId('')
    setDataAtendimento('')
    setServicosForm([emptyServicoRow])
    setCriarErro('')
    setCriando(true)
  }

  const atualizarServico = (index: number, servicoId: string) => {
    setServicosForm(servicosForm.map((row, i) => (i === index ? { ...row, servicoId } : row)))
  }

  const adicionarServico = () => {
    setServicosForm([...servicosForm, emptyServicoRow])
  }

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
              itensExtras: row.itensExtras.map((extra, j) => (j === extraIndex ? { ...extra, ...patch } : extra)),
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

  const acaoConcluindo = acaoStatus?.acao === 'concluir'
  const acaoMensagem = acaoStatus
    ? acaoConcluindo
      ? `Confirmar conclusão do atendimento de ${acaoStatus.atendimento.nomeCliente}?`
      : `Confirmar cancelamento do atendimento de ${acaoStatus.atendimento.nomeCliente}?`
    : ''

  return (
    <>
      <PageHeader
        kicker="Agenda"
        title="Atendimentos"
        subtitle="Agenda de procedimentos e mudança de status"
        action={<Button onClick={abrirCriar} className="w-full lg:w-auto">Novo atendimento</Button>}
      />

      <div className="mb-10 grid grid-cols-1 gap-x-12 gap-y-6 border-b border-hairline pb-8 md:grid-cols-2 xl:grid-cols-4">
        <Select label="Status" value={filtroStatus} onChange={(e) => setFiltroStatus(e.target.value)}>
          <option value="">Todos</option>
          <option value="AGENDADO">Agendado</option>
          <option value="CONCLUIDO">Concluído</option>
          <option value="CANCELADO">Cancelado</option>
        </Select>
        <Select label="Cliente" value={filtroCliente} onChange={(e) => setFiltroCliente(e.target.value)}>
          <option value="">Todos</option>
          {clientes?.map((cliente) => (
            <option key={cliente.id} value={cliente.id}>
              {cliente.nome}
            </option>
          ))}
        </Select>
        <TextField
          label="De"
          type="datetime-local"
          value={filtroDataInicio}
          onChange={(e) => setFiltroDataInicio(e.target.value)}
        />
        <TextField
          label="Até"
          type="datetime-local"
          value={filtroDataFim}
          onChange={(e) => setFiltroDataFim(e.target.value)}
        />
      </div>

      {error && <ErrorBanner message={error} />}

      {loading ? (
        <Spinner />
      ) : !atendimentos || atendimentos.length === 0 ? (
        <EmptyState message="Nenhum atendimento encontrado para os filtros atuais." />
      ) : (
        <>
          <CardList>
            {atendimentos.map((atendimento) => (
              <CardItem key={atendimento.id}>
                <div className="flex items-start justify-between gap-3">
                  <CardLabel>{atendimento.nomeCliente}</CardLabel>
                  <div className="shrink-0">
                    <StatusBadge status={atendimento.status} />
                  </div>
                </div>
                <CardDetail>{formatDataHora(atendimento.dataAtendimento)}</CardDetail>
                <p className="mt-0.5 font-mono text-[13px] text-accent-strong">
                  {formatMoeda(atendimento.valorTotal)}
                </p>
                <CardActions>
                  {atendimento.status === 'AGENDADO' && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setAcaoStatus({ atendimento, acao: 'concluir' })}
                    >
                      Concluir
                    </Button>
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
                  <th className="py-3 pr-8 font-medium">Cliente</th>
                  <th className="py-3 pr-8 font-medium">Data</th>
                  <th className="py-3 pr-8 font-medium">Status</th>
                  <th className="py-3 pr-8 font-medium">Total</th>
                  <th className="py-3 text-right font-medium">Ações</th>
                </tr>
              </thead>
            <tbody>
              {atendimentos.map((atendimento) => (
                <tr
                  key={atendimento.id}
                  className="border-t border-hairline transition-colors duration-150 ease-in-out hover:bg-paper"
                >
                  <td className="py-4 pr-8 font-medium text-ink">{atendimento.nomeCliente}</td>
                  <td className="py-4 pr-8 font-mono text-[11px] uppercase tracking-[0.14em] text-ink-soft">
                    {formatDataHora(atendimento.dataAtendimento)}
                  </td>
                  <td className="py-4 pr-8">
                    <StatusBadge status={atendimento.status} />
                  </td>
                  <td className="py-4 pr-8 font-mono text-[13px] text-accent-strong">
                    {formatMoeda(atendimento.valorTotal)}
                  </td>
                  <td className="py-4 text-right whitespace-nowrap">
                    {atendimento.status === 'AGENDADO' && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setAcaoStatus({ atendimento, acao: 'concluir' })}
                      >
                        Concluir
                      </Button>
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

      <Modal open={criando} title="Novo atendimento" onClose={() => setCriando(false)}>
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
                            onChange={(e) => atualizarItemExtra(rowIndex, extraIndex, { itemId: e.target.value })}
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