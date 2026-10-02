package com.cliniva.resumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.cliniva.exception.LimiteDeGeracoesExcedidoException;
import com.cliniva.resumo.model.ResumoDiaCache;
import com.cliniva.resumo.model.ResumoDiaCacheId;
import com.cliniva.resumo.provider.ResumoProvider;
import com.cliniva.resumo.repository.ResumoDiaCacheRepository;
import com.cliniva.tenancy.Clinica;
import com.cliniva.tenancy.ClinicaRepository;

@ExtendWith(MockitoExtension.class)
class ResumoServiceTest {

    @Spy
    private final Clock clock = Clock.system(ZoneId.of("America/Sao_Paulo"));

    private static final Clinica CLINICA_A = clinica("Clínica A");
    private static final Clinica CLINICA_B = clinica("Clínica B");

    @Mock
    private ResumoDiaCacheRepository cacheRepository;
    @Mock
    private ResumoAgregador agregador;
    @Mock
    private ObjectProvider<ResumoProvider> providers;
    @Mock
    private ResumoProvider provedor;
    @Mock
    private ClinicaRepository clinicaRepository;
    @Mock
    private TransactionTemplate transacao;

    @InjectMocks
    private ResumoService resumoService;

    /** Executa o callback inline: o template é mockado, não há tx real aqui. */
    @BeforeEach
    void executaTransacaoInline() {
        when(transacao.execute(any())).thenAnswer(invocacao -> {
            TransactionCallback<?> callback = invocacao.getArgument(0);
            return callback.doInTransaction((TransactionStatus) null);
        });
        lenient().when(clinicaRepository.findByIdParaUpdate(any()))
                .thenAnswer(invocacao -> Optional.of(clinicaPorId(invocacao.getArgument(0))));
    }

    private static Clinica clinicaPorId(UUID id) {
        return CLINICA_A.getId().equals(id) ? CLINICA_A : CLINICA_B;
    }

    @Test
    void deveRetornarResumoJaGeradoSemConsumirProvedor() {
        ResumoDiaCache cache = cacheCom(2, "Texto de ontem", ResumoDiaCache.Origem.IA);
        when(cacheRepository.findById(any(ResumoDiaCacheId.class))).thenReturn(Optional.of(cache));
        when(agregador.agregar(any(), any(), any())).thenReturn(contexto());

        ResumoDoDiaDTO resposta = resumoService.obterResumo(CLINICA_A);

        assertThat(resposta.texto()).isEqualTo("Texto de ontem");
        assertThat(resposta.origem()).isEqualTo(ResumoDiaCache.Origem.IA);
        assertThat(resposta.tentativasRestantes()).isEqualTo(3);
        verify(providers, never()).getIfAvailable();
        verify(cacheRepository, never()).save(any());
    }

    @Test
    void deveGerarComProvedorIaQuandoDisponivel() {
        when(cacheRepository.findById(any(ResumoDiaCacheId.class))).thenReturn(Optional.empty());
        when(agregador.agregar(any(), any(), any())).thenReturn(contexto());
        when(providers.getIfAvailable()).thenReturn(provedor);
        when(provedor.gerar(any())).thenReturn("Resumo escrito pela IA");

        ResumoDoDiaDTO resposta = resumoService.obterResumo(CLINICA_A);

        assertThat(resposta.texto()).isEqualTo("Resumo escrito pela IA");
        assertThat(resposta.origem()).isEqualTo(ResumoDiaCache.Origem.IA);
        assertThat(resposta.tentativasRestantes()).isEqualTo(4);

        ArgumentCaptor<ResumoDiaCache> captor = ArgumentCaptor.forClass(ResumoDiaCache.class);
        verify(cacheRepository).save(captor.capture());
        assertThat(captor.getValue().getTentativas()).isEqualTo(1);
        assertThat(captor.getValue().getId().getClinicaId()).isEqualTo(CLINICA_A.getId());
    }

    @Test
    void deveUsarTemplateQuandoProvedorFalhar() {
        when(cacheRepository.findById(any(ResumoDiaCacheId.class))).thenReturn(Optional.empty());
        when(agregador.agregar(any(), any(), any())).thenReturn(contexto());
        when(providers.getIfAvailable()).thenReturn(provedor);
        when(provedor.gerar(any())).thenThrow(new RuntimeException("provedor indisponível"));

        ResumoDoDiaDTO resposta = resumoService.obterResumo(CLINICA_A);

        assertThat(resposta.origem()).isEqualTo(ResumoDiaCache.Origem.TEMPLATE);
        assertThat(resposta.texto()).startsWith("Hoje:");
    }

    @Test
    void deveUsarTemplateQuandoNaoHaProvedorConfigurado() {
        when(cacheRepository.findById(any(ResumoDiaCacheId.class))).thenReturn(Optional.empty());
        when(agregador.agregar(any(), any(), any())).thenReturn(contexto());
        when(providers.getIfAvailable()).thenReturn(null);

        ResumoDoDiaDTO resposta = resumoService.obterResumo(CLINICA_A);

        assertThat(resposta.origem()).isEqualTo(ResumoDiaCache.Origem.TEMPLATE);
        assertThat(resposta.tentativasRestantes()).isEqualTo(4);
    }

    @Test
    void deveIncrementarTentativasNaRegeneracao() {
        ResumoDiaCache cache = cacheCom(2, "Resumo anterior", ResumoDiaCache.Origem.TEMPLATE);
        when(cacheRepository.findById(any(ResumoDiaCacheId.class))).thenReturn(Optional.of(cache));
        when(agregador.agregar(any(), any(), any())).thenReturn(contexto());
        when(providers.getIfAvailable()).thenReturn(null);

        ResumoDoDiaDTO resposta = resumoService.regenerar(CLINICA_A);

        assertThat(resposta.tentativasRestantes()).isEqualTo(2);

        ArgumentCaptor<ResumoDiaCache> captor = ArgumentCaptor.forClass(ResumoDiaCache.class);
        verify(cacheRepository).save(captor.capture());
        assertThat(captor.getValue().getTentativas()).isEqualTo(3);
    }

    @Test
    void deveLancar429AoExcederLimiteDiario() {
        ResumoDiaCache cache = cacheCom(5, "Resumo final", ResumoDiaCache.Origem.TEMPLATE);
        when(cacheRepository.findById(any(ResumoDiaCacheId.class))).thenReturn(Optional.of(cache));
        when(agregador.agregar(any(), any(), any())).thenReturn(contexto());

        assertThatThrownBy(() -> resumoService.regenerar(CLINICA_A))
                .isInstanceOf(LimiteDeGeracoesExcedidoException.class);
        verify(providers, never()).getIfAvailable();
    }

    @Test
    void deveIsolarResumoPorClinica() {
        when(cacheRepository.findById(any(ResumoDiaCacheId.class))).thenReturn(Optional.empty());
        when(agregador.agregar(any(), any(), any())).thenReturn(contexto());
        when(providers.getIfAvailable()).thenReturn(null);

        resumoService.obterResumo(CLINICA_B);

        ArgumentCaptor<ResumoDiaCache> captor = ArgumentCaptor.forClass(ResumoDiaCache.class);
        verify(cacheRepository).save(captor.capture());
        assertThat(captor.getValue().getId().getClinicaId()).isEqualTo(CLINICA_B.getId());
    }

    @Test
    void deveReservarTentativaAntesDeChamarOProvedor() {
        when(cacheRepository.findById(any(ResumoDiaCacheId.class))).thenReturn(Optional.empty());
        when(agregador.agregar(any(), any(), any())).thenReturn(contexto());
        when(providers.getIfAvailable()).thenReturn(provedor);
        when(provedor.gerar(any())).thenReturn("Resumo da IA");

        resumoService.regenerar(CLINICA_A);

        // A ordem importa: reservar (lock) -> chamar a IA -> gravar. Se a IA
        // fosse chamada antes da reserva, dois requests simultâneos poderiam
        // passar juntos pelo limite de custo.
        InOrder ordem = inOrder(clinicaRepository, provedor, cacheRepository);
        ordem.verify(clinicaRepository).findByIdParaUpdate(CLINICA_A.getId());
        ordem.verify(provedor).gerar(any());
        ordem.verify(cacheRepository).save(any(ResumoDiaCache.class));
    }

    private static Clinica clinica(String nome) {
        Clinica clinica = new Clinica();
        clinica.setId(UUID.randomUUID());
        clinica.setNome(nome);
        return clinica;
    }

    private static ResumoContexto contexto() {
        return new ResumoContexto(
                "Clínica A",
                3,
                new BigDecimal("150.00"),
                new BigDecimal("90.00"),
                2,
                1,
                1,
                List.of("Sérum Vitamina C"));
    }

    private static ResumoDiaCache cacheCom(int tentativas, String texto, ResumoDiaCache.Origem origem) {
        ResumoDiaCache cache = new ResumoDiaCache();
        cache.setId(new ResumoDiaCacheId(CLINICA_A.getId(), LocalDate.now()));
        cache.setTexto(texto);
        cache.setOrigem(origem);
        cache.setTentativas(tentativas);
        return cache;
    }
}