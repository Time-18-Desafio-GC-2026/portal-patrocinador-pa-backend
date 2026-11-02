package com.desafiogc.time18.repositories;

import com.desafiogc.time18.entities.Marca;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MarcaRepository extends JpaRepository<Marca, UUID> {
    boolean existsByCnpj(String cnpj);
    boolean existsByCnpjAndIdNot(String cnpj, UUID id);
    List<Marca> findByAtivaOrderByNomeAsc(boolean ativa);
}