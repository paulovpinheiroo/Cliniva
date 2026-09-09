package com.cliniva.cliente;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.cliniva.tenancy.Clinica;
import com.cliniva.tenancy.ClinicaRepository;

@DataJpaTest
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ClinicaRepository clinicaRepository;

    @Test
    void deveEncontrarAniversariantesDoMesDaClinica() {
        Clinica clinica = clinicaPadrao();
        var nascidaJunho = cliente("Maria", "111", LocalDate.of(1990, 6, 5), clinica);
        var nascidaNovembro = cliente("Joana", "222", LocalDate.of(1988, 11, 20), clinica);
        var semData = cliente("Pedro", "333", null, clinica);
        clienteRepository.saveAll(List.of(nascidaJunho, nascidaNovembro, semData));

        var aniversariantes = clienteRepository.findByDataNascimentoMesAndClinica(6, clinica);

        assertThat(aniversariantes).hasSize(1);
        assertThat(aniversariantes.get(0).getNome()).isEqualTo("Maria");
    }

    @Test
    void naoDeveEncontrarAniversariantesDeOutraClinica() {
        Clinica clinicaBase = clinicaPadrao();
        Clinica outraClinica = outraClinica();
        var nascidaJunho = cliente("Maria", "111", LocalDate.of(1990, 6, 5), clinicaBase);
        clienteRepository.save(nascidaJunho);

        var aniversariantes = clienteRepository.findByDataNascimentoMesAndClinica(6, outraClinica);

        assertThat(aniversariantes).isEmpty();
    }

    private Clinica clinicaPadrao() {
        Clinica clinica = new Clinica();
        clinica.setNome("Clínica A");
        return clinicaRepository.save(clinica);
    }

    private Clinica outraClinica() {
        Clinica clinica = new Clinica();
        clinica.setNome("Clínica B");
        return clinicaRepository.save(clinica);
    }

    private Cliente cliente(String nome, String telefone, LocalDate dataNascimento, Clinica clinica) {
        Cliente cliente = new Cliente();
        cliente.setNome(nome);
        cliente.setTelefone(telefone);
        cliente.setDataNascimento(dataNascimento);
        cliente.setClinica(clinica);
        return cliente;
    }
}