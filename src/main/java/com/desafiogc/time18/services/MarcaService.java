package com.desafiogc.time18.services;

import com.desafiogc.time18.common.ExcecaoApi;
import com.desafiogc.time18.common.ValidadorUtils;
import com.desafiogc.time18.dtos.MarcaDtos.*;
import com.desafiogc.time18.entities.*;
import com.desafiogc.time18.repositories.MarcaRepository;
import com.desafiogc.time18.repositories.UsuarioRepository;
import com.desafiogc.time18.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MarcaService {

    private final MarcaRepository marcaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AutenticacaoService autenticacaoService;
    private final AuditoriaService auditoriaService;

    public MarcaResponse criar(CriarMarcaRequest request, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        String cnpjLimpo = validarCnpjObrigatorio(request.cnpj());

        if (marcaRepository.existsByCnpj(cnpjLimpo)) {
            throw ExcecaoApi.conflito("Já existe uma marca cadastrada com este CNPJ.");
        }
        ValidadorUtils.validarSenha(request.senhaPadrao());

        Marca marca = Marca.builder()
                .nome(request.nome().trim())
                .cnpj(cnpjLimpo)
                .cota(request.cota())
                .evento("Planeta Atlântida 2026")
                .senhaPadraoHash(passwordEncoder.encode(request.senhaPadrao()))
                .ativa(true)
                .build();

        marcaRepository.save(marca);
        auditoriaService.registrar(ator, AcaoAuditoria.MARCA_CRIADA, marca,
                "Marca criada na cota " + marca.getCota());
        return MarcaResponse.de(marca);
    }

    
    @Transactional(readOnly = true)
    public List<MarcaResponse> listar(Boolean ativa, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);

        if (ator.getPerfil() == Perfil.PATROCINADOR) {
            return List.of(MarcaResponse.de(ator.getMarca()));
        }

        List<Marca> marcas = (ativa == null)
                ? marcaRepository.findAll()
                : marcaRepository.findByAtivaOrderByNomeAsc(ativa);
        return marcas.stream().map(MarcaResponse::de).toList();
    }

    
    @Transactional(readOnly = true)
    public MarcaResponse buscarPorId(UUID id, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        validarAcessoAMarca(ator, id);
        return MarcaResponse.de(buscarEntidade(id));
    }

    public MarcaResponse atualizar(UUID id, AtualizarMarcaRequest request, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = buscarEntidade(id);
        String cnpjLimpo = validarCnpjObrigatorio(request.cnpj());

        if (marcaRepository.existsByCnpjAndIdNot(cnpjLimpo, id)) {
            throw ExcecaoApi.conflito("Já existe outra marca com este CNPJ.");
        }

        marca.setNome(request.nome().trim());
        marca.setCnpj(cnpjLimpo);
        marca.setCota(request.cota());

        auditoriaService.registrar(ator, AcaoAuditoria.MARCA_ATUALIZADA, marca,
                "Dados/Cota atualizados para " + marca.getCota());
        return MarcaResponse.de(marca);
    }

    public MarcaResponse alterarSenhaPadrao(UUID id, String novaSenha, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = buscarEntidade(id);
        ValidadorUtils.validarSenha(novaSenha);

        String novoHash = passwordEncoder.encode(novaSenha);
        marca.setSenhaPadraoHash(novoHash);
        usuarioRepository.atualizarSenhasPendentes(marca.getId(), novoHash, Instant.now());

        auditoriaService.registrar(ator, AcaoAuditoria.MARCA_SENHA_PADRAO_ALTERADA, marca,
                "Senha padrão da marca alterada");
        return MarcaResponse.de(marca);
    }

    public MarcaResponse desativar(UUID id, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = buscarEntidade(id);
        if (!marca.isAtiva()) {
            throw ExcecaoApi.conflito("A marca já está desativada.");
        }
        marca.setAtiva(false);
        auditoriaService.registrar(ator, AcaoAuditoria.MARCA_DESATIVADA, marca, "Marca desativada");
        return MarcaResponse.de(marca);
    }

    public MarcaResponse reativar(UUID id, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = buscarEntidade(id);
        if (marca.isAtiva()) {
            throw ExcecaoApi.conflito("A marca já está ativa.");
        }
        marca.setAtiva(true);
        auditoriaService.registrar(ator, AcaoAuditoria.MARCA_REATIVADA, marca, "Marca reativada");
        return MarcaResponse.de(marca);
    }

    public void excluir(UUID id, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = buscarEntidade(id);
        if (marca.isAtiva()) {
            throw ExcecaoApi.conflito("Por segurança, desative a marca antes de excluí-la definitivamente.");
        }
        auditoriaService.registrar(ator, AcaoAuditoria.MARCA_EXCLUIDA, marca, "Marca excluída definitivamente");
        usuarioRepository.deletarTodosPorMarcaId(marca.getId());
        marcaRepository.delete(marca);
    }

    
    public Marca buscarEValidarAcessoMarca(UUID marcaId, Usuario ator) {
        validarAcessoAMarca(ator, marcaId);
        return buscarEntidade(marcaId);
    }

    public void validarAcessoAMarca(Usuario ator, UUID marcaIdAlvo) {
        if (ator.getPerfil() == Perfil.PATROCINADOR) {
            if (ator.getMarca() == null || !ator.getMarca().getId().equals(marcaIdAlvo)) {
                throw ExcecaoApi.acessoNegado("Acesso negado: você só tem permissão para acessar registros da sua própria marca.");
            }
        }
    }

    private Marca buscarEntidade(UUID id) {
        return marcaRepository.findById(id)
                .orElseThrow(() -> ExcecaoApi.naoEncontrado("Marca não encontrada."));
    }

    private String validarCnpjObrigatorio(String cnpj) {
        String digitos = ValidadorUtils.apenasDigitos(cnpj);
        if (!ValidadorUtils.cnpjValido(digitos)) {
            throw ExcecaoApi.dadosInvalidos("CNPJ inválido (verifique os 14 dígitos verificadores).");
        }
        return digitos;
    }
}