package com.cliniva.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cliniva.atendimento.AtendimentoService;
import com.cliniva.atendimento.dtos.AtendimentoResponseDTO;
import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.atendimento.model.Atendimento;
import com.cliniva.atendimento.repository.AtendimentoRepository;
import com.cliniva.cliente.dtos.CreateClienteNotaRequestDTO;
import com.cliniva.cliente.dtos.CreateClienteRequestDTO;
import com.cliniva.cliente.dtos.UpdateClienteRequestDTO;
import com.cliniva.cliente.enums.CanalPreferido;
import com.cliniva.cliente.enums.ClienteStatus;
import com.cliniva.cliente.enums.OrigemCliente;
import com.cliniva.exception.RecursoDuplicadoException;
import com.cliniva.exception.RecursoEmUsoException;
import com.cliniva.exception.RecursoNaoEncontradoException;
import com.cliniva.tenancy.Clinica;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteNotaRepository clienteNotaRepository;

    @Mock
    private AtendimentoRepository atendimentoRepository;

    @Mock
    private AtendimentoService atendimentoService;

    @InjectMocks
    private ClienteService clienteService;

    private static final ClienteStatus STATUS_DEFAULT = ClienteStatus.PROSPECT;

    private static final Clinica CLINICA = clinica("Clínica A");

    private static final Clinica CLINICA_B = clinica("Clínica B");

    private static Clinica clinica(String nome) {
        Clinica clinica = new Clinica();
        org.springframework.test.util.ReflectionTestUtils.setField(clinica, "id", UUID.randomUUID());
        org.springframework.test.util.ReflectionTestUtils.setField(clinica, "nome", nome);
        return clinica;
    }

    private Cliente cliente(UUID id, String nome, String email, String telefone) {
        Cliente cliente = new Cliente();
        cliente.setClinica(CLINICA);
        cliente.setNome(nome);
        cliente.setEmail(email);
        cliente.setTelefone(telefone);
        if (id != null) {
            org.springframework.test.util.ReflectionTestUtils.setField(cliente, "id", id);
        }
        return cliente;
    }

    private CreateClienteRequestDTO requestCriacaoBasico() {
        return new CreateClienteRequestDTO("Maria", "maria@email.com", "11999990000", null, null, null, null, null);
    }

    private CreateClienteRequestDTO requestCriacaoCompleto() {
        return new CreateClienteRequestDTO("Maria", "maria@email.com", "11999990000",
                LocalDate.of(1990, 5, 10), OrigemCliente.INSTAGRAM, CanalPreferido.WHATSAPP,
                "Cabelo curto", "Alergia a amônia");
    }

    private UpdateClienteRequestDTO requestAtualizacaoBasico() {
        return new UpdateClienteRequestDTO("Maria", null, "111", null, null, null, null, null, null);
    }

    private ClienteNota nota(UUID id, UUID clienteId, String texto) {
        Cliente cliente = cliente(clienteId, "Maria", null, "111");
        ClienteNota nota = new ClienteNota();
        nota.setTexto(texto);
        nota.setCliente(cliente);
        nota.setCriadaEm(LocalDateTime.of(2026, 5, 1, 10, 0));
        org.springframework.test.util.ReflectionTestUtils.setField(nota, "id", id);
        return nota;
    }

    private AtendimentoResponseDTO atendimentoDTO(UUID id, String nomeCliente, LocalDateTime data,
            StatusAtendimento status, BigDecimal valor) {
        return new AtendimentoResponseDTO(
                id, UUID.randomUUID(), nomeCliente, data, LocalDate.now(), status, valor, List.of());
    }

    @Test
    void deveCriarClienteComDadosValidos() {
        when(clienteRepository.existsByEmailAndClinica("maria@email.com", CLINICA)).thenReturn(false);
        when(clienteRepository.existsByTelefoneAndClinica("11999990000", CLINICA)).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = clienteService.createCliente(CLINICA, requestCriacaoBasico());

        assertThat(resposta.nome()).isEqualTo("Maria");
        assertThat(resposta.email()).isEqualTo("maria@email.com");
        assertThat(resposta.telefone()).isEqualTo("11999990000");
    }

    @Test
    void deveCriarClienteComPerfilCompletoEStatusProspect() {
        when(clienteRepository.existsByEmailAndClinica("maria@email.com", CLINICA)).thenReturn(false);
        when(clienteRepository.existsByTelefoneAndClinica("11999990000", CLINICA)).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = clienteService.createCliente(CLINICA, requestCriacaoCompleto());

        assertThat(resposta.dataNascimento()).isEqualTo(LocalDate.of(1990, 5, 10));
        assertThat(resposta.origem()).isEqualTo(OrigemCliente.INSTAGRAM);
        assertThat(resposta.canalPreferido()).isEqualTo(CanalPreferido.WHATSAPP);
        assertThat(resposta.preferencias()).isEqualTo("Cabelo curto");
        assertThat(resposta.observacoes()).isEqualTo("Alergia a amônia");
        assertThat(resposta.status()).isEqualTo(ClienteStatus.PROSPECT);
    }

    @Test
    void deveSalvarClienteComStatusProspectPorPadrao() {
        when(clienteRepository.existsByEmailAndClinica("maria@email.com", CLINICA)).thenReturn(false);
        when(clienteRepository.existsByTelefoneAndClinica("11999990000", CLINICA)).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        clienteService.createCliente(CLINICA, requestCriacaoBasico());

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(STATUS_DEFAULT);
        assertThat(captor.getValue().getClinica()).isEqualTo(CLINICA);
    }

    @Test
    void naoDeveCriarClienteComEmailDuplicado() {
        when(clienteRepository.existsByEmailAndClinica("maria@email.com", CLINICA)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.createCliente(CLINICA, requestCriacaoBasico()))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("Email");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarClienteComTelefoneDuplicado() {
        when(clienteRepository.existsByEmailAndClinica("maria@email.com", CLINICA)).thenReturn(false);
        when(clienteRepository.existsByTelefoneAndClinica("11999990000", CLINICA)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.createCliente(CLINICA, requestCriacaoBasico()))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("Telefone");
    }

    @Test
    void devePermitirMesmoEmailEmClinicasDiferentes() {
        when(clienteRepository.existsByEmailAndClinica("maria@email.com", CLINICA)).thenReturn(false);
        when(clienteRepository.existsByEmailAndClinica("maria@email.com", CLINICA_B)).thenReturn(false);
        when(clienteRepository.existsByTelefoneAndClinica("11999990000", CLINICA)).thenReturn(false);
        when(clienteRepository.existsByTelefoneAndClinica("11999990000", CLINICA_B)).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        clienteService.createCliente(CLINICA, requestCriacaoBasico());
        var resposta = clienteService.createCliente(CLINICA_B, requestCriacaoBasico());

        assertThat(resposta.email()).isEqualTo("maria@email.com");
        verify(clienteRepository).existsByEmailAndClinica("maria@email.com", CLINICA);
        verify(clienteRepository).existsByEmailAndClinica("maria@email.com", CLINICA_B);
    }

    @Test
    void deveListarSemFiltroQuandoNadaInformado() {
        when(clienteRepository.findByClinica(CLINICA))
                .thenReturn(List.of(cliente(null, "Maria", null, "111")));

        var lista = clienteService.listarClientes(CLINICA, null, null);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).nome()).isEqualTo("Maria");
        verify(clienteRepository, never()).findByClinicaAndNomeContainingIgnoreCase(any(), any());
        verify(clienteRepository, never()).findByClinicaAndStatus(any(), any());
    }

    @Test
    void deveListarFiltrandoPorNome() {
        when(clienteRepository.findByClinicaAndNomeContainingIgnoreCase(CLINICA, "mar"))
                .thenReturn(List.of(cliente(null, "Maria", null, "111")));

        var lista = clienteService.listarClientes(CLINICA, "mar", null);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).nome()).isEqualTo("Maria");
    }

    @Test
    void deveListarFiltrandoPorStatus() {
        when(clienteRepository.findByClinicaAndStatus(CLINICA, ClienteStatus.ATIVO))
                .thenReturn(List.of(cliente(null, "Maria", null, "111")));

        var lista = clienteService.listarClientes(CLINICA, null, ClienteStatus.ATIVO);

        assertThat(lista).hasSize(1);
        verify(clienteRepository, never()).findByClinicaAndNomeContainingIgnoreCaseAndStatus(any(), any(), any());
    }

    @Test
    void deveListarFiltrandoPorNomeEStatus() {
        when(clienteRepository.findByClinicaAndNomeContainingIgnoreCaseAndStatus(CLINICA, "mar",
                ClienteStatus.ATIVO))
                .thenReturn(List.of(cliente(null, "Maria", null, "111")));

        var lista = clienteService.listarClientes(CLINICA, "mar", ClienteStatus.ATIVO);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).nome()).isEqualTo("Maria");
    }

    @Test
    void naoDeveAtualizarClienteInexistente() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.findByIdAndClinica(id, CLINICA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.atualizarCliente(CLINICA, id, requestAtualizacaoBasico()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void naoDeveAtualizarComEmailUsadoPorOutroCliente() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.findByIdAndClinica(id, CLINICA))
                .thenReturn(Optional.of(cliente(id, "Maria", "velha@email.com", "111")));
        when(clienteRepository.existsByEmailAndIdNotAndClinica("outra@email.com", id, CLINICA)).thenReturn(true);

        var request = new UpdateClienteRequestDTO("Maria", "outra@email.com", "111", null, null, null, null, null, null);
        assertThatThrownBy(() -> clienteService.atualizarCliente(CLINICA, id, request))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    void naoDeveAtualizarComTelefoneUsadoPorOutroCliente() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.findByIdAndClinica(id, CLINICA))
                .thenReturn(Optional.of(cliente(id, "Maria", null, "111")));
        when(clienteRepository.existsByTelefoneAndIdNotAndClinica("222", id, CLINICA)).thenReturn(true);

        var request = new UpdateClienteRequestDTO("Maria", null, "222", null, null, null, null, null, null);
        assertThatThrownBy(() -> clienteService.atualizarCliente(CLINICA, id, request))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("email em branco não passa pela checagem de duplicidade")
    void atualizacaoSemEmailNaoVerificaDuplicidadeDeEmail() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.findByIdAndClinica(id, CLINICA))
                .thenReturn(Optional.of(cliente(id, "Maria", "velha@email.com", "111")));
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = clienteService.atualizarCliente(CLINICA, id, requestAtualizacaoBasico());

        assertThat(resposta.nome()).isEqualTo("Maria");
        verify(clienteRepository, never()).existsByEmailAndIdNotAndClinica(any(), any(), any());
    }

    @Test
    void deveAtualizarCamposDoPerfilQuandoInformados() {
        UUID id = UUID.randomUUID();
        var existente = cliente(id, "Maria", null, "111");
        when(clienteRepository.findByIdAndClinica(id, CLINICA)).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var request = new UpdateClienteRequestDTO("Maria", null, "111",
                LocalDate.of(1988, 3, 15), ClienteStatus.ATIVO, OrigemCliente.GOOGLE,
                CanalPreferido.INSTAGRAM, "Unhas", "Cliente VIP");
        var resposta = clienteService.atualizarCliente(CLINICA, id, request);

        assertThat(resposta.status()).isEqualTo(ClienteStatus.ATIVO);
        assertThat(resposta.dataNascimento()).isEqualTo(LocalDate.of(1988, 3, 15));
        assertThat(resposta.origem()).isEqualTo(OrigemCliente.GOOGLE);
        assertThat(resposta.canalPreferido()).isEqualTo(CanalPreferido.INSTAGRAM);
        assertThat(resposta.preferencias()).isEqualTo("Unhas");
        assertThat(resposta.observacoes()).isEqualTo("Cliente VIP");
    }

    @Test
    void naoDeveDeletarClienteInexistente() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.existsByIdAndClinica(id, CLINICA)).thenReturn(false);

        assertThatThrownBy(() -> clienteService.deletarCliente(CLINICA, id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void naoDeveDeletarClienteComAtendimentosVinculados() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.existsByIdAndClinica(id, CLINICA)).thenReturn(true);
        when(atendimentoRepository.existsByCliente_Id(id)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.deletarCliente(CLINICA, id))
                .isInstanceOf(RecursoEmUsoException.class)
                .hasMessageContaining("atendimentos");

        verify(clienteRepository, never()).deleteById(any());
    }

    @Test
    void naoDeveDeletarClienteComNotasVinculadas() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.existsByIdAndClinica(id, CLINICA)).thenReturn(true);
        when(atendimentoRepository.existsByCliente_Id(id)).thenReturn(false);
        when(clienteNotaRepository.existsByCliente_Id(id)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.deletarCliente(CLINICA, id))
                .isInstanceOf(RecursoEmUsoException.class)
                .hasMessageContaining("anotações");

        verify(clienteRepository, never()).deleteById(any());
    }

    @Test
    void deveDeletarClienteSemVinculos() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.existsByIdAndClinica(id, CLINICA)).thenReturn(true);
        when(atendimentoRepository.existsByCliente_Id(id)).thenReturn(false);
        when(clienteNotaRepository.existsByCliente_Id(id)).thenReturn(false);

        clienteService.deletarCliente(CLINICA, id);

        verify(clienteRepository).deleteById(id);
    }

    @Test
    void naoDeveAcessarClienteDeOutraClinica() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.findByIdAndClinica(id, CLINICA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarPorId(CLINICA, id))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(clienteRepository).findByIdAndClinica(id, CLINICA);
        verify(clienteRepository, never()).findById(eq(id));
    }

    @Test
    void deveListarNotasDoCliente() {
        UUID clienteId = UUID.randomUUID();
        when(clienteRepository.findByIdAndClinica(clienteId, CLINICA))
                .thenReturn(Optional.of(cliente(clienteId, "Maria", null, "111")));
        when(clienteNotaRepository.findByCliente_IdOrderByCriadaEmDesc(clienteId))
                .thenReturn(List.of(nota(UUID.randomUUID(), clienteId, "Prefere segunda-feira")));

        var notas = clienteService.listarNotas(CLINICA, clienteId);

        assertThat(notas).hasSize(1);
        assertThat(notas.get(0).texto()).isEqualTo("Prefere segunda-feira");
    }

    @Test
    void naoDeveListarNotasDeClienteInexistente() {
        UUID clienteId = UUID.randomUUID();
        when(clienteRepository.findByIdAndClinica(clienteId, CLINICA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.listarNotas(CLINICA, clienteId))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveCriarNotaParaCliente() {
        UUID clienteId = UUID.randomUUID();
        when(clienteRepository.findByIdAndClinica(clienteId, CLINICA))
                .thenReturn(Optional.of(cliente(clienteId, "Maria", null, "111")));
        when(clienteNotaRepository.save(any(ClienteNota.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = clienteService.criarNota(CLINICA, clienteId,
                new CreateClienteNotaRequestDTO("  Prefere horário da manhã  "));

        assertThat(resposta.texto()).isEqualTo("Prefere horário da manhã");
    }

    @Test
    void naoDeveCriarNotaParaClienteInexistente() {
        UUID clienteId = UUID.randomUUID();
        when(clienteRepository.findByIdAndClinica(clienteId, CLINICA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.criarNota(CLINICA, clienteId,
                new CreateClienteNotaRequestDTO("Texto")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void naoDeveDeletarNotaInexistente() {
        UUID clienteId = UUID.randomUUID();
        UUID notaId = UUID.randomUUID();
        when(clienteRepository.existsByIdAndClinica(clienteId, CLINICA)).thenReturn(true);
        when(clienteNotaRepository.findById(notaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.deletarNota(CLINICA, clienteId, notaId))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Nota");
    }

    @Test
    void naoDeveDeletarNotaDeOutroCliente() {
        UUID clienteId = UUID.randomUUID();
        UUID notaId = UUID.randomUUID();
        when(clienteRepository.existsByIdAndClinica(clienteId, CLINICA)).thenReturn(true);
        when(clienteNotaRepository.findById(notaId))
                .thenReturn(Optional.of(nota(notaId, UUID.randomUUID(), "Outro cliente")));

        assertThatThrownBy(() -> clienteService.deletarNota(CLINICA, clienteId, notaId))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveDeletarNotaDoCliente() {
        UUID clienteId = UUID.randomUUID();
        UUID notaId = UUID.randomUUID();
        when(clienteRepository.existsByIdAndClinica(clienteId, CLINICA)).thenReturn(true);
        when(clienteNotaRepository.findById(notaId))
                .thenReturn(Optional.of(nota(notaId, clienteId, "Texto")));

        clienteService.deletarNota(CLINICA, clienteId, notaId);

        verify(clienteNotaRepository).delete(any(ClienteNota.class));
    }

    @Test
    void deveConstruirHistoricoComAgregadosFinanceiros() {
        UUID clienteId = UUID.randomUUID();
        when(clienteRepository.findByIdAndClinica(clienteId, CLINICA))
                .thenReturn(Optional.of(cliente(clienteId, "Maria", null, "111")));

        Atendimento agendado = new Atendimento();
        agendado.setDataAtendimento(LocalDateTime.of(2026, 6, 10, 15, 0));
        agendado.setStatus(StatusAtendimento.AGENDADO);
        Atendimento concluidoAntigo = new Atendimento();
        concluidoAntigo.setDataAtendimento(LocalDateTime.of(2026, 5, 2, 10, 0));
        concluidoAntigo.setStatus(StatusAtendimento.CONCLUIDO);
        Atendimento concluidoRecente = new Atendimento();
        concluidoRecente.setDataAtendimento(LocalDateTime.of(2026, 6, 1, 9, 0));
        concluidoRecente.setStatus(StatusAtendimento.CONCLUIDO);

        var idAgendado = UUID.randomUUID();
        var idAntigo = UUID.randomUUID();
        var idRecente = UUID.randomUUID();
        when(atendimentoRepository.findByCliente_Id(clienteId))
                .thenReturn(List.of(agendado, concluidoAntigo, concluidoRecente));
        when(atendimentoService.buscarPorId(CLINICA, idAgendado))
                .thenReturn(atendimentoDTO(idAgendado, "Maria", LocalDateTime.of(2026, 6, 10, 15, 0),
                        StatusAtendimento.AGENDADO, new BigDecimal("0.00")));
        when(atendimentoService.buscarPorId(CLINICA, idAntigo))
                .thenReturn(atendimentoDTO(idAntigo, "Maria", LocalDateTime.of(2026, 5, 2, 10, 0),
                        StatusAtendimento.CONCLUIDO, new BigDecimal("120.00")));
        when(atendimentoService.buscarPorId(CLINICA, idRecente))
                .thenReturn(atendimentoDTO(idRecente, "Maria", LocalDateTime.of(2026, 6, 1, 9, 0),
                        StatusAtendimento.CONCLUIDO, new BigDecimal("80.00")));

        org.springframework.test.util.ReflectionTestUtils.setField(agendado, "id", idAgendado);
        org.springframework.test.util.ReflectionTestUtils.setField(concluidoAntigo, "id", idAntigo);
        org.springframework.test.util.ReflectionTestUtils.setField(concluidoRecente, "id", idRecente);

        var historico = clienteService.historicoCliente(CLINICA, clienteId);

        assertThat(historico.totalAtendimentos()).isEqualTo(3);
        assertThat(historico.atendimentosConcluidos()).isEqualTo(2);
        assertThat(historico.gastoTotal()).isEqualByComparingTo("200.00");
        assertThat(historico.ticketMedio()).isEqualByComparingTo("100.00");
        assertThat(historico.ultimaVisita()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(historico.atendimentos()).hasSize(3);
        assertThat(historico.atendimentos().get(0).dataAtendimento())
                .isEqualTo(LocalDateTime.of(2026, 6, 10, 15, 0));
    }

    @Test
    void deveRetornarHistoricoVazioParaClienteSemAtendimentos() {
        UUID clienteId = UUID.randomUUID();
        when(clienteRepository.findByIdAndClinica(clienteId, CLINICA))
                .thenReturn(Optional.of(cliente(clienteId, "Maria", null, "111")));
        when(atendimentoRepository.findByCliente_Id(clienteId)).thenReturn(List.of());

        var historico = clienteService.historicoCliente(CLINICA, clienteId);

        assertThat(historico.totalAtendimentos()).isZero();
        assertThat(historico.gastoTotal()).isEqualByComparingTo("0.00");
        assertThat(historico.ticketMedio()).isNull();
        assertThat(historico.ultimaVisita()).isNull();
    }

    @Test
    void deveListarAniversariantesDoMesInformado() {
        UUID id = UUID.randomUUID();
        var cliente = cliente(id, "Maria", null, "111");
        cliente.setDataNascimento(LocalDate.of(1990, 6, 5));
        when(clienteRepository.findByDataNascimentoMesAndClinica(6, CLINICA)).thenReturn(List.of(cliente));

        var lista = clienteService.aniversariantes(CLINICA, 6);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).dataNascimento()).isEqualTo(LocalDate.of(1990, 6, 5));
    }

    @Test
    void deveUsarMesAtualQuandoNaoInformado() {
        int mesAtual = LocalDate.now().getMonthValue();
        when(clienteRepository.findByDataNascimentoMesAndClinica(mesAtual, CLINICA)).thenReturn(List.of());

        var lista = clienteService.aniversariantes(CLINICA, null);

        assertThat(lista).isEmpty();
        verify(clienteRepository).findByDataNascimentoMesAndClinica(mesAtual, CLINICA);
    }

    @Test
    void naoDeveAceitarMesInvalido() {
        assertThatThrownBy(() -> clienteService.aniversariantes(CLINICA, 13))
                .isInstanceOf(IllegalArgumentException.class);
    }
}