package com.finanzas.repository;

import com.finanzas.model.Billetera;
import com.finanzas.model.TipoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Consultas de solo lectura para el panel principal. Viven en su propio repositorio para no
 * modificar TransaccionRepository ni BilleteraRepository. La entidad base es Billetera solo
 * para cumplir con JpaRepository; las consultas pueden leer cualquier entidad.
 */
public interface DashboardRepository extends JpaRepository<Billetera, Long> {

    /** Suma de los saldos actuales de las billeteras activas del usuario. */
    @Query("select coalesce(sum(b.saldoActual), 0) from Billetera b "
            + "where b.usuario.id = :usuarioId and b.activa = true")
    BigDecimal sumarSaldoActivo(@Param("usuarioId") Long usuarioId);

    /** Total de movimientos no anulados de un tipo (INGRESO o GASTO) dentro de un rango de fechas. */
    @Query("select coalesce(sum(t.monto), 0) from Transaccion t "
            + "where t.usuario.id = :usuarioId and t.tipo = :tipo and t.anulada = false "
            + "and t.fecha between :inicio and :fin")
    BigDecimal sumarMovimientosPeriodo(@Param("usuarioId") Long usuarioId,
                                       @Param("tipo") TipoTransaccion tipo,
                                       @Param("inicio") LocalDate inicio,
                                       @Param("fin") LocalDate fin);

    /** Cuenta los presupuestos del mes cuyo consumo ya supera el límite. */
    @Query("select count(p) from Presupuesto p where p.usuario.id = :usuarioId "
            + "and p.anio = :anio and p.mes = :mes and p.saldoConsumido > p.montoLimite")
    Long contarExcedidos(@Param("usuarioId") Long usuarioId,
                         @Param("anio") int anio,
                         @Param("mes") int mes);
}
