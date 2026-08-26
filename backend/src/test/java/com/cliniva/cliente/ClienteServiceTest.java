package com.cliniva.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cliniva.atendimento.repository.AtendimentoRepository;
import com.cliniva.cliente.dtos.CreateClienteRequestDTO;
import com.cliniva.cliente.dtos.UpdateClienteRequestDTO;
import com.cliniva.exception.RecursoDuplicadoException;
import com.cliniva.exception.RecursoEmUsoException;
import com.cliniva.exception.RecursoNaoEncontradoException;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private AtendimentoRepository atendimentoRepository;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente(UUID id, String nome, String email, String telefone) {
        Cliente cliente = new Cliente();
        cliente.setNome(nome);
        cliente.setEmail(email);
        cliente.setTelefone(telefone);
        if (id != null) {
            org.springframework.test.util.ReflectionTestUtils.setField(cliente, "id", id);
        }
        return cliente;
    }

    @Test
    void deveCriarClienteComDadosValidos() {
        when(clienteRepository.existsByEmail("maria@email.com")).thenReturn(false);
        when(clienteRepository.existsByTelefone("11999990000")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = clienteService.createCliente(
                new CreateClienteRequestDTO("Maria", "maria@email.com", "11999990000"));

        assertThat(resposta.nome()).isEqualTo("Maria");
        assertThat(resposta.email()).isEqualTo("maria@email.com");
        assertThat(resposta.telefone()).isEqualTo("11999990000");
    }

    @Test
    void naoDeveCriarClienteComEmailDuplicado() {
        when(clienteRepository.existsByEmail("maria@email.com")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.createCliente(
                new CreateClienteRequestDTO("Maria", "maria@email.com", "11999990000")))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("Email");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarClienteComTelefoneDuplicado() {
        when(clienteRepository.existsByEmail("maria@email.com")).thenReturn(false);
        when(clienteRepository.existsByTelefone("11999990000")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.createCliente(
                new CreateClienteRequestDTO("Maria", "maria@email.com", "11999990000")))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("Telefone");
    }

    @Test
    void deveListarSemFiltroQuandoNomeNaoInformado() {
        when(clienteRepository.findAll())
                .thenReturn(List.of(cliente(null, "Maria", null, "111")));

        var lista = clienteService.listarClientes(null);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).nome()).isEqualTo("Maria");
        verify(clienteRepository, never()).findByNomeContainingIgnoreCase(any());
    }

    @Test
    void deveListarFiltrandoPorNome() {
        when(clienteRepository.findByNomeContainingIgnoreCase("mar"))
                .thenReturn(List.of(cliente(null, "Maria", null, "111")));

        var lista = clienteService.listarClientes("mar");

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).nome()).isEqualTo("Maria");
    }

    @Test
    void naoDeveAtualizarClienteInexistente() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.atualizarCliente(
                id, new UpdateClienteRequestDTO("Novo", null, "222")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void naoDeveAtualizarComEmailUsadoPorOutroCliente() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.findById(id))
                .thenReturn(Optional.of(cliente(id, "Maria", "velha@email.com", "111")));
        when(clienteRepository.existsByEmailAndIdNot("outra@email.com", id)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.atualizarCliente(
                id, new UpdateClienteRequestDTO("Maria", "outra@email.com", "111")))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    void naoDeveAtualizarComTelefoneUsadoPorOutroCliente() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.findById(id))
                .thenReturn(Optional.of(cliente(id, "Maria", null, "111")));
        when(clienteRepository.existsByTelefoneAndIdNot("222", id)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.atualizarCliente(
                id, new UpdateClienteRequestDTO("Maria", null, "222")))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("email em branco não passa pela checagem de duplicidade")
    void atualizacaoSemEmailNaoVerificaDuplicidadeDeEmail() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.findById(id))
                .thenReturn(Optional.of(cliente(id, "Maria", "velha@email.com", "111")));
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = clienteService.atualizarCliente(
                id, new UpdateClienteRequestDTO("Maria Silva", null, "111"));

        assertThat(resposta.nome()).isEqualTo("Maria Silva");
        verify(clienteRepository, never()).existsByEmailAndIdNot(any(), any());
    }

    @Test
    void naoDeveDeletarClienteInexistente() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> clienteService.deletarCliente(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void naoDeveDeletarClienteComAtendimentosVinculados() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.existsById(id)).thenReturn(true);
        when(atendimentoRepository.existsByCliente_Id(id)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.deletarCliente(id))
                .isInstanceOf(RecursoEmUsoException.class)
                .hasMessageContaining("atendimentos");

        verify(clienteRepository, never()).deleteById(any());
    }

    @Test
    void deveDeletarClienteSemVinculos() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.existsById(id)).thenReturn(true);
        when(atendimentoRepository.existsByCliente_Id(id)).thenReturn(false);

        clienteService.deletarCliente(id);

        verify(clienteRepository).deleteById(id);
    }
}
