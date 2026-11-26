package com.finanzas.repository;

import com.finanzas.model.Billetera;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BilleteraRepository extends JpaRepository<Billetera, Long> {

    List<Billetera> findByUsuarioIdAndActivaTrueOrderByNombreAsc(Long usuarioId);

    /** Evita dos billeteras activas con el mismo nombre (sin distinguir mayúsculas). */
    boolean existsByUsuarioIdAndActivaTrueAndNombreIgnoreCase(Long usuarioId, String nombre);

    /** Filtra por dueño para que un usuario nunca pueda tocar billeteras ajenas. */
    Optional<Billetera> findByIdAndUsuarioId(Long id, Long usuarioId);

    /**
     * Bloquea la fila hasta el fin de la transacción (SELECT ... FOR UPDATE), así dos movimientos
     * concurrentes sobre la misma billetera no pueden leer el mismo saldo a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Billetera b where b.id = :id and b.usuario.id = :usuarioId and b.activa = true")
    Optional<Billetera> bloquearActivaDeUsuario(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    /** Para anular: aunque la billetera esté desactivada, su saldo debe poder corregirse. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Billetera b where b.id = :id")
    Optional<Billetera> bloquearPorId(@Param("id") Long id);
}
