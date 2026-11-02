package com.desafiogc.time18.repositories;

import com.desafiogc.time18.entities.CortesiaMarca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CortesiaMarcaRepository extends JpaRepository<CortesiaMarca, UUID> {
    Optional<CortesiaMarca> findByMarcaId(UUID marcaId);
}