package com.desafiogc.time18.repositories;

import com.desafiogc.time18.entities.PerguntaPesquisa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PerguntaPesquisaRepository extends JpaRepository<PerguntaPesquisa, UUID> {
    List<PerguntaPesquisa> findByMarcaIdOrderByNumeroPerguntaAsc(UUID marcaId);
    Optional<PerguntaPesquisa> findByMarcaIdAndNumeroPergunta(UUID marcaId, Integer numeroPergunta);
}