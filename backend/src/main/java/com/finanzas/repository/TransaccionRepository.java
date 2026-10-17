package com.finanzas.repository;

import com.finanzas.model.TipoTransaccion;
import com.finanzas.model.Transaccion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    List<Transaccion> findByUsuarioIdAndAnuladaFalseOrderByFechaDescIdDesc(Long usuarioId);

    /**
     * Bloquea el movimiento antes de anularlo: si dos peticiones intentan anularlo a la vez,
     * la segunda espera y ve la anulación ya hecha, así el saldo no se revierte dos veces.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transaccion t where t.id = :id and t.usuario.id = :usuarioId")
    Optional<Transaccion> bloquearDeUsuario(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    /** Total de movimientos no anulados de un tipo, categoría y rango de fechas. */
    @Query("select coalesce(sum(t.monto), 0) from Transaccion t "
            + "where t.usuario.id = :usuarioId and t.categoria.id = :categoriaId and t.tipo = :tipo "
            + "and t.anulada = false and t.fecha between :inicio and :fin")
    BigDecimal sumarMovimientos(@Param("usuarioId") Long usuarioId,
                                @Param("categoriaId") Long categoriaId,
                                @Param("tipo") TipoTransaccion tipo,
                                @Param("inicio") LocalDate inicio,
                                @Param("fin") LocalDate fin);
}
