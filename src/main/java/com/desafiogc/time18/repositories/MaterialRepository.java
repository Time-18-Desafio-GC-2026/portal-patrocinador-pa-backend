package com.desafiogc.time18.repositories;

import com.desafiogc.time18.entities.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MaterialRepository extends JpaRepository<Material, UUID> {
    // Busca todos os arquivos de um patrocinador específico (garante o isolamento!)
    List<Material> findByMarcaIdOrderByAtualizadoEmDesc(UUID marcaId);

    // Filtra os arquivos do patrocinador por Aba do portal (ex: só arquivos da ABA_3_MIDIA)
    List<Material> findByMarcaIdAndAbaDestinoOrderByAtualizadoEmDesc(UUID marcaId, String abaDestino);
}