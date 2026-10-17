package com.finanzas.repository;

import com.finanzas.model.MetaAhorro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MetaAhorroRepository extends JpaRepository<MetaAhorro, Long> {

    List<MetaAhorro> findByUsuarioIdOrderByNombreAsc(Long usuarioId);

    Optional<MetaAhorro> findByIdAndUsuarioId(Long id, Long usuarioId);

    /** Bloquea la meta para que dos aportes simultáneos no pierdan ninguno. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MetaAhorro m where m.id = :id and m.usuario.id = :usuarioId")
    Optional<MetaAhorro> bloquearDeUsuario(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
