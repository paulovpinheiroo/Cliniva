package com.cliniva.resumo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cliniva.exception.LimiteDeGeracoesExcedidoException;
import com.cliniva.resumo.model.ResumoDiaCache;
import com.cliniva.resumo.model.ResumoDiaCacheId;
import com.cliniva.resumo.provider.ResumoProvider;
import com.cliniva.resumo.repository.ResumoDiaCacheRepository;
import com.cliniva.tenancy.Clinica;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResumoService {

    static final int LIMITE_GERACOES_DIA = 5;
    static final ZoneId ZONA_BRASIL = ZoneId.of("America/Sao_Paulo");

    private final ResumoDiaCacheRepository cacheRepository;
    private final ResumoAgregador agregador;
    private final ObjectProvider<ResumoProvider> providers;

    @Transactional
    public ResumoDoDiaDTO obterResumo(Clinica clinica) {
        return gerar(clinica, false);
    }

    @Transactional
    public ResumoDoDiaDTO regenerar(Clinica clinica) {
        return gerar(clinica, true);
    }

    private ResumoDoDiaDTO gerar(Clinica clinica, boolean forcarNovaGeracao) {
        LocalDate hoje = LocalDate.now(ZONA_BRASIL);
        ResumoContexto contexto = agregador.agregar(clinica, hoje, ZONA_BRASIL);

        ResumoDiaCache cache = cacheRepository
                .findById(new ResumoDiaCacheId(clinica.getId(), hoje))
                .orElse(null);

        if (cache != null && !forcarNovaGeracao) {
            return toDto(contexto, cache.getTexto(), cache.getOrigem(), cache.getGeradoEm(),
                    cache.getTentativas());
        }

        if (cache == null) {
            cache = novoCache(clinica, hoje);
        }

        if (cache.getTentativas() >= LIMITE_GERACOES_DIA) {
            throw new LimiteDeGeracoesExcedidoException(
                    "Limite diário de resumos atingido (" + LIMITE_GERACOES_DIA + "). Volte amanhã.");
        }

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
                texto = ResumoTemplate.gerar(contexto);
                origem = ResumoDiaCache.Origem.TEMPLATE;
            }
        }

        cache.setTexto(texto);
        cache.setOrigem(origem);
        cache.setGeradoEm(Instant.now());
        cache.setTentativas(cache.getTentativas() + 1);
        cacheRepository.save(cache);

        return toDto(contexto, texto, origem, cache.getGeradoEm(), cache.getTentativas());
    }

    private ResumoDiaCache novoCache(Clinica clinica, LocalDate hoje) {
        ResumoDiaCache cache = new ResumoDiaCache();
        cache.setId(new ResumoDiaCacheId(clinica.getId(), hoje));
        cache.setClinica(clinica);
        return cache;
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