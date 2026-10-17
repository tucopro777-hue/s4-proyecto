package com.finanzas.repository;

import com.finanzas.model.Presupuesto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    List<Presupuesto> findByUsuarioIdAndAnioAndMesOrderByCategoriaNombreAsc(Long usuarioId, int anio, int mes);

    Optional<Presupuesto> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndCategoriaIdAndAnioAndMes(Long usuarioId, Long categoriaId, int anio, int mes);

    /** Bloquea el presupuesto del periodo para que dos gastos simultáneos no pierdan actualizaciones. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Presupuesto p where p.usuario.id = :usuarioId and p.categoria.id = :categoriaId "
            + "and p.anio = :anio and p.mes = :mes")
    Optional<Presupuesto> bloquearPeriodo(@Param("usuarioId") Long usuarioId,
                                          @Param("categoriaId") Long categoriaId,
                                          @Param("anio") int anio,
                                          @Param("mes") int mes);
}
