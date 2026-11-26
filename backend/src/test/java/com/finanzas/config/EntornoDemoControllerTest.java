package com.finanzas.config;

import com.finanzas.controller.EntornoDemoController;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import javax.sql.DataSource;
import java.sql.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class EntornoDemoControllerTest {
    private EntornoDemoController preparar(String url, boolean prod) throws Exception {
        var ds=mock(DataSource.class);var c=mock(Connection.class);var meta=mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);when(c.getMetaData()).thenReturn(meta);when(meta.getURL()).thenReturn(url);when(c.getCatalog()).thenReturn("finanzas");
        var env=new MockEnvironment().withProperty("app.demo.enabled","true");
        env.setActiveProfiles(prod?new String[]{"demo-local","production"}:new String[]{"demo-local"});
        return new EntornoDemoController(ds,env);
    }
    @Test void confirmaSoloLaBaseLocalEsperada() throws Exception {
        assertThat(preparar("jdbc:postgresql://localhost:5432/finanzas",false)
                .entorno(new TestingAuthenticationToken("demo@finanzas.test",""))).containsEntry("cargaPermitida",true);
    }
    @Test void rechazaHostRemotoYOtraBase() throws Exception {
        for(String url:new String[]{"jdbc:postgresql://servidor:5432/finanzas","jdbc:postgresql://localhost:5432/produccion"}) {
            var c=preparar(url,false);
            assertThatThrownBy(()->c.entorno(new TestingAuthenticationToken("demo@finanzas.test",""))).isInstanceOf(ResponseStatusException.class);
        }
    }
    @Test void rechazaPerfilProductivo() throws Exception {
        var c=preparar("jdbc:postgresql://localhost:5432/finanzas",true);
        assertThatThrownBy(()->c.entorno(new TestingAuthenticationToken("demo@finanzas.test",""))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void rechazaOtraCuenta() throws Exception {
        var c=preparar("jdbc:postgresql://localhost:5432/finanzas",false);
        assertThatThrownBy(()->c.entorno(new TestingAuthenticationToken("otra@finanzas.test",""))).isInstanceOf(ResponseStatusException.class);
    }
}
