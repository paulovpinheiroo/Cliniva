import { useState } from 'react'
import { RefreshCw } from 'lucide-react'
import { ApiError } from '@/api/http'
import { resumoApi } from '@/api/resumoApi'
import type { ResumoDoDia } from '@/api/resumoApi'
import { Button } from '@/components/ui/Button'
import { useApi } from '@/hooks/useApi'
import { formatDataLonga, formatMoeda } from '@/utils/format'

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="px-6 py-5">
      <p className="text-[10px] font-medium uppercase tracking-[0.18em] text-ink-soft">{label}</p>
      <p className="mt-1.5 font-mono text-base text-ink">{value}</p>
    </div>
  )
}

export function ResumoDoDiaCard() {
  const { data, loading, error } = useApi(() => resumoApi.obter())
  const [regenerado, setRegenerado] = useState<ResumoDoDia | null>(null)
  const [regenerando, setRegenerando] = useState(false)
  const [erroAcao, setErroAcao] = useState<string | null>(null)

  const resumo = regenerado ?? data
  const limiteAtingido = (resumo?.tentativasRestantes ?? 1) <= 0

  const regenerar = async () => {
    if (!resumo) return
    setRegenerando(true)
    setErroAcao(null)
    try {
      setRegenerado(await resumoApi.regenerar())
    } catch (err) {
      if (err instanceof ApiError && err.status === 429 && resumo) {
        setRegenerado({ ...resumo, tentativasRestantes: 0 })
      } else {
        setErroAcao(err instanceof Error ? err.message : 'Não foi possível regenerar.')
      }
    } finally {
      setRegenerando(false)
    }
  }

  if (loading && !resumo) {
    return (
      <div className="border-y border-hairline px-6 py-8">
        <p className="font-mono text-[11px] uppercase tracking-[0.18em] text-ink-soft">
          Gerando o resumo do dia…
        </p>
      </div>
    )
  }

  if (error && !resumo) {
    return (
      <div className="border-y border-hairline px-6 py-8">
        <p className="text-sm text-ink-soft">Não foi possível carregar o resumo do dia.</p>
      </div>
    )
  }

  if (!resumo) return null

  return (
    <section className="border-y border-hairline">
      <div className="flex flex-wrap items-baseline justify-between gap-3 px-6 pt-6">
        <div>
          <p className="font-mono text-[11px] uppercase tracking-[0.18em] text-ink-soft">
            Resumo do dia
          </p>
          <h2 className="mt-1 font-display text-2xl font-medium text-ink">
            {formatDataLonga(new Date().toISOString())}
          </h2>
        </div>
        <div className="flex items-center gap-4">
          <span className="font-mono text-[10px] uppercase tracking-[0.16em] text-ink-soft">
            {resumo.origem === 'IA' ? '· gerado por ia' : '· resumo padrão'}
          </span>
          <Button
            variant="secondary"
            size="sm"
            onClick={regenerar}
            disabled={regenerando || limiteAtingido}
          >
            <RefreshCw size={12} className={regenerando ? 'animate-spin' : ''} />
            Regenerar
          </Button>
        </div>
      </div>

      <p className="px-6 py-5 text-sm leading-relaxed text-ink">{resumo.texto}</p>

      <div className="grid grid-cols-1 divide-y divide-hairline border-t border-hairline md:grid-cols-5 md:divide-x md:divide-y-0">
        <Metric label="Atendimentos" value={String(resumo.atendimentosHoje)} />
        <Metric label="Receita prevista" value={formatMoeda(resumo.receitaPrevista)} />
        <Metric label="Receita realizada" value={formatMoeda(resumo.receitaRealizada)} />
        <Metric label="Novas / voltaram" value={`${resumo.novosMes} / ${resumo.recorrentesMes}`} />
        <Metric label="Aniversários" value={String(resumo.aniversariantesHoje)} />
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-hairline px-6 py-3">
        <p className="font-mono text-[10px] uppercase tracking-[0.16em] text-ink-soft">
          {limiteAtingido
            ? 'Limite de 5 gerações por dia atingido'
            : `${resumo.tentativasRestantes} ${resumo.tentativasRestantes === 1 ? 'geração restante' : 'gerações restantes'} hoje`}
        </p>
        {erroAcao && <p className="text-xs text-ink-soft">{erroAcao}</p>}
      </div>
    </section>
  )
}