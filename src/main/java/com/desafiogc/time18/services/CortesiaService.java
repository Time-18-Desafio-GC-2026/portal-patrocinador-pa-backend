package com.desafiogc.time18.services;

import com.desafiogc.time18.dtos.CortesiaDtos.CortesiaResponse;
import com.desafiogc.time18.dtos.CortesiaDtos.SalvarCortesiaRequest;
import com.desafiogc.time18.entities.AcaoAuditoria;
import com.desafiogc.time18.entities.CortesiaMarca;
import com.desafiogc.time18.entities.Marca;
import com.desafiogc.time18.entities.Usuario;
import com.desafiogc.time18.repositories.CortesiaMarcaRepository;
import com.desafiogc.time18.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CortesiaService {

    private final CortesiaMarcaRepository cortesiaRepository;
    private final MarcaService marcaService;
    private final AutenticacaoService autenticacaoService;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public CortesiaResponse buscarPorMarca(UUID marcaId, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = marcaService.buscarEValidarAcessoMarca(marcaId, ator);

        return cortesiaRepository.findByMarcaId(marca.getId())
                .map(CortesiaResponse::de)
                .orElseGet(() -> CortesiaResponse.zerado(marca.getId()));
    }

    public CortesiaResponse salvar(UUID marcaId, SalvarCortesiaRequest request, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = marcaService.buscarEValidarAcessoMarca(marcaId, ator);

        CortesiaMarca cortesia = cortesiaRepository.findByMarcaId(marca.getId())
                .orElseGet(() -> CortesiaMarca.builder().marca(marca).build());

        cortesia.setArenaPassaporte(request.arenaPassaporte());
        cortesia.setArenaSexta(request.arenaSexta());
        cortesia.setArenaSabado(request.arenaSabado());
        cortesia.setCamarotePassaporte(request.camarotePassaporte());
        cortesia.setCamaroteSexta(request.camaroteSexta());
        cortesia.setCamaroteSabado(request.camaroteSabado());
        cortesia.setPremiumPassaporte(request.premiumPassaporte());
        cortesia.setPremiumSexta(request.premiumSexta());
        cortesia.setPremiumSabado(request.premiumSabado());
        cortesia.setLoungeInfo(request.loungeInfo() != null ? request.loungeInfo().trim() : "");

        cortesiaRepository.save(cortesia);

        auditoriaService.registrar(
                ator,
                AcaoAuditoria.CORTESIA_ATUALIZADA,
                marca,
                "Quadro de cortesias atualizado para a marca " + marca.getNome()
        );

        return CortesiaResponse.de(cortesia);
    }
}