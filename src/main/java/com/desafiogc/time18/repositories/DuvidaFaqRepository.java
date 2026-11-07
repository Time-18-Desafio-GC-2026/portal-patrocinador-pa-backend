package com.desafiogc.time18.repositories;

import com.desafiogc.time18.entities.DuvidaFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DuvidaFaqRepository extends JpaRepository<DuvidaFaq, UUID> {

    
    List<DuvidaFaq> findByPublicaTrueAndStatusOrderByVisualizacoesDesc(String status);

    
    List<DuvidaFaq> findTop5ByPublicaTrueAndStatusOrderByRespondidoEmDesc(String status);

    
    List<DuvidaFaq> findByStatusOrderByCriadoEmAsc(String status);
}