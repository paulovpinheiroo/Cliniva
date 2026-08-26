package com.cliniva.atendimento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.cliniva.atendimento.dtos.CreateAtendimentoRequestDTO;
import com.cliniva.atendimento.dtos.CreateAtendimentoRequestDTO.ServicoSelecionadoDTO;
import com.cliniva.atendimento.dtos.CreateAtendimentoRequestDTO.ServicoSelecionadoDTO.ItemUsadoDTO;
import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.atendimento.model.Atendimento;
import com.cliniva.atendimento.model.AtendimentoItem;
import com.cliniva.atendimento.model.AtendimentoServico;
import com.cliniva.atendimento.repository.AtendimentoItemRepository;
import com.cliniva.atendimento.repository.AtendimentoRepository;
import com.cliniva.atendimento.repository.AtendimentoServicoRepository;
import com.cliniva.cliente.Cliente;
import com.cliniva.cliente.ClienteRepository;
import com.cliniva.exception.EstoqueInsuficienteException;
import com.cliniva.exception.RecursoDuplicadoException;
import com.cliniva.exception.RecursoNaoEncontradoException;
import com.cliniva.exception.TransicaoStatusInvalidaException;
import com.cliniva.item.Item;
import com.cliniva.item.ItemRepository;
import com.cliniva.servico.Servico;
import com.cliniva.servico.ServicoRepository;

@ExtendWith(MockitoExtension.class)
class AtendimentoServiceTest {

        private static final UUID ATENDIMENTO_ID = UUID.randomUUID();
        private static final UUID CLIENTE_ID = UUID.randomUUID();
        private static final UUID SERVICO_ID = UUID.randomUUID();
        private static final UUID ITEM_ID = UUID.randomUUID();

        @Mock
        private AtendimentoRepository atendimentoRepository;
        @Mock
        private AtendimentoServicoRepository atendimentoServicoRepository;
        @Mock
        private AtendimentoItemRepository atendimentoItemRepository;
        @Mock
        private ClienteRepository clienteRepository;
        @Mock
        private ServicoRepository servicoRepository;
        @Mock
        private ItemRepository itemRepository;

        @InjectMocks
        private AtendimentoService atendimentoService;

        private Cliente cliente(UUID id, String nome) {
                Cliente cliente = new Cliente();
                cliente.setNome(nome);
                ReflectionTestUtils.setField(cliente, "id", id);
                return cliente;
        }

        private Servico servico(String valor) {
                Servico servico = new Servico();
                servico.setNome("Limpeza de Pele");
                servico.setValor(new BigDecimal(valor));
                ReflectionTestUtils.setField(servico, "id", SERVICO_ID);
                return servico;
        }

        private Item item(String estoqueInicial) {
                Item item = new Item();
                item.setNome("Sérum Vitamina C");
                item.adicionarQuantidade(new BigDecimal(estoqueInicial));
                ReflectionTestUtils.setField(item, "id", ITEM_ID);
                return item;
        }

        private Atendimento atendimentoComStatus(StatusAtendimento status) {
                Atendimento atendimento = new Atendimento();
                atendimento.setCliente(cliente(CLIENTE_ID, "Maria"));
                atendimento.setDataAtendimento(LocalDateTime.now().plusDays(1));
                atendimento.setStatus(status);
                ReflectionTestUtils.setField(atendimento, "id", ATENDIMENTO_ID);
                return atendimento;
        }

        private AtendimentoServico atendimentoServico(Atendimento atendimento, String valorCobrado) {
                AtendimentoServico atendimentoServico = new AtendimentoServico();
                atendimentoServico.setAtendimento(atendimento);
                atendimentoServico.setServico(servico(valorCobrado));
                atendimentoServico.setValorCobrado(new BigDecimal(valorCobrado));
                return atendimentoServico;
        }

        private AtendimentoItem atendimentoItem(AtendimentoServico atendimentoServico, Item item,
                        String quantidadeUsada) {
                AtendimentoItem atendimentoItem = new AtendimentoItem();
                atendimentoItem.setAtendimentoServico(atendimentoServico);
                atendimentoItem.setItem(item);
                atendimentoItem.setQuantidadeUsada(new BigDecimal(quantidadeUsada));
                return atendimentoItem;
        }

        private CreateAtendimentoRequestDTO requisicaoComUmServicoEItens(ItemUsadoDTO... itens) {
                return new CreateAtendimentoRequestDTO(
                                CLIENTE_ID,
                                LocalDateTime.now().plusDays(1),
                                List.of(new ServicoSelecionadoDTO(SERVICO_ID, List.of(itens))));
        }

        // ---------- criação ----------

        @Test
        void deveCriarAtendimentoGravandoSnapshotDoValorDoServico() {
                Servico servico = servico("150.00");
                Item item = item("10");

                when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente(CLIENTE_ID, "Maria")));
                when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servico));
                when(atendimentoServicoRepository.existsByAtendimentoAndServico(any(), any())).thenReturn(false);
                when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
                when(atendimentoItemRepository.findByAtendimentoServicoAndItem(any(), any()))
                                .thenReturn(Optional.empty());

                var resposta = atendimentoService.createAtendimento(
                                requisicaoComUmServicoEItens(new ItemUsadoDTO(ITEM_ID, new BigDecimal("2"))));

                assertThat(resposta.status()).isEqualTo(StatusAtendimento.AGENDADO);
                assertThat(resposta.servicos()).hasSize(1);

                ArgumentCaptor<AtendimentoServico> captor = ArgumentCaptor.forClass(AtendimentoServico.class);
                verify(atendimentoServicoRepository).save(captor.capture());
                assertThat(captor.getValue().getValorCobrado()).isEqualByComparingTo("150.00");

                assertThat(item.getQuantidadeEmEstoque()).isEqualByComparingTo("8");
                verify(itemRepository).save(item);
        }

        @Test
        void deveSomarQuantidadeQuandoMesmoItemApareceDuasVezesNoServico() {
                Item item = item("10");
                AtomicReference<AtendimentoItem> salvo = new AtomicReference<>();

                when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente(CLIENTE_ID, "Maria")));
                when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servico("100.00")));
                when(atendimentoServicoRepository.existsByAtendimentoAndServico(any(), any())).thenReturn(false);
                when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
                when(atendimentoItemRepository.findByAtendimentoServicoAndItem(any(), any()))
                                .thenReturn(Optional.empty())
                                .thenAnswer(invocacao -> Optional.ofNullable(salvo.get()));
                when(atendimentoItemRepository.save(any(AtendimentoItem.class)))
                                .thenAnswer(invocacao -> {
                                        salvo.set(invocacao.getArgument(0));
                                        return salvo.get();
                                });

                var resposta = atendimentoService.createAtendimento(requisicaoComUmServicoEItens(
                                new ItemUsadoDTO(ITEM_ID, BigDecimal.ONE),
                                new ItemUsadoDTO(ITEM_ID, new BigDecimal("2"))));

                assertThat(resposta.servicos().get(0).itensExtras()).hasSize(2);
                assertThat(resposta.servicos().get(0).itensExtras().getLast().quantidadeUsada())
                                .isEqualByComparingTo("3");
                assertThat(item.getQuantidadeEmEstoque()).isEqualByComparingTo("7");
        }

        @Test
        void naoDeveCriarAtendimentoComClienteInexistente() {
                when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> atendimentoService.createAtendimento(
                                requisicaoComUmServicoEItens(new ItemUsadoDTO(ITEM_ID, BigDecimal.ONE))))
                                .isInstanceOf(RecursoNaoEncontradoException.class)
                                .hasMessageContaining("Cliente");
        }

        @Test
        void naoDeveCriarAtendimentoComServicoInexistente() {
                when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente(CLIENTE_ID, "Maria")));
                when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> atendimentoService.createAtendimento(
                                requisicaoComUmServicoEItens(new ItemUsadoDTO(ITEM_ID, BigDecimal.ONE))))
                                .isInstanceOf(RecursoNaoEncontradoException.class)
                                .hasMessageContaining("Serviço");
        }

        @Test
        void naoDeveCriarAtendimentoComItemInexistente() {
                when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente(CLIENTE_ID, "Maria")));
                when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servico("100.00")));
                when(atendimentoServicoRepository.existsByAtendimentoAndServico(any(), any())).thenReturn(false);
                when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> atendimentoService.createAtendimento(
                                requisicaoComUmServicoEItens(new ItemUsadoDTO(ITEM_ID, BigDecimal.ONE))))
                                .isInstanceOf(RecursoNaoEncontradoException.class)
                                .hasMessageContaining("Item");
        }

        @Test
        void naoDeveAceitarOMesmoServicoDuasVezesNoAtendimento() {
                when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente(CLIENTE_ID, "Maria")));
                when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servico("100.00")));
                when(atendimentoServicoRepository.existsByAtendimentoAndServico(any(), any()))
                                .thenReturn(false, true);

                CreateAtendimentoRequestDTO requisicao = new CreateAtendimentoRequestDTO(
                                CLIENTE_ID,
                                LocalDateTime.now().plusDays(1),
                                List.of(
                                                new ServicoSelecionadoDTO(SERVICO_ID, List.of()),
                                                new ServicoSelecionadoDTO(SERVICO_ID, List.of())));

                assertThatThrownBy(() -> atendimentoService.createAtendimento(requisicao))
                                .isInstanceOf(RecursoDuplicadoException.class);
        }

        @Test
        void naoDeveCriarAtendimentoSemEstoqueSuficiente() {
                Item item = item("1");

                when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente(CLIENTE_ID, "Maria")));
                when(servicoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servico("100.00")));
                when(atendimentoServicoRepository.existsByAtendimentoAndServico(any(), any())).thenReturn(false);
                when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
                when(atendimentoItemRepository.findByAtendimentoServicoAndItem(any(), any()))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> atendimentoService.createAtendimento(
                                requisicaoComUmServicoEItens(new ItemUsadoDTO(ITEM_ID, new BigDecimal("5")))))
                                .isInstanceOf(EstoqueInsuficienteException.class);

                assertThat(item.getQuantidadeEmEstoque()).isEqualByComparingTo("1");
        }

        // ---------- mudança de status ----------

        @Test
        void deveConcluirAtendimentoAgendado() {
                Atendimento atendimento = atendimentoComStatus(StatusAtendimento.AGENDADO);
                when(atendimentoRepository.findById(ATENDIMENTO_ID)).thenReturn(Optional.of(atendimento));

                var resposta = atendimentoService.alterarStatus(ATENDIMENTO_ID, StatusAtendimento.CONCLUIDO);

                assertThat(resposta.status()).isEqualTo(StatusAtendimento.CONCLUIDO);
                verify(itemRepository, never()).save(any());
        }

        @Test
        void cancelamentoDeAtendimentoAgendadoDeveDevolverEstoque() {
                Atendimento atendimento = atendimentoComStatus(StatusAtendimento.AGENDADO);
                Item item = item("5");
                AtendimentoItem consumo = atendimentoItem(
                                atendimentoServico(atendimento, "100.00"), item, "2");

                when(atendimentoRepository.findById(ATENDIMENTO_ID)).thenReturn(Optional.of(atendimento));
                when(atendimentoItemRepository.findByAtendimentoServico_Atendimento(atendimento))
                                .thenReturn(List.of(consumo));

                var resposta = atendimentoService.alterarStatus(ATENDIMENTO_ID, StatusAtendimento.CANCELADO);

                assertThat(resposta.status()).isEqualTo(StatusAtendimento.CANCELADO);
                assertThat(item.getQuantidadeEmEstoque()).isEqualByComparingTo("7");
                verify(itemRepository).save(item);
        }

        @Test
        void correcaoDeConcluidoParaCanceladoNaoDeveDevolverEstoque() {
                Atendimento atendimento = atendimentoComStatus(StatusAtendimento.CONCLUIDO);

                when(atendimentoRepository.findById(ATENDIMENTO_ID)).thenReturn(Optional.of(atendimento));

                var resposta = atendimentoService.alterarStatus(ATENDIMENTO_ID, StatusAtendimento.CANCELADO);

                assertThat(resposta.status()).isEqualTo(StatusAtendimento.CANCELADO);
                verify(itemRepository, never()).save(any());
        }

        @Test
        void canceladoEDefinitivoENaoPodeVoltar() {
                Atendimento cancelado = atendimentoComStatus(StatusAtendimento.CANCELADO);
                when(atendimentoRepository.findById(ATENDIMENTO_ID)).thenReturn(Optional.of(cancelado));

                assertThatThrownBy(() -> atendimentoService.alterarStatus(ATENDIMENTO_ID, StatusAtendimento.AGENDADO))
                                .isInstanceOf(TransicaoStatusInvalidaException.class)
                                .hasMessageContaining("cancelado");
        }

        @Test
        void naoDeveAlterarParaOMesmoStatusAtual() {
                Atendimento agendado = atendimentoComStatus(StatusAtendimento.AGENDADO);
                when(atendimentoRepository.findById(ATENDIMENTO_ID)).thenReturn(Optional.of(agendado));

                assertThatThrownBy(() -> atendimentoService.alterarStatus(ATENDIMENTO_ID, StatusAtendimento.AGENDADO))
                                .isInstanceOf(TransicaoStatusInvalidaException.class);
        }

        @Test
        void naoDeveAlterarStatusDeAtendimentoInexistente() {
                when(atendimentoRepository.findById(ATENDIMENTO_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> atendimentoService.alterarStatus(ATENDIMENTO_ID, StatusAtendimento.CONCLUIDO))
                                .isInstanceOf(RecursoNaoEncontradoException.class);
        }

        // ---------- remarcação ----------

        @Test
        void deveRemarcarDataEClienteDeAtendimentoAgendado() {
                Atendimento atendimento = atendimentoComStatus(StatusAtendimento.AGENDADO);
                UUID novoClienteId = UUID.randomUUID();
                LocalDateTime novaData = LocalDateTime.now().plusDays(7);

                when(atendimentoRepository.findById(ATENDIMENTO_ID)).thenReturn(Optional.of(atendimento));
                when(clienteRepository.findById(novoClienteId))
                                .thenReturn(Optional.of(cliente(novoClienteId, "Joana")));

                atendimentoService.atualizarAtendimento(ATENDIMENTO_ID,
                                new com.cliniva.atendimento.dtos.UpdateAtendimentoRequestDTO(novoClienteId, novaData));

                assertThat(atendimento.getDataAtendimento()).isEqualTo(novaData);
                assertThat(atendimento.getCliente().getNome()).isEqualTo("Joana");
                verify(atendimentoRepository).save(atendimento);
        }

        @Test
        void naoDeveRemarcarAtendimentoConcluido() {
                Atendimento concluido = atendimentoComStatus(StatusAtendimento.CONCLUIDO);
                when(atendimentoRepository.findById(ATENDIMENTO_ID)).thenReturn(Optional.of(concluido));

                assertThatThrownBy(() -> atendimentoService.atualizarAtendimento(ATENDIMENTO_ID,
                                new com.cliniva.atendimento.dtos.UpdateAtendimentoRequestDTO(CLIENTE_ID,
                                                LocalDateTime.now())))
                                .isInstanceOf(TransicaoStatusInvalidaException.class)
                                .hasMessageContaining("AGENDADO");
        }

        @Test
        void naoDeveRemarcarComClienteInexistente() {
                Atendimento agendado = atendimentoComStatus(StatusAtendimento.AGENDADO);
                UUID inexistente = UUID.randomUUID();

                when(atendimentoRepository.findById(ATENDIMENTO_ID)).thenReturn(Optional.of(agendado));
                when(clienteRepository.findById(inexistente)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> atendimentoService.atualizarAtendimento(ATENDIMENTO_ID,
                                new com.cliniva.atendimento.dtos.UpdateAtendimentoRequestDTO(inexistente,
                                                LocalDateTime.now())))
                                .isInstanceOf(RecursoNaoEncontradoException.class);
        }

        // ---------- leitura ----------

        @SuppressWarnings("unchecked")
        @Test
        void deveListarResumosComValorTotalCalculado() {
                Atendimento primeiro = atendimentoComStatus(StatusAtendimento.AGENDADO);
                Atendimento segundo = atendimentoComStatus(StatusAtendimento.CONCLUIDO);

                when(atendimentoRepository.findAll(any(Specification.class)))
                                .thenReturn(List.of(primeiro, segundo));
                when(atendimentoServicoRepository.findByAtendimento(primeiro))
                                .thenReturn(List.of(
                                                atendimentoServico(primeiro, "60.00"),
                                                atendimentoServico(primeiro, "40.00")));
                when(atendimentoServicoRepository.findByAtendimento(segundo))
                                .thenReturn(List.of(atendimentoServico(segundo, "25.00")));

                var lista = atendimentoService.listarAtendimentos(
                                StatusAtendimento.AGENDADO, null, null, null);

                assertThat(lista).hasSize(2);
                assertThat(lista.get(0).nomeCliente()).isEqualTo("Maria");
                assertThat(lista.get(0).valorTotal()).isEqualByComparingTo("100.00");
                assertThat(lista.get(1).valorTotal()).isEqualByComparingTo("25.00");
        }

        @Test
        void deveRetornarDetalheAninhadoComItensUsados() {
                Atendimento atendimento = atendimentoComStatus(StatusAtendimento.AGENDADO);
                Item item = item("10");
                AtendimentoServico atendimentoServico = atendimentoServico(atendimento, "100.00");
                AtendimentoItem consumo = atendimentoItem(atendimentoServico, item, "3");

                when(atendimentoRepository.findById(ATENDIMENTO_ID)).thenReturn(Optional.of(atendimento));
                when(atendimentoServicoRepository.findByAtendimento(atendimento))
                                .thenReturn(List.of(atendimentoServico));
                when(atendimentoItemRepository.findByAtendimentoServico(atendimentoServico))
                                .thenReturn(List.of(consumo));

                var resposta = atendimentoService.buscarPorId(ATENDIMENTO_ID);

                assertThat(resposta.nomeCliente()).isEqualTo("Maria");
                assertThat(resposta.valorTotal()).isEqualByComparingTo("100.00");
                assertThat(resposta.servicos()).hasSize(1);
                assertThat(resposta.servicos().get(0).nomeServico()).isEqualTo("Limpeza de Pele");
                assertThat(resposta.servicos().get(0).itensUsados().get(0).nomeItem())
                                .isEqualTo("Sérum Vitamina C");
                assertThat(resposta.servicos().get(0).itensUsados().get(0).quantidadeUsada())
                                .isEqualByComparingTo("3");
        }

        @Test
        void naoDeveBuscarAtendimentoInexistente() {
                when(atendimentoRepository.findById(ATENDIMENTO_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> atendimentoService.buscarPorId(ATENDIMENTO_ID))
                                .isInstanceOf(RecursoNaoEncontradoException.class);
        }
}
