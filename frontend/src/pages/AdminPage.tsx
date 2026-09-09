import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { adminApi } from '@/api/adminApi'
import { Button } from '@/components/ui/Button'
import { CardActions, CardDetail, CardItem, CardLabel, CardList } from '@/components/ui/CardList'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorBanner } from '@/components/ui/ErrorBanner'
import { Modal } from '@/components/ui/Modal'
import { PageHeader } from '@/components/ui/PageHeader'
import { Spinner } from '@/components/ui/Spinner'
import { TextField } from '@/components/ui/TextField'
import { useApi } from '@/hooks/useApi'
import { sessionStore } from '@/lib/session'
import type {
  ClinicaDetalheResponseDTO,
  ListarClinicaResponseDTO,
} from '@/types/admin'
import { formatData, formatMoeda } from '@/utils/format'

const microLabel = 'text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft'

function SenhaTemporaria({ email, senha }: { email: string; senha: string }) {
  return (
    <div className="border border-accent bg-ivory p-4">
      <p className={microLabel}>Senha temporária para {email}</p>
      <p className="mt-2 break-all font-mono text-lg tracking-[0.12em] text-accent-strong">{senha}</p>
      <p className="mt-2 text-xs leading-relaxed text-ink-soft">
        Compartilhe a senha apenas com o responsável. Ele deve trocá-la após o primeiro acesso.
      </p>
    </div>
  )
}

export function AdminPage() {
  const navigate = useNavigate()
  const { data: clinicas, loading, error, refetch } = useApi(() => adminApi.listarClinicas())
  const { data: metricas, refetch: refetchMetricas } = useApi(() => adminApi.metricas())

  const [criando, setCriando] = useState(false)
  const [formCriar, setFormCriar] = useState({ nome: '', emailResponsavel: '', nomeResponsavel: '' })
  const [erroCriar, setErroCriar] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [resultadoCriar, setResultadoCriar] = useState<{ email: string; senha: string } | null>(null)

  const [detalhe, setDetalhe] = useState<ClinicaDetalheResponseDTO | null>(null)
  const [carregandoDetalhe, setCarregandoDetalhe] = useState(false)
  const [erroDetalhe, setErroDetalhe] = useState('')

  const [formResponsavel, setFormResponsavel] = useState({ email: '', nome: '' })
  const [erroResponsavel, setErroResponsavel] = useState('')
  const [salvandoResponsavel, setSalvandoResponsavel] = useState(false)
  const [resultadoResponsavel, setResultadoResponsavel] = useState<{ email: string; senha: string } | null>(null)

  const [senhaReset, setSenhaReset] = useState<{ email: string; senha: string } | null>(null)
  const [resetandoId, setResetandoId] = useState<string | null>(null)

  const clinicaSuporteId = sessionStore.getClinicaId()
  const clinicaSuporte = clinicas?.find((clinica) => clinica.id === clinicaSuporteId)

  const abrirCriar = () => {
    setFormCriar({ nome: '', emailResponsavel: '', nomeResponsavel: '' })
    setErroCriar('')
    setResultadoCriar(null)
    setCriando(true)
  }

  const criarClinica = async () => {
    if (!formCriar.nome.trim() || !formCriar.emailResponsavel.trim() || !formCriar.nomeResponsavel.trim()) {
      setErroCriar('Preencha nome da clínica, e-mail e nome do responsável.')
      return
    }
    setSalvando(true)
    setErroCriar('')
    try {
      const criada = await adminApi.criarClinica(formCriar)
      setResultadoCriar({ email: criada.emailResponsavel ?? '', senha: criada.senhaTemporaria ?? '' })
      refetch()
      refetchMetricas()
    } catch (err) {
      setErroCriar(err instanceof Error ? err.message : 'Falha ao criar a clínica.')
    } finally {
      setSalvando(false)
    }
  }

  const entrarComo = (clinica: ListarClinicaResponseDTO) => {
    sessionStore.setClinicaId(clinica.id)
    navigate('/')
  }

  const sairDoModoSuporte = () => {
    sessionStore.setClinicaId(null)
  }

  const abrirDetalhe = async (clinica: ListarClinicaResponseDTO) => {
    setCarregandoDetalhe(true)
    setErroDetalhe('')
    setResultadoResponsavel(null)
    setSenhaReset(null)
    setFormResponsavel({ email: '', nome: '' })
    try {
      const dados = await adminApi.detalharClinica(clinica.id)
      setDetalhe(dados)
    } catch (err) {
      setErroDetalhe(err instanceof Error ? err.message : 'Falha ao carregar a clínica.')
    } finally {
      setCarregandoDetalhe(false)
    }
  }

  const adicionarResponsavel = async () => {
    if (!detalhe) return
    if (!formResponsavel.email.trim() || !formResponsavel.nome.trim()) {
      setErroResponsavel('Preencha e-mail e nome do responsável.')
      return
    }
    setSalvandoResponsavel(true)
    setErroResponsavel('')
    try {
      const criado = await adminApi.adicionarResponsavel(detalhe.id, formResponsavel)
      setResultadoResponsavel({ email: criado.email, senha: criado.senhaTemporaria ?? '' })
      setFormResponsavel({ email: '', nome: '' })
      const atualizado = await adminApi.detalharClinica(detalhe.id)
      setDetalhe(atualizado)
      refetch()
    } catch (err) {
      setErroResponsavel(err instanceof Error ? err.message : 'Falha ao adicionar responsável.')
    } finally {
      setSalvandoResponsavel(false)
    }
  }

  const resetarSenha = async (usuarioId: string) => {
    setResetandoId(usuarioId)
    setSenhaReset(null)
    try {
      const resultado = await adminApi.resetarSenha(usuarioId)
      setSenhaReset({ email: resultado.email, senha: resultado.senhaTemporaria })
    } catch (err) {
      setErroDetalhe(err instanceof Error ? err.message : 'Falha ao resetar a senha.')
    } finally {
      setResetandoId(null)
    }
  }

  const alternarAtiva = async () => {
    if (!detalhe) return
    setErroDetalhe('')
    try {
      const atualizada = await adminApi.atualizarClinica(detalhe.id, { ativa: !detalhe.ativa })
      setDetalhe(atualizada)
      refetch()
      refetchMetricas()
    } catch (err) {
      setErroDetalhe(err instanceof Error ? err.message : 'Falha ao atualizar a clínica.')
    }
  }

  return (
    <>
      <PageHeader
        kicker="Administração"
        title="Clínicas"
        subtitle="Gerencie as clínicas, responsáveis e acompanhe as métricas gerais"
        action={<Button onClick={abrirCriar} className="w-full lg:w-auto">Nova clínica</Button>}
      />

      {clinicaSuporte && clinicaSuporteId && (
        <div className="mb-8 flex flex-col items-start justify-between gap-4 border border-accent bg-ivory p-5 md:flex-row md:items-center">
          <div>
            <p className={microLabel}>Modo suporte ativo</p>
            <p className="mt-1 text-sm text-ink">
              Consultando como <span className="font-medium text-accent-strong">{clinicaSuporte?.nome}</span>
            </p>
          </div>
          <Button variant="secondary" size="sm" onClick={sairDoModoSuporte}>
            Sair do modo suporte
          </Button>
        </div>
      )}

      {error && <ErrorBanner message={error} />}

      {metricas && (
        <div className="mb-8 grid grid-cols-2 gap-4 lg:grid-cols-4">
          <div className="border border-hairline bg-paper p-5">
            <p className={microLabel}>Clínicas</p>
            <p className="mt-2 font-mono text-2xl text-ink">{metricas.totalClinicas}</p>
          </div>
          <div className="border border-hairline bg-paper p-5">
            <p className={microLabel}>Clientes</p>
            <p className="mt-2 font-mono text-2xl text-ink">{metricas.totalClientes}</p>
          </div>
          <div className="border border-hairline bg-paper p-5">
            <p className={microLabel}>Atendimentos</p>
            <p className="mt-2 font-mono text-2xl text-ink">{metricas.totalAtendimentos}</p>
          </div>
          <div className="border border-hairline bg-paper p-5">
            <p className={microLabel}>Receita</p>
            <p className="mt-2 font-mono text-2xl text-accent-strong">
              {formatMoeda(metricas.porClinica.reduce((soma, clinica) => soma + clinica.receita, 0))}
            </p>
          </div>
        </div>
      )}

      {loading ? (
        <Spinner />
      ) : !clinicas || clinicas.length === 0 ? (
        <EmptyState message="Nenhuma clínica cadastrada. Crie a primeira com o botão acima." />
      ) : (
        <>
          <CardList>
            {clinicas.map((clinica) => (
              <CardItem key={clinica.id}>
                <CardLabel>{clinica.nome}</CardLabel>
                <CardDetail>
                  {clinica.ativa ? 'Ativa' : 'Inativa'} · {clinica.totalResponsaveis} responsáveis ·{' '}
                  {formatData(clinica.criadaEm)}
                </CardDetail>
                <CardActions>
                  <Button variant="ghost" size="sm" onClick={() => abrirDetalhe(clinica)}>
                    Detalhes
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => entrarComo(clinica)}>
                    Acessar
                  </Button>
                </CardActions>
              </CardItem>
            ))}
          </CardList>
          <div className="hidden border-t border-hairline md:block">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">
                  <th className="py-3 pr-8 font-medium">Clínica</th>
                  <th className="py-3 pr-8 font-medium">Status</th>
                  <th className="py-3 pr-8 font-medium">Responsáveis</th>
                  <th className="py-3 pr-8 font-medium">Criada em</th>
                  <th className="py-3 text-right font-medium">Ações</th>
                </tr>
              </thead>
              <tbody>
                {clinicas.map((clinica) => (
                  <tr
                    key={clinica.id}
                    className="border-t border-hairline transition-colors duration-150 ease-in-out hover:bg-paper"
                  >
                    <td className="py-4 pr-8 font-medium text-ink">{clinica.nome}</td>
                    <td className="py-4 pr-8">
                      <span
                        className={
                          clinica.ativa
                            ? 'text-[11px] font-medium uppercase tracking-[0.14em] text-sage-dark'
                            : 'text-[11px] font-medium uppercase tracking-[0.14em] text-ink-soft'
                        }
                      >
                        {clinica.ativa ? 'Ativa' : 'Inativa'}
                      </span>
                    </td>
                    <td className="py-4 pr-8 font-mono text-[13px] text-ink-soft">
                      {clinica.totalResponsaveis}
                    </td>
                    <td className="py-4 pr-8 text-ink-soft">{formatData(clinica.criadaEm)}</td>
                    <td className="py-4 text-right whitespace-nowrap">
                      <Button variant="ghost" size="sm" onClick={() => abrirDetalhe(clinica)}>
                        Detalhes
                      </Button>
                      <Button variant="ghost" size="sm" className="ml-4" onClick={() => entrarComo(clinica)}>
                        Acessar
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      <Modal open={criando} title="Nova clínica" onClose={() => setCriando(false)}>
        {!resultadoCriar ? (
          <div className="flex flex-col gap-5">
            <TextField
              label="Nome da clínica *"
              value={formCriar.nome}
              onChange={(e) => setFormCriar({ ...formCriar, nome: e.target.value })}
              placeholder="Ex.: Clínica Vida"
            />
            <TextField
              label="E-mail do responsável *"
              type="email"
              value={formCriar.emailResponsavel}
              onChange={(e) => setFormCriar({ ...formCriar, emailResponsavel: e.target.value })}
              placeholder="dono@clinica.com"
            />
            <TextField
              label="Nome do responsável *"
              value={formCriar.nomeResponsavel}
              onChange={(e) => setFormCriar({ ...formCriar, nomeResponsavel: e.target.value })}
              placeholder="Nome de quem administra a clínica"
            />
            {erroCriar && <p className="text-sm text-red-600">{erroCriar}</p>}
            <div className="mt-2 flex justify-end gap-3">
              <Button variant="ghost" onClick={() => setCriando(false)}>
                Cancelar
              </Button>
              <Button onClick={criarClinica} disabled={salvando}>
                {salvando ? 'Criando...' : 'Criar clínica'}
              </Button>
            </div>
          </div>
        ) : resultadoCriar.senha ? (
          <div className="flex flex-col gap-5">
            <SenhaTemporaria email={resultadoCriar.email} senha={resultadoCriar.senha} />
            <div className="flex justify-end">
              <Button onClick={() => setCriando(false)}>Concluir</Button>
            </div>
          </div>
        ) : (
          <div className="flex flex-col gap-5">
            <p className="text-sm text-ink-soft">
              Clínica criada sem integração com o Supabase. O responsável ainda não tem acesso por
              e-mail/senha.
            </p>
            <div className="flex justify-end">
              <Button onClick={() => setCriando(false)}>Concluir</Button>
            </div>
          </div>
        )}
      </Modal>

      <Modal
        open={detalhe !== null}
        title={detalhe?.nome ?? 'Detalhes'}
        index={detalhe ? 1 : undefined}
        onClose={() => setDetalhe(null)}
      >
        {carregandoDetalhe ? (
          <Spinner />
        ) : detalhe ? (
          <div className="flex flex-col gap-6">
            {erroDetalhe && <ErrorBanner message={erroDetalhe} />}

            <div className="grid grid-cols-2 gap-4 border-b border-hairline pb-6 sm:grid-cols-4">
              <div>
                <p className={microLabel}>Status</p>
                <p className={`mt-1 text-sm font-medium ${detalhe.ativa ? 'text-sage-dark' : 'text-ink-soft'}`}>
                  {detalhe.ativa ? 'Ativa' : 'Inativa'}
                </p>
                <Button variant="ghost" size="sm" className="mt-1 !px-0" onClick={alternarAtiva}>
                  {detalhe.ativa ? 'Desativar' : 'Ativar'}
                </Button>
              </div>
              <div>
                <p className={microLabel}>Clientes</p>
                <p className="mt-1 font-mono text-lg text-ink">{detalhe.totalClientes}</p>
              </div>
              <div>
                <p className={microLabel}>Atendimentos</p>
                <p className="mt-1 font-mono text-lg text-ink">{detalhe.totalAtendimentos}</p>
              </div>
              <div>
                <p className={microLabel}>Receita</p>
                <p className="mt-1 font-mono text-lg text-accent-strong">
                  {formatMoeda(detalhe.receita)}
                </p>
              </div>
            </div>

            <div className="flex flex-col gap-4">
              <div className="flex items-center justify-between">
                <p className={microLabel}>Responsáveis</p>
                <span className="font-mono text-[11px] tracking-[0.14em] text-ink-soft">
                  {detalhe.responsaveis.length}
                </span>
              </div>
              <div className="flex flex-col divide-y divide-hairline border-y border-hairline">
                {detalhe.responsaveis.map((responsavel) => (
                  <div key={responsavel.id} className="flex flex-col gap-2 py-3 md:flex-row md:items-center">
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-ink">{responsavel.nome}</p>
                      <p className="truncate text-xs text-ink-soft">{responsavel.email}</p>
                    </div>
                    <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft">
                      {responsavel.papel}
                    </span>
                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={resetandoId === responsavel.id}
                      onClick={() => resetarSenha(responsavel.id)}
                    >
                      {resetandoId === responsavel.id ? 'Resetando...' : 'Resetar senha'}
                    </Button>
                  </div>
                ))}
              </div>
              {senhaReset && <SenhaTemporaria email={senhaReset.email} senha={senhaReset.senha} />}
            </div>

            <div className="flex flex-col gap-4 border-t border-hairline pt-6">
              <p className={microLabel}>Adicionar responsável</p>
              <div className="flex flex-col gap-3 sm:flex-row">
                <TextField
                  label="Nome *"
                  value={formResponsavel.nome}
                  onChange={(e) => setFormResponsavel({ ...formResponsavel, nome: e.target.value })}
                />
                <TextField
                  label="E-mail *"
                  type="email"
                  value={formResponsavel.email}
                  onChange={(e) => setFormResponsavel({ ...formResponsavel, email: e.target.value })}
                />
              </div>
              {resultadoResponsavel && !formResponsavel.nome && (
                <SenhaTemporaria email={resultadoResponsavel.email} senha={resultadoResponsavel.senha} />
              )}
              {erroResponsavel && <p className="text-sm text-red-600">{erroResponsavel}</p>}
              <div className="flex justify-end">
                <Button onClick={adicionarResponsavel} disabled={salvandoResponsavel}>
                  {salvandoResponsavel ? 'Adicionando...' : 'Adicionar responsável'}
                </Button>
              </div>
            </div>
          </div>
        ) : null}
      </Modal>
    </>
  )
}