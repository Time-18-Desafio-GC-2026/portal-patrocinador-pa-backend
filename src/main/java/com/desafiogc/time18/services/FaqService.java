package com.desafiogc.time18.services;

import com.desafiogc.time18.common.ExcecaoApi;
import com.desafiogc.time18.dtos.FaqDtos.*;
import com.desafiogc.time18.entities.AcaoAuditoria;
import com.desafiogc.time18.entities.DuvidaFaq;
import com.desafiogc.time18.entities.Usuario;
import com.desafiogc.time18.repositories.DuvidaFaqRepository;
import com.desafiogc.time18.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FaqService {

    private final DuvidaFaqRepository faqRepository;
    private final AutenticacaoService autenticacaoService;
    private final AuditoriaService auditoriaService;

    
    @Transactional(readOnly = true)
    public List<FaqResponse> listarFaqMaisAcessadas() {
        return faqRepository.findByPublicaTrueAndStatusOrderByVisualizacoesDesc("RESPONDIDA")
                .stream().map(FaqResponse::de).toList();
    }

    
    @Transactional(readOnly = true)
    public List<FaqResponse> listarAlertasNovasRespostas() {
        return faqRepository.findTop5ByPublicaTrueAndStatusOrderByRespondidoEmDesc("RESPONDIDA")
                .stream().map(FaqResponse::de).toList();
    }

    
    public FaqResponse visualizarArtigoFaq(UUID id, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        DuvidaFaq duvida = faqRepository.findById(id)
                .orElseThrow(() -> ExcecaoApi.naoEncontrado("Artigo do FAQ não encontrado."));

        duvida.setVisualizacoes(duvida.getVisualizacoes() + 1);
        auditoriaService.registrar(ator, AcaoAuditoria.DUVIDA_FAQ_VISUALIZADA, ator.getMarca(),
                "Consultou no FAQ: " + duvida.getPergunta());
        return FaqResponse.de(duvida);
    }

    
    public FaqResponse enviarNovaPergunta(NovaPerguntaFaqRequest request, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);

        DuvidaFaq nova = DuvidaFaq.builder()
                .pergunta(request.pergunta().trim())
                .categoria(request.categoria().trim().toUpperCase())
                .status("PENDENTE")
                .publica(false)
                .visualizacoes(0)
                .autorPerguntaId(ator.getId())
                .autorMarca(ator.getMarca())
                .build();

        faqRepository.save(nova);
        auditoriaService.registrar(ator, AcaoAuditoria.DUVIDA_FAQ_ENVIADA, ator.getMarca(),
                "Nova pergunta enviada ao FAQ: " + nova.getPergunta());
        return FaqResponse.de(nova);
    }

    
    public FaqResponse responderDuvida(UUID id, ResponderFaqRequest request, UsuarioAutenticado logado) {
        Usuario admin = autenticacaoService.buscarUsuarioAtivo(logado);
        DuvidaFaq duvida = faqRepository.findById(id)
                .orElseThrow(() -> ExcecaoApi.naoEncontrado("Dúvida não encontrada."));

        duvida.setResposta(request.resposta().trim());
        duvida.setStatus("RESPONDIDA");
        duvida.setPublica(request.publicarParaTodos());
        duvida.setRespondidoPor(admin.getNome() != null ? admin.getNome() : admin.getEmail());
        duvida.setRespondidoEm(Instant.now());

        auditoriaService.registrar(admin, AcaoAuditoria.DUVIDA_FAQ_RESPONDIDA, duvida.getAutorMarca(),
                "Resposta publicada no FAQ: " + duvida.getPergunta());
        return FaqResponse.de(duvida);
    }

    
    @Transactional(readOnly = true)
    public List<FaqResponse> listarPendentesRbs() {
        return faqRepository.findByStatusOrderByCriadoEmAsc("PENDENTE")
                .stream().map(FaqResponse::de).toList();
    }

    public void excluirDuvida(UUID id, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        DuvidaFaq duvida = faqRepository.findById(id)
                .orElseThrow(() -> ExcecaoApi.naoEncontrado("Dúvida não encontrada."));

        boolean ehAdmin = ator.getPerfil().name().equals("ADMIN") || ator.getPerfil().name().equals("SUPERADMIN");
        boolean ehAutor = duvida.getAutorPerguntaId() != null && duvida.getAutorPerguntaId().equals(ator.getId());

        if (!ehAdmin && !ehAutor) {
            throw ExcecaoApi.acessoNegado("Você não tem permissão para excluir esta pergunta.");
        }

        faqRepository.delete(duvida);
    }
}