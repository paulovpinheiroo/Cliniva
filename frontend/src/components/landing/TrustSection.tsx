interface ItemProps {
  label: string
  children: string
}

function TrustItem({ label, children }: ItemProps) {
  return (
    <li className="py-5">
      <p className="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-soft">{label}</p>
      <p className="mt-2 text-sm leading-relaxed text-ink">{children}</p>
    </li>
  )
}

export function TrustSection() {
  return (
    <section className="border-b border-hairline bg-paper">
      <div className="mx-auto grid w-full max-w-6xl grid-cols-1 gap-12 px-4 py-16 md:px-6 lg:grid-cols-12 lg:gap-16 lg:px-10 lg:py-24">
        <div className="lg:col-span-7">
          <p className="font-mono text-[11px] font-medium uppercase tracking-[0.2em] text-accent-strong">
            Confiança
          </p>
          <h2 className="mt-4 max-w-xl font-display text-3xl font-medium leading-[1.1] text-ink sm:text-4xl">
            Construído por quem entende a rotina da clínica pequena.
          </h2>
          <p className="mt-5 max-w-xl text-sm leading-relaxed text-ink-soft">
            A Clíniva é um produto novo, feito em iterações curtas com donos e donas
            de clínica — não uma plataforma genérica. O que está no ar foi construído
            ouvindo problemas reais: agenda esquecida, cliente sem follow-up, estoque
            sem aviso. Sua clínica ajuda a definir o que vem a seguir.
          </p>
        </div>
        <ul className="divide-y divide-hairline border-y border-hairline lg:col-span-5">
          <TrustItem label="1 · Privacidade">
            Isolamento multi-tenant: cada clínica enxerga somente os próprios dados.
          </TrustItem>
          <TrustItem label="2 · Transparência">
            O que você lê aqui é o que está no ar hoje — sem prometer o que ainda não existe.
          </TrustItem>
          <TrustItem label="3 · Em evolução">
            Versões curtas, guiadas por quem usa. Sua opinião vira a próxima versão.
          </TrustItem>
        </ul>
      </div>
    </section>
  )
}