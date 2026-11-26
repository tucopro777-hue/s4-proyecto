package com.finanzas.config;

import com.finanzas.model.Usuario;
import com.finanzas.repository.BilleteraRepository;
import com.finanzas.repository.TransaccionRepository;
import com.finanzas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** La semilla no usa @Transactional en la prueba: crea datos reales que luego se verifican. */
@SpringBootTest(properties = "app.demo.enabled=true")
class DatosDemoInicializadorTest {

    private static final String EMAIL = "demo@finanzas.test";

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private BilleteraRepository billeteraRepository;
    @Autowired
    private TransaccionRepository transaccionRepository;
    @Autowired
    private DatosDemoInicializador datosDemo;

    @Test
    void creaUsuarioConBilleterasYMovimientos() {
        Usuario demo = usuarioRepository.findByEmail(EMAIL).orElseThrow();

        assertThat(billeteraRepository.findByUsuarioIdAndActivaTrueOrderByNombreAsc(demo.getId())).hasSize(3);
        assertThat(transaccionRepository.findByUsuarioIdAndAnuladaFalseOrderByFechaDescIdDesc(demo.getId())).hasSize(4);
    }

    @Test
    void noDuplicaLosDatosAlEjecutarseDeNuevo() {
        datosDemo.run(null);

        long usuarios = usuarioRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(EMAIL))
                .count();
        assertThat(usuarios).isEqualTo(1);
    }
}
