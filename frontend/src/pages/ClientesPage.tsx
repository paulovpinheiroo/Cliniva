import { useState } from 'react'
import { clientesApi } from '@/api/clientesApi'
import { Button } from '@/components/ui/Button'
import { CardActions, CardDetail, CardItem, CardLabel, CardList } from '@/components/ui/CardList'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Modal } from '@/components/ui/Modal'
import { PageHeader } from '@/components/ui/PageHeader'
import { Spinner } from '@/components/ui/Spinner'
import { TextField } from '@/components/ui/TextField'
import { useApi } from '@/hooks/useApi'
import type { Cliente, ClienteInput } from '@/types'

const emptyForm: ClienteInput = { nome: '', email: '', telefone: '' }

export function ClientesPage() {
  const [busca, setBusca] = useState('')
  const { data: clientes, loading, error, refetch } = useApi(
    () => clientesApi.listar(busca.trim() || undefined),
    [busca],
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
    setForm({ nome: cliente.nome, email: cliente.email ?? '', telefone: cliente.telefone })
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
        await clientesApi.atualizar(editando.id, form)
      } else {
        await clientesApi.criar(form)
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
        subtitle="Cadastro e busca de clientes da clínica"
        action={<Button onClick={abrirCriar} className="w-full lg:w-auto">Novo cliente</Button>}
      />

      <TextField
        label="Buscar por nome"
        type="search"
        placeholder="Digite um nome de cliente..."
        value={busca}
        onChange={(e) => setBusca(e.target.value)}
        className="mb-10 max-w-sm"
      />

      {error && <ErrorBanner message={error} />}

      {loading ? (
        <Spinner />
      ) : !clientes || clientes.length === 0 ? (
        <EmptyState message="Nenhum cliente encontrado. Cadastre o primeiro com o botão acima." />
      ) : (
        <>
          <CardList>
            {clientes.map((cliente) => (
              <CardItem key={cliente.id}>
                <CardLabel>{cliente.nome}</CardLabel>
                <CardDetail>{cliente.email || '—'}</CardDetail>
                <CardDetail>{cliente.telefone}</CardDetail>
                <CardActions>
                  <Button variant="ghost" size="sm" onClick={() => abrirEditar(cliente)}>
                    Editar
                  </Button>
                  <Button variant="dangerText" size="sm" onClick={() => setDeletando(cliente)}>
                    Excluir
                  </Button>
                </CardActions>
              </CardItem>
            ))}
          </CardList>
          <div className="hidden border-t border-hairline md:block">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">
                  <th className="py-3 pr-8 font-medium">Nome</th>
                  <th className="py-3 pr-8 font-medium">E-mail</th>
                  <th className="py-3 pr-8 font-medium">Telefone</th>
                  <th className="py-3 text-right font-medium">Ações</th>
                </tr>
              </thead>
            <tbody>
              {clientes.map((cliente) => (
                <tr
                  key={cliente.id}
                  className="border-t border-hairline transition-colors duration-150 ease-in-out hover:bg-paper"
                >
                  <td className="py-4 pr-8 font-medium text-ink">{cliente.nome}</td>
                  <td className="py-4 pr-8 text-ink-soft">{cliente.email || '—'}</td>
                  <td className="py-4 pr-8 font-mono text-[13px] text-ink-soft">{cliente.telefone}</td>
                  <td className="py-4 text-right whitespace-nowrap">
                    <Button variant="ghost" size="sm" onClick={() => abrirEditar(cliente)}>
                      Editar
                    </Button>
                    <Button
                      variant="dangerText"
                      size="sm"
                      className="ml-4"
                      onClick={() => setDeletando(cliente)}
                    >
                      Excluir
                    </Button>
                  </td>
                </tr>
              ))}
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
        <div className="flex flex-col gap-5">
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