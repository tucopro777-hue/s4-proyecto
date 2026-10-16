package com.finanzas.repository;

import com.finanzas.model.Categoria;
import com.finanzas.model.TipoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByUsuarioIdAndActivaTrueOrderByNombreAsc(Long usuarioId);

    List<Categoria> findByUsuarioIdAndTipoAndActivaTrueOrderByNombreAsc(Long usuarioId, TipoCategoria tipo);

    /** Filtra por dueño para que un usuario nunca pueda usar ni tocar categorías ajenas. */
    Optional<Categoria> findByIdAndUsuarioId(Long id, Long usuarioId);

    Optional<Categoria> findByIdAndUsuarioIdAndActivaTrue(Long id, Long usuarioId);

    /** Incluye las inactivas: el nombre sigue ocupado a efectos de la restricción única. */
    boolean existsByUsuarioIdAndNombreIgnoreCase(Long usuarioId, String nombre);
}
