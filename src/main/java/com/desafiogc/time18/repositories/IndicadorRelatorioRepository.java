package com.desafiogc.time18.repositories;

import com.desafiogc.time18.entities.IndicadorRelatorio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface IndicadorRelatorioRepository extends JpaRepository<IndicadorRelatorio, UUID> {
    List<IndicadorRelatorio> findByMarcaIdOrderBySecaoAscNomeIndicadorAsc(UUID marcaId);
    List<IndicadorRelatorio> findByMarcaIdAndSecaoOrderByNomeIndicadorAsc(UUID marcaId, String secao);
    void deleteByMarcaIdAndSecao(UUID marcaId, String secao);
}