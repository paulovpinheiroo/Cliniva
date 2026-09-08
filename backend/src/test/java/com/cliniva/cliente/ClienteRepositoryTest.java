package com.cliniva.cliente;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    void deveEncontrarAniversariantesDoMes() {
        var nascidaJunho = new Cliente();
        nascidaJunho.setNome("Maria");
        nascidaJunho.setTelefone("111");
        nascidaJunho.setDataNascimento(LocalDate.of(1990, 6, 5));
        var nascidaNovembro = new Cliente();
        nascidaNovembro.setNome("Joana");
        nascidaNovembro.setTelefone("222");
        nascidaNovembro.setDataNascimento(LocalDate.of(1988, 11, 20));
        var semData = new Cliente();
        semData.setNome("Pedro");
        semData.setTelefone("333");
        clienteRepository.saveAll(List.of(nascidaJunho, nascidaNovembro, semData));

        var aniversariantes = clienteRepository.findByDataNascimentoMes(6);

        assertThat(aniversariantes).hasSize(1);
        assertThat(aniversariantes.get(0).getNome()).isEqualTo("Maria");
    }

    @Test
    void naoDeveEncontrarQuandoMesNaoExiste() {
        var cliente = new Cliente();
        cliente.setNome("Joana");
        cliente.setTelefone("222");
        cliente.setDataNascimento(LocalDate.of(1988, 11, 20));
        clienteRepository.save(cliente);

        var aniversariantes = clienteRepository.findByDataNascimentoMes(6);

        assertThat(aniversariantes).isEmpty();
    }
}