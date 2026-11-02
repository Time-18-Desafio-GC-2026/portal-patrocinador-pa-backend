package com.desafiogc.time18.repositories;

import com.desafiogc.time18.entities.Perfil;
import com.desafiogc.time18.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
    Optional<Usuario> findFirstByPerfil(Perfil perfil);
    List<Usuario> findByMarcaIdOrderByCriadoEmDesc(UUID marcaId);

    @Modifying(flushAutomatically = true)
    @Query("update Usuario u set u.senhaHash = :hash, u.atualizadoEm = :agora where u.marca.id = :marcaId and u.precisaTrocarSenha = true")
    int atualizarSenhasPendentes(@Param("marcaId") UUID marcaId, @Param("hash") String hash, @Param("agora") Instant agora);

    @Modifying(flushAutomatically = true)
    @Query("delete from Usuario u where u.marca.id = :marcaId")
    int deletarTodosPorMarcaId(@Param("marcaId") UUID marcaId);
}