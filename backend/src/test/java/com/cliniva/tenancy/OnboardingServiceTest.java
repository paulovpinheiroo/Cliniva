package com.cliniva.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cliniva.tenancy.dtos.TenancyDtos.CadastroOnboardingRequestDTO;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private ClinicaRepository clinicaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SupabaseUsersService supabaseUsers;

    @InjectMocks
    private OnboardingService onboardingService;

    @Test
    void deveCadastrarClinicaEResponsavelOwnerr() {
        when(clinicaRepository.existsByNome("Clínica Teste")).thenReturn(false);
        when(usuarioRepository.existsByEmail("dona@email.com")).thenReturn(false);
        when(supabaseUsers.configurada()).thenReturn(false);
        when(clinicaRepository.save(any(Clinica.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

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
        when(clinicaRepository.existsByNome("Clínica Teste")).thenReturn(false);
        when(usuarioRepository.existsByEmail("dona@email.com")).thenReturn(false);
        when(supabaseUsers.configurada()).thenReturn(true);
        when(supabaseUsers.buscarPorEmail("dona@email.com"))
                .thenReturn(Optional.of(new SupabaseUsersService.UsuarioSupabase("sup-123", "dona@email.com")));
        when(clinicaRepository.save(any(Clinica.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        onboardingService.cadastrarClinica(
                new CadastroOnboardingRequestDTO("Clínica Teste", "Dona Maria", "dona@email.com"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getSupabaseUserId()).isEqualTo("sup-123");
    }

    @Test
    void naoDeveCadastrarComNomeDeClinicaJaExistente() {
        when(clinicaRepository.existsByNome("Clínica Teste")).thenReturn(true);

        assertThatThrownBy(() -> onboardingService.cadastrarClinica(
                new CadastroOnboardingRequestDTO("Clínica Teste", "Dona Maria", "dona@email.com")))
                .hasMessageContaining("nome");
    }

    @Test
    void naoDeveCadastrarComEmailJaExistente() {
        when(clinicaRepository.existsByNome("Clínica Teste")).thenReturn(false);
        when(usuarioRepository.existsByEmail("dona@email.com")).thenReturn(true);

        assertThatThrownBy(() -> onboardingService.cadastrarClinica(
                new CadastroOnboardingRequestDTO("Clínica Teste", "Dona Maria", "dona@email.com")))
                .hasMessageContaining("E-mail");
    }
}