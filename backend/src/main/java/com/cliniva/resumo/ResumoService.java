package com.cliniva.resumo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.Supplier;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.cliniva.exception.LimiteDeGeracoesExcedidoException;
import com.cliniva.exception.RecursoNaoEncontradoException;
import com.cliniva.resumo.model.ResumoDiaCache;
import com.cliniva.resumo.model.ResumoDiaCacheId;
import com.cliniva.resumo.provider.ResumoProvider;
import com.cliniva.resumo.repository.ResumoDiaCacheRepository;
import com.cliniva.tenancy.Clinica;
import com.cliniva.tenancy.ClinicaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumoService {

    static final int LIMITE_GERACOES_DIA = 5;

    private final ResumoDiaCacheRepository cacheRepository;
    private final ResumoAgregador agregador;
    private final ObjectProvider<ResumoProvider> providers;
    private final ClinicaRepository clinicaRepository;
    private final TransactionTemplate transacao;
    private final Clock clock;

    public ResumoDoDiaDTO obterResumo(Clinica clinica) {
        return gerar(clinica, false);
    }

    public ResumoDoDiaDTO regenerar(Clinica clinica) {
        return gerar(clinica, true);
    }

    /**
     * Fluxo em três etapas, deliberadamente <b>sem</b> uma transação envolvendo
     * tudo: a chamada ao provider de IA é HTTP e pode levar segundos, e segurar
     * uma conexão do pool durante a inferência esgota o pool com poucos usuários
     * simultâneos.
     */
    private ResumoDoDiaDTO gerar(Clinica clinica, boolean forcarNovaGeracao) {
        // Pelo Clock da aplicação: a zona vem de cliniva.zona, e o mesmo dia
        // precisa valer para a janela do resumo e para a do agregado.
        LocalDate hoje = LocalDate.now(clock);
        ZoneId zona = clock.getZone();
        ResumoContexto contexto = emTransacao(() -> agregador.agregar(clinica, hoje, zona));

        ResumoDiaCache cache = emTransacao(
                () -> cacheRepository.findById(new ResumoDiaCacheId(clinica.getId(), hoje)).orElse(null));

        if (cache != null && !forcarNovaGeracao) {
            return toDto(contexto, cache.getTexto(), cache.getOrigem(), cache.getGeradoEm(),
                    cache.getTentativas());
        }

        // Reserva a tentativa ANTES de chamar o provider. O limite existe para
        // conter custo de API, e ler-o-depois-escrever deixava dois requests
        // simultâneos passarem juntos. A reserva é serializada pelo lock da
        // clínica (mesmo padrão da agenda) e dura só o suficiente para um read.
        int tentativas = reservarTentativa(clinica, hoje);

        String texto;
        ResumoDiaCache.Origem origem;
        ResumoProvider provider = providers.getIfAvailable();
        if (provider == null) {
            texto = ResumoTemplate.gerar(contexto);
            origem = ResumoDiaCache.Origem.TEMPLATE;
        } else {
            try {
                texto = provider.gerar(contexto);
                origem = ResumoDiaCache.Origem.IA;
            } catch (RuntimeException excecao) {
                // Sem log, uma falha de cota/429 do provider é invisível e o
                // produto só mostra "template" sem explicar o porquê.
                log.warn("Falha ao gerar resumo via IA; usando template. "
                        + "Verifique cota da chave e o modelo configurado.", excecao);
                texto = ResumoTemplate.gerar(contexto);
                origem = ResumoDiaCache.Origem.TEMPLATE;
            }
        }

        Instant geradoEm = Instant.now();
        ResumoDiaCache aGravar = newCache(clinica, hoje);
        aGravar.setTexto(texto);
        aGravar.setOrigem(origem);
        aGravar.setGeradoEm(geradoEm);
        aGravar.setTentativas(tentativas);
        emTransacao(() -> cacheRepository.save(aGravar));

        return toDto(contexto, texto, origem, geradoEm, tentativas);
    }

    /**
     * Consome uma das {@value #LIMITE_GERACOES_DIA} tentativas do dia de forma
     * atômica. Trava a clínica para serializar com qualquer outra geração
     * concorrente da mesma clínica.
     */
    private int reservarTentativa(Clinica clinica, LocalDate hoje) {
        return emTransacao(() -> {
            clinicaRepository.findByIdParaUpdate(clinica.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Clínica não encontrada"));

            int usadas = cacheRepository.findById(new ResumoDiaCacheId(clinica.getId(), hoje))
                    .map(ResumoDiaCache::getTentativas)
                    .orElse(0);
            if (usadas >= LIMITE_GERACOES_DIA) {
                throw new LimiteDeGeracoesExcedidoException(
                        "Limite diário de resumos atingido (" + LIMITE_GERACOES_DIA + "). Volte amanhã.");
            }
            return usadas + 1;
        });
    }

    private ResumoDiaCache newCache(Clinica clinica, LocalDate hoje) {
        ResumoDiaCache cache = new ResumoDiaCache();
        cache.setId(new ResumoDiaCacheId(clinica.getId(), hoje));
        cache.setClinica(clinica);
        return cache;
    }

    private <T> T emTransacao(Supplier<T> acao) {
        return transacao.execute(estado -> acao.get());
    }

    private ResumoDoDiaDTO toDto(ResumoContexto contexto, String texto, ResumoDiaCache.Origem origem,
            Instant geradoEm, int tentativas) {
        return new ResumoDoDiaDTO(
                contexto.atendimentosHoje(),
                contexto.receitaPrevista(),
                contexto.receitaRealizada(),
                contexto.novosMes(),
                contexto.recorrentesMes(),
                contexto.aniversariantesHoje(),
                contexto.itensAbaixoMinimo(),
                texto,
                origem,
                geradoEm,
                Math.max(0, LIMITE_GERACOES_DIA - tentativas));
    }
}