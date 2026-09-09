package com.cliniva.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cliniva.atendimento.repository.AtendimentoServicoRepository;
import com.cliniva.exception.RecursoDuplicadoException;
import com.cliniva.exception.RecursoEmUsoException;
import com.cliniva.exception.RecursoNaoEncontradoException;
import com.cliniva.servico.dtos.CreateServicoRequestDTO;
import com.cliniva.servico.dtos.UpdateServicoRequestDTO;
import com.cliniva.tenancy.Clinica;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private AtendimentoServicoRepository atendimentoServicoRepository;

    @InjectMocks
    private ServicoService servicoService;

    private static final Clinica CLINICA = clinica("Clínica A");

    private static Clinica clinica(String nome) {
        Clinica clinica = new Clinica();
        org.springframework.test.util.ReflectionTestUtils.setField(clinica, "id", UUID.randomUUID());
        org.springframework.test.util.ReflectionTestUtils.setField(clinica, "nome", nome);
        return clinica;
    }

    private Servico servico(UUID id, String nome, String valor) {
        Servico servico = new Servico();
        servico.setClinica(CLINICA);
        servico.setNome(nome);
        servico.setValor(new BigDecimal(valor));
        if (id != null) {
            org.springframework.test.util.ReflectionTestUtils.setField(servico, "id", id);
        }
        return servico;
    }

    @Test
    void deveCriarServicoComDadosValidos() {
        when(servicoRepository.existsByNomeAndClinica("Limpeza de Pele", CLINICA)).thenReturn(false);
        when(servicoRepository.save(any(Servico.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = servicoService.createServico(CLINICA, new CreateServicoRequestDTO(
                "Limpeza de Pele", "limpeza profunda", new BigDecimal("120.00")));

        assertThat(resposta.nome()).isEqualTo("Limpeza de Pele");
        assertThat(resposta.valor()).isEqualByComparingTo("120.00");
    }

    @Test
    void naoDeveCriarServicoComNomeDuplicado() {
        when(servicoRepository.existsByNomeAndClinica("Limpeza de Pele", CLINICA)).thenReturn(true);

        assertThatThrownBy(() -> servicoService.createServico(CLINICA,
                new CreateServicoRequestDTO("Limpeza de Pele", null, new BigDecimal("120.00"))))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(servicoRepository, never()).save(any());
    }

    @Test
    void deveListarServicos() {
        when(servicoRepository.findByClinica(CLINICA))
                .thenReturn(List.of(servico(null, "Massagem", "90.00")));

        var lista = servicoService.listarServicos(CLINICA);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).nome()).isEqualTo("Massagem");
    }

    @Test
    void naoDeveBuscarServicoInexistente() {
        UUID id = UUID.randomUUID();
        when(servicoRepository.findByIdAndClinica(id, CLINICA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicoService.buscarPorId(CLINICA, id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void naoDeveAtualizarComNomeUsadoPorOutroServico() {
        UUID id = UUID.randomUUID();
        when(servicoRepository.findByIdAndClinica(id, CLINICA))
                .thenReturn(Optional.of(servico(id, "Massagem", "90.00")));
        when(servicoRepository.existsByNomeAndIdNotAndClinica("Drenagem", id, CLINICA)).thenReturn(true);

        assertThatThrownBy(() -> servicoService.atualizarServico(
                CLINICA, id, new UpdateServicoRequestDTO("Drenagem", null, new BigDecimal("100.00"))))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    void deveAtualizarServicoComDadosValidos() {
        UUID id = UUID.randomUUID();
        when(servicoRepository.findByIdAndClinica(id, CLINICA))
                .thenReturn(Optional.of(servico(id, "Massagem", "90.00")));
        when(servicoRepository.existsByNomeAndIdNotAndClinica("Massagem Relaxante", id, CLINICA))
                .thenReturn(false);
        when(servicoRepository.save(any(Servico.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = servicoService.atualizarServico(
                CLINICA, id, new UpdateServicoRequestDTO("Massagem Relaxante", "com óleos",
                        new BigDecimal("110.00")));

        assertThat(resposta.nome()).isEqualTo("Massagem Relaxante");
        assertThat(resposta.valor()).isEqualByComparingTo("110.00");
    }

    @Test
    void naoDeveDeletarServicoUsadoEmAtendimentos() {
        UUID id = UUID.randomUUID();
        when(servicoRepository.existsByIdAndClinica(id, CLINICA)).thenReturn(true);
        when(atendimentoServicoRepository.existsByServico_Id(id)).thenReturn(true);

        assertThatThrownBy(() -> servicoService.deletarServico(CLINICA, id))
                .isInstanceOf(RecursoEmUsoException.class);

        verify(servicoRepository, never()).deleteById(any());
    }

    @Test
    void deveDeletarServicoSemVinculos() {
        UUID id = UUID.randomUUID();
        when(servicoRepository.existsByIdAndClinica(id, CLINICA)).thenReturn(true);
        when(atendimentoServicoRepository.existsByServico_Id(id)).thenReturn(false);

        servicoService.deletarServico(CLINICA, id);

        verify(servicoRepository).deleteById(id);
    }
}