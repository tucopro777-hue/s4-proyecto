package com.finanzas.repository;

import com.finanzas.model.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    List<Gasto> findByUsuarioIdOrderByFechaDescIdDesc(Long usuarioId);
}
