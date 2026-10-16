package com.finanzas.repository;

import com.finanzas.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    List<Transaccion> findByUsuarioIdOrderByFechaDescIdDesc(Long usuarioId);
}
