import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { atendimentosApi } from '@/api/atendimentosApi'
import { clientesApi } from '@/api/clientesApi'
import { Button } from '@/components/ui/Button'
import { CardActions, CardDetail, CardItem, CardLabel, CardList } from '@/components/ui/CardList'
import { ClienteStatusBadge } from '@/components/ui/ClienteStatusBadge'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Modal } from '@/components/ui/Modal'
import { PageHeader } from '@/components/ui/PageHeader'
import { Select } from '@/components/ui/Select'
import { Spinner } from '@/components/ui/Spinner'
import { TextField } from '@/components/ui/TextField'
import { useApi } from '@/hooks/useApi'
import type { CanalPreferido, Cliente, ClienteInput, ClienteStatus, OrigemCliente } from '@/types'
import { fidelidadeCor, fidelidadeLabel } from '@/utils/format'

const emptyForm: ClienteInput = { nome: '', email: '', telefone: '' }

function sanitizarClienteInput(form: ClienteInput): ClienteInput {
  const limpo: ClienteInput = { nome: form.nome, email: form.email, telefone: form.telefone }
  if (form.dataNascimento) limpo.dataNascimento = form.dataNascimento
  if (form.status) limpo.status = form.status
  if (form.origem) limpo.origem = form.origem
  if (form.canalPreferido) limpo.canalPreferido = form.canalPreferido
  if (form.preferencias) limpo.preferencias = form.preferencias
  if (form.observacoes) limpo.observacoes = form.observacoes
  return limpo
}

function contagemConcluidosPorCliente(atendimentos: { clienteId: string; status: string }[]) {
  const contagem = new Map<string, number>()
  for (const atendimento of atendimentos) {
    if (atendimento.status !== 'CONCLUIDO') continue
    contagem.set(atendimento.clienteId, (contagem.get(atendimento.clienteId) ?? 0) + 1)
  }
  return contagem
}

export function ClientesPage() {
  const navigate = useNavigate()
  const [busca, setBusca] = useState('')
  const [filtroStatus, setFiltroStatus] = useState('')
  const { data: clientes, loading, error, refetch } = useApi(
    () => clientesApi.listar(busca.trim() || undefined, (filtroStatus as ClienteStatus) || undefined),
    [busca, filtroStatus],
  )
  const { data: concluidos } = useApi(() => atendimentosApi.listar({ status: 'CONCLUIDO' }), [])

  const atendimentosPorCliente = useMemo(
    () => contagemConcluidosPorCliente(concluidos ?? []),
    [concluidos],
  )

  const [modalAberto, setModalAberto] = useState(false)
  const [editando, setEditando] = useState<Cliente | null>(null)
  const [form, setForm] = useState<ClienteInput>(emptyForm)
  const [formErro, setFormErro] = useState('')
  const [salvando, setSalvando] = useState(false)

  const [deletando, setDeletando] = useState<Cliente | null>(null)
  const [deleteErro, setDeleteErro] = useState('')

  const abrirCriar = () => {
    setEditando(null)
    setForm(emptyForm)
    setFormErro('')
    setModalAberto(true)
  }

  const abrirEditar = (cliente: Cliente) => {
    setEditando(cliente)
    setForm({
      nome: cliente.nome,
      email: cliente.email ?? '',
      telefone: cliente.telefone,
      dataNascimento: cliente.dataNascimento ?? '',
      status: cliente.status,
      origem: cliente.origem ?? '',
      canalPreferido: cliente.canalPreferido ?? '',
      preferencias: cliente.preferencias ?? '',
      observacoes: cliente.observacoes ?? '',
    })
    setFormErro('')
    setModalAberto(true)
  }

  const salvar = async () => {
    if (!form.nome.trim() || !form.telefone.trim()) {
      setFormErro('Nome e telefone são obrigatórios.')
      return
    }
    setSalvando(true)
    setFormErro('')
    try {
      if (editando) {
        await clientesApi.atualizar(editando.id, sanitizarClienteInput(form))
      } else {
        await clientesApi.criar(sanitizarClienteInput(form))
      }
      setModalAberto(false)
      refetch()
    } catch (err) {
      setFormErro(err instanceof Error ? err.message : 'Falha ao salvar cliente.')
    } finally {
      setSalvando(false)
    }
  }

  const confirmarDelete = async () => {
    if (!deletando) return
    setSalvando(true)
    setDeleteErro('')
    try {
      await clientesApi.deletar(deletando.id)
      setDeletando(null)
      refetch()
    } catch (err) {
      setDeleteErro(err instanceof Error ? err.message : 'Falha ao deletar cliente.')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <>
      <PageHeader
        kicker="Cadastro"
        title="Clientes"
        subtitle="Cadastro, perfil e fidelização dos clientes da clínica"
        action={<Button onClick={abrirCriar} className="w-full lg:w-auto">Novo cliente</Button>}
      />

      <div className="mb-10 grid grid-cols-1 gap-x-12 gap-y-6 border-b border-hairline pb-8 md:grid-cols-2">
        <TextField
          label="Buscar por nome"
          type="search"
          placeholder="Digite um nome de cliente..."
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          className="max-w-sm"
        />
        <Select label="Status" value={filtroStatus} onChange={(e) => setFiltroStatus(e.target.value)}>
          <option value="">Todos</option>
          <option value="PROSPECT">Prospect</option>
          <option value="ATIVO">Ativo</option>
          <option value="INATIVO">Inativo</option>
        </Select>
      </div>

      {error && <ErrorBanner message={error} />}

      {loading ? (
        <Spinner />
      ) : !clientes || clientes.length === 0 ? (
        <EmptyState message="Nenhum cliente encontrado. Cadastre o primeiro com o botão acima." />
      ) : (
        <>
          <CardList>
            {clientes.map((cliente) => {
              const total = atendimentosPorCliente.get(cliente.id) ?? 0
              return (
                <CardItem key={cliente.id} onClick={() => navigate(`/clientes/${cliente.id}`)}>
                  <div className="flex items-start justify-between gap-3">
                    <CardLabel>{cliente.nome}</CardLabel>
                    <div className="shrink-0">
                      <ClienteStatusBadge status={cliente.status} />
                    </div>
                  </div>
                  <CardDetail>{cliente.email || cliente.telefone}</CardDetail>
                  <span className={`mt-1 block text-[11px] font-medium uppercase tracking-[0.14em] ${fidelidadeCor(total)}`}>
                    {fidelidadeLabel(total)}
                  </span>
                  <CardActions>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation()
                        abrirEditar(cliente)
                      }}
                    >
                      Editar
                    </Button>
                    <Button
                      variant="dangerText"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation()
                        setDeletando(cliente)
                      }}
                    >
                      Excluir
                    </Button>
                  </CardActions>
                </CardItem>
              )
            })}
          </CardList>
          <div className="hidden border-t border-hairline md:block">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">
                  <th className="py-3 pr-8 font-medium">Nome</th>
                  <th className="py-3 pr-8 font-medium">Status</th>
                  <th className="py-3 pr-8 font-medium">Fidelidade</th>
                  <th className="py-3 pr-8 font-medium">E-mail</th>
                  <th className="py-3 text-right font-medium">Ações</th>
                </tr>
              </thead>
            <tbody>
              {clientes.map((cliente) => {
                const total = atendimentosPorCliente.get(cliente.id) ?? 0
                return (
                  <tr
                    key={cliente.id}
                    onClick={() => navigate(`/clientes/${cliente.id}`)}
                    className="cursor-pointer border-t border-hairline transition-colors duration-150 ease-in-out hover:bg-paper"
                  >
                    <td className="py-4 pr-8 font-medium text-ink">{cliente.nome}</td>
                    <td className="py-4 pr-8">
                      <ClienteStatusBadge status={cliente.status} />
                    </td>
                    <td className={`py-4 pr-8 text-[11px] font-medium uppercase tracking-[0.14em] ${fidelidadeCor(total)}`}>
                      {fidelidadeLabel(total)}
                    </td>
                    <td className="py-4 pr-8 text-ink-soft">{cliente.email || '—'}</td>
                    <td className="py-4 text-right whitespace-nowrap">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={(e) => {
                          e.stopPropagation()
                          abrirEditar(cliente)
                        }}
                      >
                        Editar
                      </Button>
                      <Button
                        variant="dangerText"
                        size="sm"
                        className="ml-4"
                        onClick={(e) => {
                          e.stopPropagation()
                          setDeletando(cliente)
                        }}
                      >
                        Excluir
                      </Button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
          </div>
        </>
      )}

      <Modal
        open={modalAberto}
        title={editando ? 'Editar cliente' : 'Novo cliente'}
        onClose={() => setModalAberto(false)}
      >
        <div className="flex max-h-[70vh] flex-col gap-5 overflow-y-auto pr-1">
          <TextField
            label="Nome *"
            value={form.nome}
            onChange={(e) => setForm({ ...form, nome: e.target.value })}
            placeholder="Nome completo"
          />
          <TextField
            label="Telefone *"
            value={form.telefone}
            onChange={(e) => setForm({ ...form, telefone: e.target.value })}
            placeholder="(11) 99999-9999"
          />
          <TextField
            label="E-mail"
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            placeholder="cliente@email.com"
          />
          <TextField
            label="Data de nascimento"
            type="date"
            value={form.dataNascimento ?? ''}
            onChange={(e) => setForm({ ...form, dataNascimento: e.target.value })}
          />
          {editando && (
            <Select
              label="Status"
              value={form.status ?? 'PROSPECT'}
              onChange={(e) => setForm({ ...form, status: e.target.value as ClienteStatus })}
            >
              <option value="PROSPECT">Prospect</option>
              <option value="ATIVO">Ativo</option>
              <option value="INATIVO">Inativo</option>
            </Select>
          )}
          <Select
            label="Origem"
            value={form.origem ?? ''}
            onChange={(e) => setForm({ ...form, origem: e.target.value as OrigemCliente })}
          >
            <option value="">Não informado</option>
            <option value="INDICACAO">Indicação</option>
            <option value="INSTAGRAM">Instagram</option>
            <option value="GOOGLE">Google</option>
            <option value="PASSOU_NA_RUA">Passou na rua</option>
          </Select>
          <Select
            label="Canal preferido"
            value={form.canalPreferido ?? ''}
            onChange={(e) => setForm({ ...form, canalPreferido: e.target.value as CanalPreferido })}
          >
            <option value="">Não informado</option>
            <option value="WHATSAPP">WhatsApp</option>
            <option value="INSTAGRAM">Instagram</option>
            <option value="EMAIL">E-mail</option>
            <option value="LIGACAO">Ligação</option>
          </Select>
          <TextField
            label="Preferências"
            value={form.preferencias ?? ''}
            onChange={(e) => setForm({ ...form, preferencias: e.target.value })}
            placeholder="Estilo, horários, produtos preferidos..."
          />
          <TextField
            label="Observações"
            value={form.observacoes ?? ''}
            onChange={(e) => setForm({ ...form, observacoes: e.target.value })}
            placeholder="Restrições, alergias, histórico relevante..."
          />
          {formErro && <p className="text-sm text-red-600">{formErro}</p>}
          <div className="mt-2 flex justify-end gap-3">
            <Button variant="ghost" onClick={() => setModalAberto(false)}>
              Cancelar
            </Button>
            <Button onClick={salvar} disabled={salvando}>
              {salvando ? 'Salvando...' : editando ? 'Salvar alterações' : 'Cadastrar'}
            </Button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        open={deletando !== null}
        title="Excluir cliente"
        message={`Tem certeza que deseja excluir "${deletando?.nome}"? Esta ação não pode ser desfeita.`}
        confirmLabel="Excluir"
        onConfirm={confirmarDelete}
        onCancel={() => {
          setDeletando(null)
          setDeleteErro('')
        }}
      />
      {deleteErro && <ErrorBanner message={deleteErro} />}
    </>
  )
}