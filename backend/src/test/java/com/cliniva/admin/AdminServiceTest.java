package com.cliniva.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import com.cliniva.admin.dtos.AdminDtos.AtualizarClinicaRequestDTO;
import com.cliniva.admin.dtos.AdminDtos.CriarClinicaRequestDTO;
import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.atendimento.repository.AtendimentoRepository;
import com.cliniva.atendimento.repository.AtendimentoServicoRepository;
import com.cliniva.cliente.ClienteRepository;
import com.cliniva.exception.RecursoNaoEncontradoException;
import com.cliniva.exception.SupabaseIndisponivelException;
import com.cliniva.tenancy.Clinica;
import com.cliniva.tenancy.ClinicaRepository;
import com.cliniva.tenancy.Papel;
import com.cliniva.tenancy.SupabaseUsersService;
import com.cliniva.tenancy.Usuario;
import com.cliniva.tenancy.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ClinicaRepository clinicaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private AtendimentoRepository atendimentoRepository;
    @Mock
    private AtendimentoServicoRepository atendimentoServicoRepository;
    @Mock
    private SupabaseUsersService supabaseUsers;

    @InjectMocks
    private AdminService adminService;

    private Usuario usuario(String email) {
        Usuario usuario = new Usuario();
        org.springframework.test.util.ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        usuario.setEmail(email);
        usuario.setNome("Maria");
        usuario.setPapel(Papel.OWNER);
        return usuario;
    }

    @Test
    void deveCriarClinicaComResponsavelSemSupabase() {
        when(clinicaRepository.existsByNome("Nova Clínica")).thenReturn(false);
        when(usuarioRepository.existsByEmail("dona@email.com")).thenReturn(false);
        when(supabaseUsers.configurada()).thenReturn(false);
        when(clinicaRepository.save(any(Clinica.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = adminService.criarClinica(
                new CriarClinicaRequestDTO("Nova Clínica", "dona@email.com", "Dona Maria"));

        assertThat(resposta.clinicaNome()).isEqualTo("Nova Clínica");
        assertThat(resposta.senhaTemporaria()).isNull();
        verify(supabaseUsers, never()).criarUsuario(anyString(), anyString());
    }

    @Test
    void deveCriarClinicaCriandoUsuarioNoSupabase() {
        when(clinicaRepository.existsByNome("Nova Clínica")).thenReturn(false);
        when(usuarioRepository.existsByEmail("dona@email.com")).thenReturn(false);
        when(supabaseUsers.configurada()).thenReturn(true);
        when(supabaseUsers.criarUsuario(eq("dona@email.com"), anyString()))
                .thenReturn(new SupabaseUsersService.UsuarioSupabase(UUID.randomUUID().toString(), "dona@email.com"));
        when(clinicaRepository.save(any(Clinica.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = adminService.criarClinica(
                new CriarClinicaRequestDTO("Nova Clínica", "dona@email.com", "Dona Maria"));

        assertThat(resposta.senhaTemporaria()).isNotBlank();
        var captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPapel()).isEqualTo(Papel.OWNER);
        assertThat(captor.getValue().getSupabaseUserId()).isNotNull();
    }

    @Test
    void naoDeveCriarClinicaComNomeDuplicado() {
        when(clinicaRepository.existsByNome("Nova Clínica")).thenReturn(true);

        assertThatThrownBy(() -> adminService.criarClinica(
                new CriarClinicaRequestDTO("Nova Clínica", "dona@email.com", "Dona Maria")))
                .hasMessageContaining("nome");
    }

    @Test
    void naoDeveResetarSenhaDeUsuarioSemSupabase() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario("maria@email.com")));

        assertThatThrownBy(() -> adminService.resetarSenha(id))
                .isInstanceOf(SupabaseIndisponivelException.class);
    }

    @Test
    void deveDetalharClinicaComReceitaConcluida() {
        Clinica clinica = new Clinica();
        org.springframework.test.util.ReflectionTestUtils.setField(clinica, "id", UUID.randomUUID());
        clinica.setNome("Clínica A");
        clinica.setAtiva(true);

        when(clinicaRepository.findById(clinica.getId())).thenReturn(Optional.of(clinica));
        when(usuarioRepository.findByClinica_IdOrderByNomeAsc(clinica.getId()))
                .thenReturn(List.of(usuario("maria@email.com")));
        when(atendimentoServicoRepository.totalCobradoPorClinica(StatusAtendimento.CONCLUIDO))
                .thenReturn(List.<Object[]>of(new Object[] { clinica.getId(), new BigDecimal("250.00") }));

        var detalhe = adminService.detalharClinica(clinica.getId());

        assertThat(detalhe.responsaveis()).hasSize(1);
        assertThat(detalhe.receita()).isEqualByComparingTo("250.00");
        assertThat(detalhe.totalAtendimentos()).isZero();
    }

    @Test
    void deveListarMetricasPorClinica() {
        Clinica clinica = new Clinica();
        org.springframework.test.util.ReflectionTestUtils.setField(clinica, "id", UUID.randomUUID());
        clinica.setNome("Clínica A");

        when(clinicaRepository.count()).thenReturn(1L);
        when(clinicaRepository.findAll()).thenReturn(List.of(clinica));
        when(clienteRepository.contarPorClinica())
                .thenReturn(List.<Object[]>of(new Object[] { clinica.getId(), 5L }));
        when(atendimentoRepository.contarPorClinica())
                .thenReturn(List.<Object[]>of(new Object[] { clinica.getId(), 3L }));
        when(atendimentoServicoRepository.totalCobradoPorClinica(StatusAtendimento.CONCLUIDO))
                .thenReturn(List.<Object[]>of(new Object[] { clinica.getId(), new BigDecimal("100.00") }));

        var metricas = adminService.metricas();

        assertThat(metricas.totalClinicas()).isEqualTo(1);
        assertThat(metricas.totalClientes()).isEqualTo(5);
        assertThat(metricas.totalAtendimentos()).isEqualTo(3);
        assertThat(metricas.porClinica()).hasSize(1);
        assertThat(metricas.porClinica().get(0).receita()).isEqualByComparingTo("100.00");
    }

    @Test
    void naoDeveAtualizarClinicaInexistente() {
        UUID id = UUID.randomUUID();
        when(clinicaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.atualizarClinica(id,
                new AtualizarClinicaRequestDTO("Outro", false)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveAtualizarNomeEAtivaDaClinica() {
        Clinica clinica = new Clinica();
        org.springframework.test.util.ReflectionTestUtils.setField(clinica, "id", UUID.randomUUID());
        clinica.setNome("Clínica A");
        clinica.setAtiva(true);

        when(clinicaRepository.findById(clinica.getId())).thenReturn(Optional.of(clinica));
        when(clinicaRepository.existsByNomeAndIdNot("Clínica B", clinica.getId())).thenReturn(false);
        when(usuarioRepository.findByClinica_IdOrderByNomeAsc(clinica.getId())).thenReturn(List.of());
        when(atendimentoServicoRepository.totalCobradoPorClinica(StatusAtendimento.CONCLUIDO))
                .thenReturn(List.of());

        var detalhe = adminService.atualizarClinica(clinica.getId(),
                new AtualizarClinicaRequestDTO("Clínica B", false));

        assertThat(detalhe.nome()).isEqualTo("Clínica B");
        assertThat(detalhe.ativa()).isFalse();
    }
}