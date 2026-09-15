interface ProblemaProps {
  n: string
  titulo: string
  antes: string
  depois: string
}

const problemas: ProblemaProps[] = [
  {
    n: '01',
    titulo: 'Agenda no caderno e no WhatsApp',
    antes: 'Marca na hora, esquece na hora. Sem histórico de quem veio, quando voltou e quanto pagou.',
    depois:
      'Atendimentos com data, status, valor e histórico por cliente — agenda e ficha do cliente lado a lado.',
  },
  {
    n: '02',
    titulo: 'Cliente que você conhecia de cor',
    antes: 'Aniversário, preferência, último procedimento — tudo na sua cabeça, até a agenda lotar.',
    depois:
      'CRM com perfil, histórico, notas e follow-up pelo WhatsApp. Aniversariantes do mês avisados sem esforço.',
  },
  {
    n: '03',
    titulo: 'Caixa na planilha',
    antes: 'Quanto entrou no mês? Soma célula por célula — e quebra quando alguém remarca em cima da hora.',
    depois: 'Valor por atendimento e métricas do mês: novos, recorrentes e receita, sem montar fórmula.',
  },
  {
    n: '04',
    titulo: 'Estoque que acaba na hora errada',
    antes: 'Produto em falta no dia do cliente, reposição de memória, desperdício sem controle.',
    depois:
      'Cada item acompanhado, com alerta de estoque baixo no dashboard — você repõe antes de faltar.',
  },
]

export function ProblemsSection() {
  return (
    <section className="border-b border-hairline bg-paper">
      <div className="mx-auto w-full max-w-6xl px-4 py-16 md:px-6 lg:px-10 lg:py-24">
        <p className="font-mono text-[11px] font-medium uppercase tracking-[0.2em] text-accent-strong">
          O problema · A solução
        </p>
        <h2 className="mt-4 max-w-2xl font-display text-3xl font-medium leading-[1.1] text-ink sm:text-4xl lg:text-5xl">
          Do caderno e da planilha para um painel que mostra a casa inteira.
        </h2>
        <div className="mt-12 grid grid-cols-1 gap-px border border-hairline bg-hairline md:grid-cols-2">
          {problemas.map((problema) => (
            <div key={problema.n} className="bg-paper p-6 lg:p-8">
              <div className="flex items-baseline justify-between gap-4">
                <h3 className="font-display text-2xl font-medium leading-tight text-ink">
                  {problema.titulo}
                </h3>
                <span className="shrink-0 font-mono text-[11px] uppercase tracking-[0.2em] text-ink-soft">
                  {problema.n}
                </span>
              </div>
              <div className="mt-5 border-l border-hairline pl-4">
                <p className="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft">
                  Antes
                </p>
                <p className="mt-1 text-sm leading-relaxed text-ink-soft">{problema.antes}</p>
              </div>
              <div className="mt-4 border-l border-hairline pl-4">
                <p className="font-mono text-[10px] uppercase tracking-[0.18em] text-accent-strong">
                  Na Clíniva
                </p>
                <p className="mt-1 text-sm leading-relaxed text-ink">{problema.depois}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}