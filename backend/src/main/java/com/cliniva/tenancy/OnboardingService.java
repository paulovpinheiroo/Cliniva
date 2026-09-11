package com.cliniva.tenancy;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        clinicaRepository.save(clinica);

        Usuario responsavel = existente.orElseGet(Usuario::new);
        responsavel.setNome(request.nomeResponsavel().trim());
        responsavel.setEmail(email);
        responsavel.setClinica(clinica);
        responsavel.setPapel(Papel.OWNER);
        vincularSupabase(responsavel, email);
        usuarioRepository.save(responsavel);

        return new CadastroOnboardingResponseDTO(clinica.getId(), clinica.getNome(), responsavel.getId());
    }

    private void vincularSupabase(Usuario responsavel, String email) {
        if (!supabaseUsers.configurada()) {
            return;
        }
        Optional<SupabaseUsersService.UsuarioSupabase> usuario = supabaseUsers.buscarPorEmail(email);
        usuario.ifPresent(u -> responsavel.setSupabaseUserId(u.id()));
    }
}