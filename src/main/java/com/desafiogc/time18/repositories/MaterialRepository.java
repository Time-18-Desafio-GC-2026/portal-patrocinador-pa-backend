package com.desafiogc.time18.repositories;

import com.desafiogc.time18.entities.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MaterialRepository extends JpaRepository<Material, UUID> {
    
    List<Material> findByMarcaIdOrderByAtualizadoEmDesc(UUID marcaId);

    
    List<Material> findByMarcaIdAndAbaDestinoOrderByAtualizadoEmDesc(UUID marcaId, String abaDestino);
}