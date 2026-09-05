import { useState } from 'react'
import { servicosApi } from '@/api/servicosApi'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Modal } from '@/components/ui/Modal'
import { PageHeader } from '@/components/ui/PageHeader'
import { Spinner } from '@/components/ui/Spinner'
import { TextField } from '@/components/ui/TextField'
import { useApi } from '@/hooks/useApi'
import type { Servico, ServicoInput } from '@/types'
import { formatMoeda } from '@/utils/format'

const emptyForm: ServicoInput = { nome: '', descricao: '', valor: 0 }

export function ServicosPage() {
  const { data: servicos, loading, error, refetch } = useApi(() => servicosApi.listar())

  const [modalAberto, setModalAberto] = useState(false)
  const [editando, setEditando] = useState<Servico | null>(null)
  const [form, setForm] = useState<ServicoInput>(emptyForm)
  const [formErro, setFormErro] = useState('')
  const [salvando, setSalvando] = useState(false)

  const [deletando, setDeletando] = useState<Servico | null>(null)
  const [deleteErro, setDeleteErro] = useState('')

  const abrirCriar = () => {
    setEditando(null)
    setForm(emptyForm)
    setFormErro('')
    setModalAberto(true)
  }

  const abrirEditar = (servico: Servico) => {
    setEditando(servico)
    setForm({ nome: servico.nome, descricao: servico.descricao ?? '', valor: servico.valor })
    setFormErro('')
    setModalAberto(true)
  }

  const salvar = async () => {
    if (!form.nome.trim()) {
      setFormErro('Nome é obrigatório.')
      return
    }
    if (!form.valor || form.valor <= 0) {
      setFormErro('Valor deve ser maior que zero.')
      return
    }
    setSalvando(true)
    setFormErro('')
    try {
      if (editando) {
        await servicosApi.atualizar(editando.id, form)
      } else {
        await servicosApi.criar(form)
      }
      setModalAberto(false)
      refetch()
    } catch (err) {
      setFormErro(err instanceof Error ? err.message : 'Falha ao salvar serviço.')
    } finally {
      setSalvando(false)
    }
  }

  const confirmarDelete = async () => {
    if (!deletando) return
    setSalvando(true)
    setDeleteErro('')
    try {
      await servicosApi.deletar(deletando.id)
      setDeletando(null)
      refetch()
    } catch (err) {
      setDeleteErro(err instanceof Error ? err.message : 'Falha ao deletar serviço.')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <>
      <PageHeader
        kicker="Catálogo"
        title="Serviços"
        subtitle="Procedimentos oferecidos pela clínica"
        action={<Button onClick={abrirCriar}>Novo serviço</Button>}
      />

      {error && <ErrorBanner message={error} />}

      {loading ? (
        <Spinner />
      ) : !servicos || servicos.length === 0 ? (
        <EmptyState message="Nenhum serviço cadastrado. Adicione o primeiro com o botão acima." />
      ) : (
        <div className="border-t border-hairline">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">
                <th className="py-3 pr-8 font-medium">Nome</th>
                <th className="py-3 pr-8 font-medium">Descrição</th>
                <th className="py-3 pr-8 font-medium">Valor</th>
                <th className="py-3 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody>
              {servicos.map((servico) => (
                <tr
                  key={servico.id}
                  className="border-t border-hairline transition-colors duration-150 ease-in-out hover:bg-paper"
                >
                  <td className="py-4 pr-8 font-medium text-ink">{servico.nome}</td>
                  <td className="max-w-md truncate py-4 pr-8 text-ink-soft">
                    {servico.descricao || '—'}
                  </td>
                  <td className="py-4 pr-8 font-mono text-[13px] text-sage-dark">
                    {formatMoeda(servico.valor)}
                  </td>
                  <td className="py-4 text-right whitespace-nowrap">
                    <Button variant="ghost" size="sm" onClick={() => abrirEditar(servico)}>
                      Editar
                    </Button>
                    <Button
                      variant="dangerText"
                      size="sm"
                      className="ml-4"
                      onClick={() => setDeletando(servico)}
                    >
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
        title={editando ? 'Editar serviço' : 'Novo serviço'}
        onClose={() => setModalAberto(false)}
      >
        <div className="flex flex-col gap-5">
          <TextField
            label="Nome *"
            value={form.nome}
            onChange={(e) => setForm({ ...form, nome: e.target.value })}
            placeholder="Ex.: Limpeza de pele"
          />
          <TextField
            label="Descrição"
            value={form.descricao}
            onChange={(e) => setForm({ ...form, descricao: e.target.value })}
            placeholder="Descrição do procedimento (opcional)"
          />
          <TextField
            label="Valor (R$) *"
            type="number"
            min={0}
            step="0.01"
            value={form.valor === 0 ? '' : form.valor}
            onChange={(e) => setForm({ ...form, valor: Number(e.target.value) })}
            placeholder="0,00"
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
        title="Excluir serviço"
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