package com.cliniva.tenancy;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cliniva.agenda.AgendaService;
import com.cliniva.exception.RecursoDuplicadoException;
import com.cliniva.tenancy.dtos.TenancyDtos.CadastroOnboardingRequestDTO;
import com.cliniva.tenancy.dtos.TenancyDtos.CadastroOnboardingResponseDTO;

import lombok.RequiredArgsConstructor;

/**
 * Auto-cadastro (público): cria a clínica e o responsável OWNER. Quando o
 * Supabase está configurado, o usuário que já se registrou no Supabase
 * (frontend) é vinculado pelo e-mail.
 */
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final ClinicaRepository clinicaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SupabaseUsersService supabaseUsers;
    private final AgendaService agendaService;

    @Transactional
    public CadastroOnboardingResponseDTO cadastrarClinica(CadastroOnboardingRequestDTO request) {
        String nomeClinica = request.nomeClinica().trim();
        String email = request.email().trim().toLowerCase();

        Optional<Usuario> existente = usuarioRepository.findByEmail(email);
        if (existente.isPresent()) {
            Usuario jaCadastrado = existente.get();
            vincularSupabase(jaCadastrado, email);
            if (jaCadastrado.getClinica() != null) {
                return new CadastroOnboardingResponseDTO(jaCadastrado.getClinica().getId(),
                        jaCadastrado.getClinica().getNome(), jaCadastrado.getId());
            }
        }

        if (clinicaRepository.existsByNome(nomeClinica)) {
            throw new RecursoDuplicadoException("Clínica com esse nome já cadastrada");
        }

        Clinica clinica = new Clinica();
        clinica.setNome(nomeClinica);
        clinica.setSlug(gerarSlugUnico(nomeClinica));
        clinicaRepository.save(clinica);
        agendaService.semearPadrao(clinica);

        Usuario responsavel = existente.orElseGet(Usuario::new);
        responsavel.setNome(request.nomeResponsavel().trim());
        responsavel.setEmail(email);
        responsavel.setClinica(clinica);
        responsavel.setPapel(Papel.OWNER);
        vincularSupabase(responsavel, email);
        usuarioRepository.save(responsavel);

        return new CadastroOnboardingResponseDTO(clinica.getId(), clinica.getNome(), responsavel.getId());
    }

    private String gerarSlugUnico(String nome) {
        String base = slugify(nome);
        String slug = base;
        int sufixo = 2;
        while (clinicaRepository.existsBySlug(slug)) {
            slug = base + "-" + sufixo++;
        }
        return slug;
    }

    private String slugify(String nome) {
        String semAcentos = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = semAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "clinica" : slug;
    }

    private void vincularSupabase(Usuario responsavel, String email) {
        if (!supabaseUsers.configurada()) {
            return;
        }
        Optional<SupabaseUsersService.UsuarioSupabase> usuario = supabaseUsers.buscarPorEmail(email);
        usuario.ifPresent(u -> responsavel.setSupabaseUserId(u.id()));
    }
}