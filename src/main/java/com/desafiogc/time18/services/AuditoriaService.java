package com.desafiogc.time18.services;

import com.desafiogc.time18.common.ExcecaoApi;
import com.desafiogc.time18.dtos.AuditoriaDtos.LogAuditoriaResponse;
import com.desafiogc.time18.entities.*;
import com.desafiogc.time18.repositories.LogAuditoriaRepository;
import com.desafiogc.time18.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Serviço central de Auditoria e Histórico.
 * Cada chamada a registrar() cria uma NOVA linha imutável na tabela log_auditoria.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final LogAuditoriaRepository logRepository;

    @Transactional
    public void registrar(Usuario ator, AcaoAuditoria acao, Marca marcaAlvo, String detalhe) {
        LogAuditoria log = LogAuditoria.builder()
                .id(gerarIdCurtoUnico())
                .atorId(ator.getId())
                .atorNome(ator.getEmail()) // Garante que a coluna sempre exiba o e-mail do usuário
                .atorPerfil(ator.getPerfil())
                .atorMarcaNome(ator.getMarca() != null ? ator.getMarca().getNome() : null)
                .acao(acao)
                .marcaId(marcaAlvo != null ? marcaAlvo.getId() : null)
                .marcaNome(marcaAlvo != null ? marcaAlvo.getNome() : null)
                .detalhe(detalhe)
                .criadoEm(Instant.now())
                .build();
        logRepository.save(log);
    }

    /**
     * Consulta o histórico de auditoria respeitando o isolamento de marcas:
     * - ADMIN/SUPERADMIN RBS: Pode ver logs de todas as marcas ou filtrar por uma marca específica.
     * - PATROCINADOR: Só vê os logs da PRÓPRIA marca, e o nome individual do funcionário da RBS é mascarado como "Administrador RBS".
     */
    @Transactional(readOnly = true)
    public List<LogAuditoriaResponse> listarHistorico(UsuarioAutenticado logado, UUID filtroMarcaId) {
        boolean ehPatrocinador = logado.perfil() == Perfil.PATROCINADOR;

        if (ehPatrocinador) {
            if (logado.marcaId() == null) {
                throw ExcecaoApi.acessoNegado("Patrocinador sem marca vinculada.");
            }
            // Se o patrocinador tentar passar o ID de outra marca no filtro, bloqueamos!
            if (filtroMarcaId != null && !filtroMarcaId.equals(logado.marcaId())) {
                throw ExcecaoApi.acessoNegado("Acesso negado: você só pode consultar o histórico da sua própria marca.");
            }
            return logRepository.findByMarcaIdOrderByCriadoEmDesc(logado.marcaId())
                    .stream()
                    .map(l -> converterParaDto(l, true))
                    .toList();
        }

        // Se for Admin da RBS:
        List<LogAuditoria> logs = (filtroMarcaId != null)
                ? logRepository.findByMarcaIdOrderByCriadoEmDesc(filtroMarcaId)
                : logRepository.findAllByOrderByCriadoEmDesc();

        return logs.stream().map(l -> converterParaDto(l, false)).toList();
    }

    private LogAuditoriaResponse converterParaDto(LogAuditoria log, boolean mascararAdmin) {
        boolean atorEhAdmin = log.getAtorPerfil() == Perfil.ADMIN || log.getAtorPerfil() == Perfil.SUPERADMIN;
        String nomeExibido = (mascararAdmin && atorEhAdmin) ? "Equipe RBS" : log.getAtorNome();
        Perfil perfilExibido = (mascararAdmin && atorEhAdmin) ? Perfil.ADMIN : log.getAtorPerfil();

        return new LogAuditoriaResponse(
                log.getId(),
                nomeExibido,
                perfilExibido,
                log.getAtorMarcaNome(),
                log.getAcao(),
                log.getMarcaId(),
                log.getMarcaNome(),
                log.getDetalhe(),
                log.getCriadoEm()
        );
    }

    private String gerarIdCurtoUnico() {
        for (int tentativa = 0; tentativa < 20; tentativa++) {
            byte[] bytes = new byte[4];
            RANDOM.nextBytes(bytes);
            String id = HexFormat.of().formatHex(bytes).substring(0, 7);
            if (!logRepository.existsById(id)) {
                return id;
            }
        }
        throw new IllegalStateException("Não foi possível gerar ID de auditoria.");
    }
}