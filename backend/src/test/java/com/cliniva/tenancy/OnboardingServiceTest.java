package com.cliniva.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cliniva.agenda.AgendaService;
import com.cliniva.exception.RecursoDuplicadoException;
import com.cliniva.tenancy.dtos.TenancyDtos.CadastroOnboardingRequestDTO;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SupabaseUsersService supabaseUsers;
    @Mock
    private ClinicaProvisioningService clinicaProvisioning;

    @InjectMocks
    private OnboardingService onboardingService;

    @Test
    void deveCadastrarClinicaEResponsavelOwner() {
        Clinica clinica = new Clinica();
        org.springframework.test.util.ReflectionTestUtils.setField(clinica, "id", UUID.randomUUID());
        clinica.setNome("Clínica Teste");
        clinica.setSlug("clinica-teste");

        when(usuarioRepository.findByEmail("dona@email.com")).thenReturn(Optional.empty());
        when(supabaseUsers.configurada()).thenReturn(false);
        when(clinicaProvisioning.criarClinica("Clínica Teste")).thenReturn(clinica);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        var resposta = onboardingService.cadastrarClinica(
                new CadastroOnboardingRequestDTO("Clínica Teste", "Dona Maria", "dona@email.com"));

        assertThat(resposta.clinicaNome()).isEqualTo("Clínica Teste");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPapel()).isEqualTo(Papel.OWNER);
        assertThat(captor.getValue().getClinica().getNome()).isEqualTo("Clínica Teste");
        assertThat(captor.getValue().getEmail()).isEqualTo("dona@email.com");
    }

    @Test
    void deveVincularUsuarioSupabaseJaExistente() {
        Clinica clinica = new Clinica();
        org.springframework.test.util.ReflectionTestUtils.setField(clinica, "id", UUID.randomUUID());
        clinica.setNome("Clínica Teste");
        clinica.setSlug("clinica-teste");

        when(usuarioRepository.findByEmail("dona@email.com")).thenReturn(Optional.empty());
        when(supabaseUsers.configurada()).thenReturn(true);
        when(supabaseUsers.buscarPorEmail("dona@email.com"))
                .thenReturn(Optional.of(new SupabaseUsersService.UsuarioSupabase("sup-123", "dona@email.com")));
        when(clinicaProvisioning.criarClinica("Clínica Teste")).thenReturn(clinica);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        onboardingService.cadastrarClinica(
                new CadastroOnboardingRequestDTO("Clínica Teste", "Dona Maria", "dona@email.com"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getSupabaseUserId()).isEqualTo("sup-123");
    }

    @Test
    void naoDevePermitirQueOnboardingRebaixeAdminMaster() {
        Usuario admin = new Usuario();
        admin.setEmail("admin@cliniva.com");
        admin.setPapel(Papel.ADMIN);
        when(usuarioRepository.findByEmail("admin@cliniva.com")).thenReturn(Optional.of(admin));
        when(supabaseUsers.configurada()).thenReturn(false);

        assertThatThrownBy(() -> onboardingService.cadastrarClinica(
                new CadastroOnboardingRequestDTO("Clínica X", "Admin", "admin@cliniva.com")))
                .isInstanceOf(com.cliniva.exception.AcessoNaoPermitidoException.class)
                .hasMessageContaining("administradora");

        verify(clinicaProvisioning, org.mockito.Mockito.never()).criarClinica(any());
    }

    @Test
    void deveRetornarClinicaJaExistenteSeEmailJaCadastrado() {
        Clinica clinica = new Clinica();
        clinica.setNome("Clínica Existente");
        clinica.setSlug("clinica-existente");
        Usuario existente = new Usuario();
        existente.setId(java.util.UUID.fromString("00000000-0000-0000-0000-000000000009"));
        existente.setEmail("dona@email.com");
        existente.setClinica(clinica);
        when(usuarioRepository.findByEmail("dona@email.com")).thenReturn(Optional.of(existente));
        when(supabaseUsers.configurada()).thenReturn(false);

        var resposta = onboardingService.cadastrarClinica(
                new CadastroOnboardingRequestDTO("Clínica Teste", "Dona Maria", "dona@email.com"));

        assertThat(resposta.responsavelId()).isEqualTo(existente.getId());
        assertThat(resposta.clinicaNome()).isEqualTo("Clínica Existente");
    }

    @Test
    void deveDelegarCriacaoDaClinicaAoProvisionamento() {
        Clinica clinica = new Clinica();
        org.springframework.test.util.ReflectionTestUtils.setField(clinica, "id", UUID.randomUUID());
        clinica.setNome("Clínica Teste");
        clinica.setSlug("clinica-teste");

        when(usuarioRepository.findByEmail("dona@email.com")).thenReturn(Optional.empty());
        when(supabaseUsers.configurada()).thenReturn(false);
        when(clinicaProvisioning.criarClinica("Clínica Teste")).thenReturn(clinica);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        onboardingService.cadastrarClinica(
                new CadastroOnboardingRequestDTO("Clínica Teste", "Dona Maria", "dona@email.com"));

        // slug + expediente vêm garantidos pelo provisionamento compartilhado
        verify(clinicaProvisioning).criarClinica("Clínica Teste");
    }
}
