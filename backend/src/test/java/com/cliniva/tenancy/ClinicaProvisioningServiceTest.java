package com.cliniva.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cliniva.agenda.AgendaService;
import com.cliniva.exception.RecursoDuplicadoException;

@ExtendWith(MockitoExtension.class)
class ClinicaProvisioningServiceTest {

    @Mock
    private ClinicaRepository clinicaRepository;
    @Mock
    private AgendaService agendaService;

    @InjectMocks
    private ClinicaProvisioningService provisioning;

    @Test
    void deveGerarSlugNormalizadoEApartirDoExpediente() {
        when(clinicaRepository.existsByNome("Clínica Ação")).thenReturn(false);
        when(clinicaRepository.existsBySlug("clinica-acao")).thenReturn(false);
        when(clinicaRepository.save(any(Clinica.class))).thenAnswer(i -> i.getArgument(0));

        Clinica clinica = provisioning.criarClinica("  Clínica Ação  ");

        assertThat(clinica.getNome()).isEqualTo("Clínica Ação");
        assertThat(clinica.getSlug()).isEqualTo("clinica-acao");
        verify(agendaService).semearPadrao(clinica);
    }

    @Test
    void deveResolverColisaoDeSlugComSufixo() {
        when(clinicaRepository.existsByNome("Clínica Ação")).thenReturn(false);
        when(clinicaRepository.existsBySlug("clinica-acao")).thenReturn(true);
        when(clinicaRepository.existsBySlug("clinica-acao-2")).thenReturn(false);
        when(clinicaRepository.save(any(Clinica.class))).thenAnswer(i -> i.getArgument(0));

        Clinica clinica = provisioning.criarClinica("Clínica Ação");

        assertThat(clinica.getSlug()).isEqualTo("clinica-acao-2");
    }

    @Test
    void deveUsarFallbackQuandoNomeNaoGeraSlug() {
        when(clinicaRepository.existsByNome("!!!")).thenReturn(false);
        when(clinicaRepository.existsBySlug("clinica")).thenReturn(false);
        when(clinicaRepository.save(any(Clinica.class))).thenAnswer(i -> i.getArgument(0));

        Clinica clinica = provisioning.criarClinica("!!!");

        assertThat(clinica.getSlug()).isEqualTo("clinica");
    }

    @Test
    void naoDeveCriarComNomeDuplicado() {
        when(clinicaRepository.existsByNome("Clínica X")).thenReturn(true);

        assertThatThrownBy(() -> provisioning.criarClinica("Clínica X"))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(clinicaRepository, never()).save(any());
        verify(agendaService, never()).semearPadrao(any());
    }

    @Test
    void slugifyDeveRemoverAcentosETratarPontuacao() {
        assertThat(ClinicaProvisioningService.slugify("Clínica Padrão")).isEqualTo("clinica-padrao");
        assertThat(ClinicaProvisioningService.slugify(" spaces  ")).isEqualTo("spaces");
        assertThat(ClinicaProvisioningService.slugify("---")).isEqualTo("clinica");
    }
}
