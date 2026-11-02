package com.desafiogc.time18.controllers;

import com.desafiogc.time18.dtos.FaqDtos.*;
import com.desafiogc.time18.security.UsuarioAutenticado;
import com.desafiogc.time18.services.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/faq")
@RequiredArgsConstructor
@Tag(name = "5. FAQ e Alertas de Resposta", description = "Perguntas frequentes ordenadas pelas mais acessadas e alertas de novas respostas da RBS")
public class FaqController {

    private final FaqService faqService;

    @GetMapping
    @Operation(summary = "Lista todas as respostas públicas do FAQ em ordem das MAIS ACESSADAS pelos usuários")
    public List<FaqResponse> listarMaisAcessadas() {
        return faqService.listarFaqMaisAcessadas();
    }

    @GetMapping("/alertas")
    @Operation(summary = "Mural de Alertas: exibe avisos sempre que uma nova resposta é publicada no FAQ pela RBS")
    public List<FaqResponse> listarAlertasRecentes() {
        return faqService.listarAlertasNovasRespostas();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Abre uma pergunta/resposta do FAQ (soma +1 visualização no ranking de acessos)")
    public FaqResponse visualizar(@PathVariable UUID id,
                                  @AuthenticationPrincipal UsuarioAutenticado logado) {
        return faqService.visualizarArtigoFaq(id, logado);
    }

    @PostMapping("/perguntas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Patrocinador envia uma nova dúvida para o time da RBS responder")
    public FaqResponse enviarPergunta(@Valid @RequestBody NovaPerguntaFaqRequest request,
                                      @AuthenticationPrincipal UsuarioAutenticado logado) {
        return faqService.enviarNovaPergunta(request, logado);
    }

    @GetMapping("/pendentes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista dúvidas enviadas pelos patrocinadores que aguardam resposta")
    public List<FaqResponse> listarPendentes() {
        return faqService.listarPendentesRbs();
    }

    @PutMapping("/{id}/responder")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Redige a resposta e publica no FAQ gerando alerta para todos os patrocinadores")
    public FaqResponse responder(@PathVariable UUID id,
                                 @Valid @RequestBody ResponderFaqRequest request,
                                 @AuthenticationPrincipal UsuarioAutenticado logado) {
        return faqService.responderDuvida(id, request, logado);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove uma pergunta do FAQ (Admin ou autor da pergunta)")
    public void excluir(@PathVariable UUID id,
                        @AuthenticationPrincipal UsuarioAutenticado logado) {
        faqService.excluirDuvida(id, logado);
    }
}