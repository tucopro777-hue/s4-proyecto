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
     * Consulta NUEVA para el listado con filtros avanzados (Persona 3).
     *
     * IMPORTANTE sobre nulos en PostgreSQL + JPQL:
     * Se usa el patrón "(:param IS NULL OR columna = :param)" y no COALESCE sobre la columna.
     * Con COALESCE(:desde, t.fecha) el optimizador de PostgreSQL puede generar un plan que
     * ignore los índices y, además, si :param fuera de tipo distinto (ej. null sin tipar en
     * Hibernate 6 contra PostgreSQL) se produce el error
     * "could not determine data type of parameter". Con la forma "IS NULL OR ..." Hibernate
     * tipa cada parámetro explícitamente (LocalDate, Long, enum) y PostgreSQL no falla.
     *
     * Los filtros se combinan entre sí (AND) y SIEMPRE se excluyen las anuladas.
     * Sin filtros, equivale exactamente al listado predeterminado.
     */
    @Query("select t from Transaccion t "
            + "where t.usuario.id = :usuarioId "
            + "and t.anulada = false "
            + "and (:desde IS NULL OR t.fecha >= :desde) "
            + "and (:hasta IS NULL OR t.fecha <= :hasta) "
            + "and (:categoriaId IS NULL OR t.categoria.id = :categoriaId) "
            + "and (:tipo IS NULL OR t.tipo = :tipo) "
            + "order by t.fecha desc, t.id desc")
    List<Transaccion> buscarConFiltros(@Param("usuarioId") Long usuarioId,
                                       @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta,
                                       @Param("categoriaId") Long categoriaId,
                                       @Param("tipo") TipoTransaccion tipo);

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
