import { useState } from 'react'
import { clientesApi } from '@/api/clientesApi'
import { Button } from '@/components/ui/Button'
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
        title="Clientes"
        subtitle="Cadastro e busca de clientes da clínica"
        action={<Button onClick={abrirCriar}>Novo cliente</Button>}
      />

      <TextField
        label="Buscar por nome"
        type="search"
        placeholder="Digite um nome de cliente..."
        value={busca}
        onChange={(e) => setBusca(e.target.value)}
        className="mb-4 max-w-xs"
      />

      {error && <ErrorBanner message={error} />}

      {loading ? (
        <Spinner />
      ) : !clientes || clientes.length === 0 ? (
        <EmptyState message="Nenhum cliente encontrado. Cadastre o primeiro com o botão acima." />
      ) : (
        <div className="overflow-hidden rounded-xl border border-borderline">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-borderline bg-surface text-left text-xs uppercase tracking-wide text-lilac">
                <th className="px-4 py-3 font-medium">Nome</th>
                <th className="px-4 py-3 font-medium">E-mail</th>
                <th className="px-4 py-3 font-medium">Telefone</th>
                <th className="px-4 py-3 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody>
              {clientes.map((cliente) => (
                <tr key={cliente.id} className="border-b border-borderline/50 last:border-0">
                  <td className="px-4 py-3 text-white">{cliente.nome}</td>
                  <td className="px-4 py-3 text-slate-400">{cliente.email || '—'}</td>
                  <td className="px-4 py-3 text-slate-300">{cliente.telefone}</td>
                  <td className="px-4 py-3 text-right">
                    <Button variant="secondary" size="sm" onClick={() => abrirEditar(cliente)}>
                      Editar
                    </Button>
                    <Button variant="danger" size="sm" onClick={() => setDeletando(cliente)}>
                      Excluir
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal
        open={modalAberto}
        title={editando ? 'Editar cliente' : 'Novo cliente'}
        onClose={() => setModalAberto(false)}
      >
        <div className="flex flex-col gap-4">
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
          {formErro && <p className="text-sm text-red-400">{formErro}</p>}
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