package com.cliniva.admin;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cliniva.admin.dtos.AdminDtos.AdicionarUsuarioRequestDTO;
import com.cliniva.admin.dtos.AdminDtos.AdicionarUsuarioResponseDTO;
import com.cliniva.admin.dtos.AdminDtos.AtualizarClinicaRequestDTO;
import com.cliniva.admin.dtos.AdminDtos.ClinicaDetalheResponseDTO;
import com.cliniva.admin.dtos.AdminDtos.CriarClinicaRequestDTO;
import com.cliniva.admin.dtos.AdminDtos.CriarClinicaResponseDTO;
import com.cliniva.admin.dtos.AdminDtos.ListarClinicaResponseDTO;
import com.cliniva.admin.dtos.AdminDtos.MetricaClinicaDTO;
import com.cliniva.admin.dtos.AdminDtos.MetricasAdminResponseDTO;
import com.cliniva.admin.dtos.AdminDtos.ResetarSenhaResponseDTO;
import com.cliniva.admin.dtos.AdminDtos.UsuarioResponseDTO;
import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.atendimento.repository.AtendimentoRepository;
import com.cliniva.atendimento.repository.AtendimentoServicoRepository;
import com.cliniva.cliente.ClienteRepository;
import com.cliniva.exception.RecursoDuplicadoException;
import com.cliniva.exception.RecursoNaoEncontradoException;
import com.cliniva.exception.SupabaseIndisponivelException;
import com.cliniva.tenancy.Clinica;
import com.cliniva.tenancy.ClinicaRepository;
import com.cliniva.tenancy.Papel;
import com.cliniva.tenancy.SupabaseUsersService;
import com.cliniva.tenancy.Usuario;
import com.cliniva.tenancy.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ClinicaRepository clinicaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final AtendimentoServicoRepository atendimentoServicoRepository;
    private final SupabaseUsersService supabaseUsers;

    @Transactional(readOnly = true)
    public List<ListarClinicaResponseDTO> listarClinicas() {
        return clinicaRepository.findAll().stream()
                .sorted(Comparator.comparing(Clinica::getNome))
                .map(this::toListagem)
                .toList();
    }

    @Transactional
    public CriarClinicaResponseDTO criarClinica(CriarClinicaRequestDTO request) {
        String nome = request.nome().trim();
        String email = request.emailResponsavel().trim().toLowerCase();
        if (clinicaRepository.existsByNome(nome)) {
            throw new RecursoDuplicadoException("Clínica com esse nome já cadastrada");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new RecursoDuplicadoException("E-mail já cadastrado");
        }

        Clinica clinica = new Clinica();
        clinica.setNome(nome);
        clinicaRepository.save(clinica);

        NovoResponsavel responsavel = novoResponsavel(email, request.nomeResponsavel().trim(), clinica);
        usuarioRepository.save(responsavel.usuario());

        return new CriarClinicaResponseDTO(clinica.getId(), clinica.getNome(), email,
                responsavel.senhaTemporaria());
    }

    @Transactional(readOnly = true)
    public ClinicaDetalheResponseDTO detalharClinica(UUID clinicaId) {
        Clinica clinica = clinicaRepository.findById(clinicaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Clínica não encontrada"));

        List<UsuarioResponseDTO> responsaveis = usuarioRepository
                .findByClinica_IdOrderByNomeAsc(clinicaId).stream()
                .map(this::toUsuario)
                .toList();

        BigDecimal receita = receitaDaClinica(clinicaId);

        return new ClinicaDetalheResponseDTO(clinica.getId(), clinica.getNome(), clinica.isAtiva(),
                clinica.getCriadaEm(),
                clienteRepository.countByClinica(clinica),
                atendimentoRepository.countByClinica(clinica),
                receita,
                responsaveis);
    }

    @Transactional
    public ClinicaDetalheResponseDTO atualizarClinica(UUID clinicaId, AtualizarClinicaRequestDTO request) {
        Clinica clinica = clinicaRepository.findById(clinicaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Clínica não encontrada"));

        if (request.nome() != null && !request.nome().isBlank()) {
            String novoNome = request.nome().trim();
            if (!novoNome.equals(clinica.getNome())
                    && clinicaRepository.existsByNomeAndIdNot(novoNome, clinicaId)) {
                throw new RecursoDuplicadoException("Clínica com esse nome já cadastrada");
            }
            clinica.setNome(novoNome);
        }
        if (request.ativa() != null) {
            clinica.setAtiva(request.ativa());
        }
        clinicaRepository.save(clinica);
        return detalharClinica(clinicaId);
    }

    @Transactional
    public AdicionarUsuarioResponseDTO adicionarResponsavel(UUID clinicaId, AdicionarUsuarioRequestDTO request) {
        Clinica clinica = clinicaRepository.findById(clinicaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Clínica não encontrada"));

        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new RecursoDuplicadoException("E-mail já cadastrado");
        }

        NovoResponsavel responsavel = novoResponsavel(email, request.nome().trim(), clinica);
        usuarioRepository.save(responsavel.usuario());

        return new AdicionarUsuarioResponseDTO(responsavel.usuario().getId(), email,
                responsavel.usuario().getNome(), Papel.OWNER, responsavel.senhaTemporaria());
    }

    @Transactional
    public ResetarSenhaResponseDTO resetarSenha(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        if (usuario.getSupabaseUserId() == null) {
            throw new SupabaseIndisponivelException("Usuário não vinculado ao Supabase");
        }
        String novaSenha = gerarSenhaTemporaria();
        supabaseUsers.definirSenha(usuario.getSupabaseUserId(), novaSenha);
        return new ResetarSenhaResponseDTO(usuario.getEmail(), novaSenha);
    }

    @Transactional(readOnly = true)
    public MetricasAdminResponseDTO metricas() {
        Map<String, Long> clientesPorClinica = agruparContagens(clienteRepository.contarPorClinica());
        Map<String, Long> atendimentosPorClinica = agruparContagens(atendimentoRepository.contarPorClinica());
        Map<String, BigDecimal> receitaPorClinica = agruparSomatorio(
                atendimentoServicoRepository.totalCobradoPorClinica(StatusAtendimento.CONCLUIDO));

        List<MetricaClinicaDTO> porClinica = clinicaRepository.findAll().stream()
                .sorted(Comparator.comparing(Clinica::getNome))
                .map(clinica -> new MetricaClinicaDTO(
                        clinica.getId().toString(),
                        clinica.getNome(),
                        clientesPorClinica.getOrDefault(clinica.getId().toString(), 0L),
                        atendimentosPorClinica.getOrDefault(clinica.getId().toString(), 0L),
                        receitaPorClinica.getOrDefault(clinica.getId().toString(), BigDecimal.ZERO)))
                .toList();

        return new MetricasAdminResponseDTO(
                clinicaRepository.count(),
                clientesPorClinica.values().stream().mapToLong(Long::longValue).sum(),
                atendimentosPorClinica.values().stream().mapToLong(Long::longValue).sum(),
                porClinica);
    }

    private NovoResponsavel novoResponsavel(String email, String nome, Clinica clinica) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNome(nome);
        usuario.setClinica(clinica);
        usuario.setPapel(Papel.OWNER);
        String senhaTemporaria = null;
        if (supabaseUsers.configurada()) {
            senhaTemporaria = gerarSenhaTemporaria();
            var supabaseUser = supabaseUsers.criarUsuario(email, senhaTemporaria);
            usuario.setSupabaseUserId(supabaseUser.id());
        }
        return new NovoResponsavel(usuario, senhaTemporaria);
    }

    private ListarClinicaResponseDTO toListagem(Clinica clinica) {
        return new ListarClinicaResponseDTO(clinica.getId(), clinica.getNome(), clinica.isAtiva(),
                clinica.getCriadaEm(),
                usuarioRepository.findByClinica_IdOrderByNomeAsc(clinica.getId()).size());
    }

    private UsuarioResponseDTO toUsuario(Usuario usuario) {
        return new UsuarioResponseDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPapel(), usuario.isAtivo());
    }

    private BigDecimal receitaDaClinica(UUID clinicaId) {
        return atendimentoServicoRepository
                .totalCobradoPorClinica(StatusAtendimento.CONCLUIDO).stream()
                .filter(linha -> linha[0].toString().equals(clinicaId.toString()))
                .findFirst()
                .map(linha -> (BigDecimal) linha[1])
                .orElse(BigDecimal.ZERO);
    }

    private Map<String, Long> agruparContagens(List<Object[]> linhas) {
        Map<String, Long> mapa = new HashMap<>();
        for (Object[] linha : linhas) {
            mapa.put(linha[0].toString(), ((Number) linha[1]).longValue());
        }
        return mapa;
    }

    private Map<String, BigDecimal> agruparSomatorio(List<Object[]> linhas) {
        Map<String, BigDecimal> mapa = new HashMap<>();
        for (Object[] linha : linhas) {
            mapa.put(linha[0].toString(), (BigDecimal) linha[1]);
        }
        return mapa;
    }

    private String gerarSenhaTemporaria() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private record NovoResponsavel(Usuario usuario, String senhaTemporaria) {
    }
}