package com.desafiogc.time18.repositories;

import com.desafiogc.time18.entities.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, String> {
    List<LogAuditoria> findAllByOrderByCriadoEmDesc();
    List<LogAuditoria> findByMarcaIdOrderByCriadoEmDesc(UUID marcaId);
}