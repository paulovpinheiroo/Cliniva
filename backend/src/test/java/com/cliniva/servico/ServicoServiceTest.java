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

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private AtendimentoServicoRepository atendimentoServicoRepository;

    @InjectMocks
    private ServicoService servicoService;

    private Servico servico(UUID id, String nome, String valor) {
        Servico servico = new Servico();
        servico.setNome(nome);
        servico.setValor(new BigDecimal(valor));
        if (id != null) {
            org.springframework.test.util.ReflectionTestUtils.setField(servico, "id", id);
        }
        return servico;
    }

    @Test
    void deveCriarServicoComDadosValidos() {
        when(servicoRepository.existsByNome("Limpeza de Pele")).thenReturn(false);
        when(servicoRepository.save(any(Servico.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = servicoService.createServico(new CreateServicoRequestDTO(
                "Limpeza de Pele", "limpeza profunda", new BigDecimal("120.00")));

        assertThat(resposta.nome()).isEqualTo("Limpeza de Pele");
        assertThat(resposta.valor()).isEqualByComparingTo("120.00");
    }

    @Test
    void naoDeveCriarServicoComNomeDuplicado() {
        when(servicoRepository.existsByNome("Limpeza de Pele")).thenReturn(true);

        assertThatThrownBy(() -> servicoService.createServico(
                new CreateServicoRequestDTO("Limpeza de Pele", null, new BigDecimal("120.00"))))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(servicoRepository, never()).save(any());
    }

    @Test
    void deveListarServicos() {
        when(servicoRepository.findAll())
                .thenReturn(List.of(servico(null, "Massagem", "90.00")));

        var lista = servicoService.listarServicos();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).nome()).isEqualTo("Massagem");
    }

    @Test
    void naoDeveBuscarServicoInexistente() {
        UUID id = UUID.randomUUID();
        when(servicoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicoService.buscarPorId(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void naoDeveAtualizarComNomeUsadoPorOutroServico() {
        UUID id = UUID.randomUUID();
        when(servicoRepository.findById(id))
                .thenReturn(Optional.of(servico(id, "Massagem", "90.00")));
        when(servicoRepository.existsByNomeAndIdNot("Drenagem", id)).thenReturn(true);

        assertThatThrownBy(() -> servicoService.atualizarServico(
                id, new UpdateServicoRequestDTO("Drenagem", null, new BigDecimal("100.00"))))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    void deveAtualizarServicoComDadosValidos() {
        UUID id = UUID.randomUUID();
        when(servicoRepository.findById(id))
                .thenReturn(Optional.of(servico(id, "Massagem", "90.00")));
        when(servicoRepository.existsByNomeAndIdNot("Massagem Relaxante", id)).thenReturn(false);
        when(servicoRepository.save(any(Servico.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = servicoService.atualizarServico(
                id, new UpdateServicoRequestDTO("Massagem Relaxante", "com óleos", new BigDecimal("110.00")));

        assertThat(resposta.nome()).isEqualTo("Massagem Relaxante");
        assertThat(resposta.valor()).isEqualByComparingTo("110.00");
    }

    @Test
    void naoDeveDeletarServicoUsadoEmAtendimentos() {
        UUID id = UUID.randomUUID();
        when(servicoRepository.existsById(id)).thenReturn(true);
        when(atendimentoServicoRepository.existsByServico_Id(id)).thenReturn(true);

        assertThatThrownBy(() -> servicoService.deletarServico(id))
                .isInstanceOf(RecursoEmUsoException.class);

        verify(servicoRepository, never()).deleteById(any());
    }

    @Test
    void deveDeletarServicoSemVinculos() {
        UUID id = UUID.randomUUID();
        when(servicoRepository.existsById(id)).thenReturn(true);
        when(atendimentoServicoRepository.existsByServico_Id(id)).thenReturn(false);

        servicoService.deletarServico(id);

        verify(servicoRepository).deleteById(id);
    }
}
