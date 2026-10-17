package com.finanzas.repository;

import com.finanzas.model.Traspaso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TraspasoRepository extends JpaRepository<Traspaso, Long> {

    List<Traspaso> findByUsuarioIdOrderByFechaDescIdDesc(Long usuarioId);
}
