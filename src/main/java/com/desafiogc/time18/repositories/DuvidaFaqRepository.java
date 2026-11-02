package com.desafiogc.time18.repositories;

import com.desafiogc.time18.entities.DuvidaFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DuvidaFaqRepository extends JpaRepository<DuvidaFaq, UUID> {

    // 1. Lista o FAQ para os patrocinadores em ordem das perguntas MAIS ACESSADAS!
    List<DuvidaFaq> findByPublicaTrueAndStatusOrderByVisualizacoesDesc(String status);

    // 2. Alimenta o Alerta Global para todos os patrocinadores: traz as últimas perguntas respondidas pela RBS!
    List<DuvidaFaq> findTop5ByPublicaTrueAndStatusOrderByRespondidoEmDesc(String status);

    // 3. Lista para os Admins da RBS responderem as perguntas que chegaram
    List<DuvidaFaq> findByStatusOrderByCriadoEmAsc(String status);
}