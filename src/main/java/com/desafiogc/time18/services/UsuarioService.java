package com.desafiogc.time18.services;

import com.desafiogc.time18.common.ExcecaoApi;
import com.desafiogc.time18.common.ValidadorUtils;
import com.desafiogc.time18.dtos.UsuarioDtos.*;
import com.desafiogc.time18.entities.*;
import com.desafiogc.time18.repositories.MarcaRepository;
import com.desafiogc.time18.repositories.UsuarioRepository;
import com.desafiogc.time18.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final MarcaRepository marcaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AutenticacaoService autenticacaoService;
    private final AuditoriaService auditoriaService;

    
    public UsuarioResponse criarPatrocinador(UUID marcaId, CriarPatrocinadorRequest request, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = marcaRepository.findById(marcaId)
                .orElseThrow(() -> ExcecaoApi.naoEncontrado("Marca não encontrada."));

        if (!marca.isAtiva()) {
            throw ExcecaoApi.conflito("Não é possível adicionar usuários a uma marca desativada.");
        }

        String email = ValidadorUtils.normalizarEmail(request.email());
        garantirEmailDisponivel(email, null);

        Usuario novoPatrocinador = Usuario.builder()
                .email(email)
                .senhaHash(marca.getSenhaPadraoHash())
                .perfil(Perfil.PATROCINADOR)
                .marca(marca)
                .ativo(true)
                .precisaTrocarSenha(true)
                .build();

        usuarioRepository.save(novoPatrocinador);
        auditoriaService.registrar(ator, AcaoAuditoria.USUARIO_CRIADO, marca,
                "E-mail de patrocinador liberado: " + email);
        return UsuarioResponse.de(novoPatrocinador);
    }

    
    public UsuarioResponse criarAdmin(CriarAdminRequest request, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        ValidadorUtils.validarSenha(request.senhaInicial());

        String email = ValidadorUtils.normalizarEmail(request.email());
        garantirEmailDisponivel(email, null);

        Usuario novoAdmin = Usuario.builder()
                .email(email)
                .nome(request.nome() != null && !request.nome().isBlank() ? request.nome().trim() : "Admin RBS")
                .senhaHash(passwordEncoder.encode(request.senhaInicial()))
                .perfil(Perfil.ADMIN)
                .ativo(true)
                .precisaTrocarSenha(true)
                .build();

        usuarioRepository.save(novoAdmin);
        auditoriaService.registrar(ator, AcaoAuditoria.USUARIO_CRIADO, null,
                "Novo Admin RBS criado: " + email);
        return UsuarioResponse.de(novoAdmin);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(UUID marcaId, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);

        if (ator.getPerfil() == Perfil.PATROCINADOR) {
            return usuarioRepository.findByMarcaIdOrderByCriadoEmDesc(ator.getMarca().getId())
                    .stream().map(UsuarioResponse::de).toList();
        }

        List<Usuario> usuarios = (marcaId != null)
                ? usuarioRepository.findByMarcaIdOrderByCriadoEmDesc(marcaId)
                : usuarioRepository.findAll();

        return usuarios.stream().map(UsuarioResponse::de).toList();
    }

    public UsuarioResponse desativar(UUID id, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Usuario alvo = buscarAlvoGerenciavel(ator, id);
        if (!alvo.isAtivo()) {
            throw ExcecaoApi.conflito("O usuário já está desativado.");
        }
        alvo.setAtivo(false);
        auditoriaService.registrar(ator, AcaoAuditoria.USUARIO_DESATIVADO, alvo.getMarca(),
                "Usuário desativado: " + alvo.getEmail());
        return UsuarioResponse.de(alvo);
    }

    public UsuarioResponse reativar(UUID id, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Usuario alvo = buscarAlvoGerenciavel(ator, id);
        if (alvo.isAtivo()) {
            throw ExcecaoApi.conflito("O usuário já está ativo.");
        }
        alvo.setAtivo(true);
        auditoriaService.registrar(ator, AcaoAuditoria.USUARIO_REATIVADO, alvo.getMarca(),
                "Usuário reativado: " + alvo.getEmail());
        return UsuarioResponse.de(alvo);
    }

    public void excluir(UUID id, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Usuario alvo = buscarAlvoGerenciavel(ator, id);
        if (alvo.isAtivo()) {
            throw ExcecaoApi.conflito("Desative o usuário antes de excluí-lo definitivamente.");
        }
        auditoriaService.registrar(ator, AcaoAuditoria.USUARIO_EXCLUIDO, alvo.getMarca(),
                "Usuário excluído: " + alvo.getEmail());
        usuarioRepository.delete(alvo);
    }

    public UsuarioResponse resetarSenha(UUID id, String novaSenhaTemporaria, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Usuario alvo = buscarAlvoGerenciavel(ator, id);

        if (alvo.getPerfil() == Perfil.PATROCINADOR) {
            alvo.setSenhaHash(alvo.getMarca().getSenhaPadraoHash());
        } else {
            ValidadorUtils.validarSenha(novaSenhaTemporaria);
            alvo.setSenhaHash(passwordEncoder.encode(novaSenhaTemporaria));
        }
        alvo.setPrecisaTrocarSenha(true);
        auditoriaService.registrar(ator, AcaoAuditoria.SENHA_RESETADA, alvo.getMarca(),
                "Senha resetada para o usuário: " + alvo.getEmail());
        return UsuarioResponse.de(alvo);
    }

    private Usuario buscarAlvoGerenciavel(Usuario ator, UUID idAlvo) {
        Usuario alvo = usuarioRepository.findById(idAlvo)
                .orElseThrow(() -> ExcecaoApi.naoEncontrado("Usuário não encontrado."));
        if (alvo.getPerfil() == Perfil.SUPERADMIN) {
            throw ExcecaoApi.acessoNegado("O Superadmin não pode ser alterado ou desativado pela API.");
        }
        if (alvo.getPerfil() == Perfil.ADMIN && ator.getPerfil() != Perfil.SUPERADMIN) {
            throw ExcecaoApi.acessoNegado("Apenas o Superadmin pode gerenciar contas de Administradores RBS.");
        }
        if (ator.getId().equals(alvo.getId())) {
            throw ExcecaoApi.acessoNegado("Você não pode bloquear ou excluir a sua própria conta.");
        }
        return alvo;
    }

    private void garantirEmailDisponivel(String email, UUID ignorarId) {
        boolean existe = (ignorarId == null)
                ? usuarioRepository.existsByEmail(email)
                : usuarioRepository.existsByEmailAndIdNot(email, ignorarId);
        if (existe) {
            throw ExcecaoApi.conflito("Este e-mail já está cadastrado na plataforma.");
        }
    }
}