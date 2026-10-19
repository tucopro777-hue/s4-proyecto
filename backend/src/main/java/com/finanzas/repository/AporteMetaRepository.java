package com.finanzas.repository;

import com.finanzas.model.AporteMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AporteMetaRepository extends JpaRepository<AporteMeta, Long> {

    /** Aportes automáticos que originó un ingreso; se revierten al anularlo. */
    List<AporteMeta> findByTransaccionId(Long transaccionId);

    /** Aportes que recibió una meta; se devuelven a sus billeteras si la meta se elimina. */
    List<AporteMeta> findByMetaId(Long metaId);
}
