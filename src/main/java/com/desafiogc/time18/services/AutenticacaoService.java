package com.desafiogc.time18.services;

import com.desafiogc.time18.common.ExcecaoApi;
import com.desafiogc.time18.common.ValidadorUtils;
import com.desafiogc.time18.dtos.AutenticacaoDtos.*;
import com.desafiogc.time18.dtos.UsuarioDtos.UsuarioResponse;
import com.desafiogc.time18.entities.AcaoAuditoria;
import com.desafiogc.time18.entities.Perfil;
import com.desafiogc.time18.entities.Usuario;
import com.desafiogc.time18.repositories.UsuarioRepository;
import com.desafiogc.time18.security.TokenService;
import com.desafiogc.time18.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public SessaoResponse login(LoginRequest request) {
        String email = ValidadorUtils.normalizarEmail(request.email());

        
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> ExcecaoApi.naoAutenticado("E-mail ou senha inválidos (ou e-mail não autorizado pela RBS)."));

        
        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw ExcecaoApi.naoAutenticado("E-mail ou senha inválidos.");
        }

        
        validarUsuarioAtivo(usuario);

        
        usuario.setPrimeiroAcessoEm(Instant.now());

        
        auditoriaService.registrar(usuario, AcaoAuditoria.LOGIN_REALIZADO, usuario.getMarca(), "Acesso realizado na plataforma");

        
        String token = tokenService.gerarToken(usuario);
        return new SessaoResponse(token, "Bearer", UsuarioResponse.de(usuario));
    }

    @Transactional
    public SessaoResponse concluirPrimeiroAcesso(UsuarioAutenticado logado, PrimeiroAcessoRequest request) {
        Usuario usuario = buscarUsuarioAtivo(logado);

        if (usuario.getPerfil() == Perfil.SUPERADMIN) {
            throw ExcecaoApi.acessoNegado("O Superadmin não possui fluxo de primeiro acesso.");
        }
        if (!usuario.isPrecisaTrocarSenha()) {
            throw ExcecaoApi.conflito("O primeiro acesso já foi concluído anteriormente.");
        }

        ValidadorUtils.validarSenha(request.novaSenha());
        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenhaHash())) {
            throw ExcecaoApi.dadosInvalidos("A sua nova senha pessoal deve ser diferente da senha padrão inicial.");
        }

        usuario.setNome(request.nome().trim());
        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuario.setPrecisaTrocarSenha(false);
        if (usuario.getPrimeiroAcessoEm() == null) {
            usuario.setPrimeiroAcessoEm(Instant.now());
        }

        auditoriaService.registrar(usuario, AcaoAuditoria.PRIMEIRO_ACESSO_CONCLUIDO, usuario.getMarca(),
                "Definiu nome (" + usuario.getNome() + ") e senha pessoal");

        String novoToken = tokenService.gerarToken(usuario);
        return new SessaoResponse(novoToken, "Bearer", UsuarioResponse.de(usuario));
    }

    @Transactional
    public SessaoResponse alterarSenha(UsuarioAutenticado logado, AlterarSenhaRequest request) {
        Usuario usuario = buscarUsuarioAtivo(logado);

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            throw ExcecaoApi.dadosInvalidos("A senha atual informada está incorreta.");
        }
        ValidadorUtils.validarSenha(request.novaSenha());
        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenhaHash())) {
            throw ExcecaoApi.dadosInvalidos("A nova senha deve ser diferente da senha atual.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuario.setPrecisaTrocarSenha(false);

        auditoriaService.registrar(usuario, AcaoAuditoria.SENHA_ALTERADA, usuario.getMarca(), "Alteração voluntária de senha");

        String novoToken = tokenService.gerarToken(usuario);
        return new SessaoResponse(novoToken, "Bearer", UsuarioResponse.de(usuario));
    }

    @Transactional
    public UsuarioResponse atualizarNome(UsuarioAutenticado logado, AtualizarNomeRequest request) {
        Usuario usuario = buscarUsuarioAtivo(logado);
        String nomeAnterior = usuario.getNome();
        usuario.setNome(request.nome().trim());

        auditoriaService.registrar(
                usuario,
                AcaoAuditoria.USUARIO_ATUALIZADO,
                usuario.getMarca(),
                "Nome de usuário alterado de \"" + (nomeAnterior != null ? nomeAnterior : usuario.getEmail()) + "\" para \"" + usuario.getNome() + "\""
        );

        return UsuarioResponse.de(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse meusDados(UsuarioAutenticado logado) {
        return UsuarioResponse.de(buscarUsuarioAtivo(logado));
    }

    public Usuario buscarUsuarioAtivo(UsuarioAutenticado logado) {
        if (logado == null) {
            throw ExcecaoApi.naoAutenticado("Autenticação necessária.");
        }
        Usuario usuario = usuarioRepository.findById(logado.id())
                .orElseThrow(() -> ExcecaoApi.naoAutenticado("Usuário não encontrado."));
        validarUsuarioAtivo(usuario);
        return usuario;
    }

    private void validarUsuarioAtivo(Usuario usuario) {
        if (!usuario.isAtivo()) {
            throw ExcecaoApi.acessoNegado("Sua conta de usuário está desativada.");
        }
        if (usuario.getMarca() != null && !usuario.getMarca().isAtiva()) {
            throw ExcecaoApi.acessoNegado("A marca vinculada à sua conta está desativada.");
        }
    }
}