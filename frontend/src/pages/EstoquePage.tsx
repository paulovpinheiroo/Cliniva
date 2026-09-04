import { useState } from 'react'
import { itensApi } from '@/api/itensApi'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Modal } from '@/components/ui/Modal'
import { PageHeader } from '@/components/ui/PageHeader'
import { Select } from '@/components/ui/Select'
import { Spinner } from '@/components/ui/Spinner'
import { TextField } from '@/components/ui/TextField'
import { useApi } from '@/hooks/useApi'
import type { Item, ItemInput, MovimentacaoEstoqueInput, TipoMovimentacao } from '@/types'

const emptyForm: ItemInput = { nome: '', quantidadeEmEstoque: 0 }

const MOVIMENTACAO_LABEL: Record<TipoMovimentacao, string> = {
  ENTRADA: 'Entrada',
  SAIDA: 'Saída',
}

export function EstoquePage() {
  const { data: itens, loading, error, refetch } = useApi(() => itensApi.listar())

  const [modalAberto, setModalAberto] = useState(false)
  const [editando, setEditando] = useState<Item | null>(null)
  const [form, setForm] = useState<ItemInput>(emptyForm)
  const [formErro, setFormErro] = useState('')
  const [salvando, setSalvando] = useState(false)

  const [movimentando, setMovimentando] = useState<Item | null>(null)
  const [movForm, setMovForm] = useState<MovimentacaoEstoqueInput>({ tipo: 'ENTRADA', quantidade: 1 })
  const [movErro, setMovErro] = useState('')

  const [deletando, setDeletando] = useState<Item | null>(null)
  const [deleteErro, setDeleteErro] = useState('')

  const abrirCriar = () => {
    setEditando(null)
    setForm(emptyForm)
    setFormErro('')
    setModalAberto(true)
  }

  const abrirEditar = (item: Item) => {
    setEditando(item)
    setForm({ nome: item.nome, quantidadeEmEstoque: 0 })
    setFormErro('')
    setModalAberto(true)
  }

  const abrirMovimentacao = (item: Item) => {
    setMovimentando(item)
    setMovForm({ tipo: 'ENTRADA', quantidade: 1 })
    setMovErro('')
  }

  const salvar = async () => {
    if (!form.nome.trim()) {
      setFormErro('Nome é obrigatório.')
      return
    }
    setSalvando(true)
    setFormErro('')
    try {
      if (editando) {
        await itensApi.atualizar(editando.id, { nome: form.nome })
      } else {
        await itensApi.criar({ nome: form.nome, quantidadeEmEstoque: form.quantidadeEmEstoque })
      }
      setModalAberto(false)
      refetch()
    } catch (err) {
      setFormErro(err instanceof Error ? err.message : 'Falha ao salvar item.')
    } finally {
      setSalvando(false)
    }
  }

  const confirmarMovimentacao = async () => {
    if (!movimentando) return
    if (!movForm.quantidade || movForm.quantidade <= 0) {
      setMovErro('Quantidade deve ser maior que zero.')
      return
    }
    setSalvando(true)
    setMovErro('')
    try {
      await itensApi.movimentarEstoque(movimentando.id, movForm)
      setMovimentando(null)
      refetch()
    } catch (err) {
      setMovErro(err instanceof Error ? err.message : 'Falha na movimentação.')
    } finally {
      setSalvando(false)
    }
  }

  const confirmarDelete = async () => {
    if (!deletando) return
    setSalvando(true)
    setDeleteErro('')
    try {
      await itensApi.deletar(deletando.id)
      setDeletando(null)
      refetch()
    } catch (err) {
      setDeleteErro(err instanceof Error ? err.message : 'Falha ao deletar item.')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <>
      <PageHeader
        title="Estoque"
        subtitle="Itens e materiais utilizados na clínica"
        action={<Button onClick={abrirCriar}>Novo item</Button>}
      />

      {error && <ErrorBanner message={error} />}

      {loading ? (
        <Spinner />
      ) : !itens || itens.length === 0 ? (
        <EmptyState message="Nenhum item no estoque. Cadastre o primeiro com o botão acima." />
      ) : (
        <div className="overflow-hidden rounded-xl border border-borderline">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-borderline bg-surface text-left text-xs uppercase tracking-wide text-lilac">
                <th className="px-4 py-3 font-medium">Item</th>
                <th className="px-4 py-3 font-medium">Quantidade</th>
                <th className="px-4 py-3 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.id} className="border-b border-borderline/50 last:border-0">
                  <td className="px-4 py-3 text-white">{item.nome}</td>
                  <td className="px-4 py-3">
                    <span
                      className={
                        item.quantidadeEmEstoque === 0
                          ? 'font-medium text-red-400'
                          : item.quantidadeEmEstoque < 5
                            ? 'font-medium text-yellow-400'
                            : 'font-medium text-sage'
                      }
                    >
                      {item.quantidadeEmEstoque}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Button variant="ghost" size="sm" onClick={() => abrirMovimentacao(item)}>
                      Entrada/Saída
                    </Button>
                    <Button variant="secondary" size="sm" onClick={() => abrirEditar(item)}>
                      Editar
                    </Button>
                    <Button variant="danger" size="sm" onClick={() => setDeletando(item)}>
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
        title={editando ? 'Editar item' : 'Novo item'}
        onClose={() => setModalAberto(false)}
      >
        <div className="flex flex-col gap-4">
          <TextField
            label="Nome *"
            value={form.nome}
            onChange={(e) => setForm({ ...form, nome: e.target.value })}
            placeholder="Ex.: Hidratante facial"
          />
          {!editando && (
            <TextField
              label="Quantidade inicial"
              type="number"
              min={0}
              step="0.01"
              value={form.quantidadeEmEstoque}
              onChange={(e) => setForm({ ...form, quantidadeEmEstoque: Number(e.target.value) })}
            />
          )}
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

      <Modal
        open={movimentando !== null}
        title={`Movimentar estoque — ${movimentando?.nome ?? ''}`}
        onClose={() => setMovimentando(null)}
      >
        <div className="flex flex-col gap-4">
          <p className="text-sm text-slate-400">
            Quantidade atual: <span className="font-medium text-white">{movimentando?.quantidadeEmEstoque}</span>
          </p>
          <Select
            label="Tipo de movimentação"
            value={movForm.tipo}
            onChange={(e) => setMovForm({ ...movForm, tipo: e.target.value as TipoMovimentacao })}
          >
            <option value="ENTRADA">Entrada</option>
            <option value="SAIDA">Saída</option>
          </Select>
          <TextField
            label="Quantidade *"
            type="number"
            min={0}
            step="0.01"
            value={movForm.quantidade}
            onChange={(e) => setMovForm({ ...movForm, quantidade: Number(e.target.value) })}
          />
          {movErro && <p className="text-sm text-red-400">{movErro}</p>}
          <div className="mt-2 flex justify-end gap-3">
            <Button variant="ghost" onClick={() => setMovimentando(null)}>
              Cancelar
            </Button>
            <Button onClick={confirmarMovimentacao} disabled={salvando}>
              {salvando ? 'Movimentando...' : `Confirmar ${MOVIMENTACAO_LABEL[movForm.tipo].toLowerCase()}`}
            </Button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        open={deletando !== null}
        title="Excluir item"
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