package com.finanzas.service;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pruebas HTTP de todos los módulos sobre H2. Cada caso revierte todos sus registros. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SistemaCompletoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private final String email="qa.sistema@finanzas.test";
    private JsonNode respuesta(String texto) throws Exception { return json.readTree(texto); }
    private JsonNode registrar(String correo) throws Exception {
        return respuesta(mvc.perform(post("/api/auth/registro").contentType("application/json")
                .content(json.writeValueAsString(Map.of("nombre","QA ficticio","email",correo,"password","Prueba1234"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    private JsonNode crear(String ruta,Object datos) throws Exception {
        return respuesta(mvc.perform(post("/api/"+ruta).with(user(email)).contentType("application/json")
                .content(json.writeValueAsString(datos))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    private JsonNode leer(String ruta) throws Exception {
        return respuesta(mvc.perform(get("/api/"+ruta).with(user(email))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private long billetera(String nombre,int saldo,String divisa) throws Exception {
        return crear("billeteras",Map.of("nombre",nombre,"saldoInicial",saldo,"divisa",divisa)).get("id").asLong();
    }
    private long categoria(String nombre,String tipo) throws Exception {
        return crear("categorias",Map.of("nombre",nombre,"tipo",tipo,"color","#FF8800","icono","food")).get("id").asLong();
    }
    private Map<String,Object> movimiento(long b,long c,String tipo,int monto,LocalDate fecha) {
        return Map.of("billeteraId",b,"categoriaId",c,"tipo",tipo,"monto",monto,"fecha",fecha.toString(),"descripcion","QA ficticio");
    }

    @Test void consultaIngresosYGastosDeMesesAnterioresConFiltrosOpcionales() throws Exception {
        registrar(email);
        long b=billetera("Historial",1000,"BOB");
        long i=categoria("Ingreso histórico","INGRESO"),g=categoria("Gasto histórico","GASTO");
        var mes=YearMonth.now().minusMonths(2);
        crear("transacciones",movimiento(b,i,"INGRESO",200,mes.atDay(1)));
        crear("transacciones",movimiento(b,g,"GASTO",50,mes.atEndOfMonth()));
        crear("transacciones",movimiento(b,i,"INGRESO",100,mes.plusMonths(1).atDay(1)));
        String rango="transacciones?desde="+mes.atDay(1)+"&hasta="+mes.atEndOfMonth();
        assertThat(leer("transacciones").size()).isEqualTo(3);
        var historial=leer(rango);
        assertThat(historial.size()).isEqualTo(2);
        assertThat(historial.get(0).get("fecha").asText()).isEqualTo(mes.atEndOfMonth().toString());
        assertThat(leer(rango+"&tipo=INGRESO").size()).isEqualTo(1);
        assertThat(leer(rango+"&categoriaId="+g).get(0).get("tipo").asText()).isEqualTo("GASTO");
        assertThat(leer("transacciones?tipo=INGRESO").size()).isEqualTo(2);
        assertThat(leer("transacciones?hasta="+mes.atEndOfMonth()).size()).isEqualTo(2);
        assertThat(leer("transacciones?desde="+mes.plusMonths(1).atDay(1)).size()).isEqualTo(1);
    }

    @Test void registroLoginYProteccionJwt() throws Exception {
        var auth=registrar(email);
        mvc.perform(get("/api/billeteras")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/billeteras").header("Authorization","Bearer "+auth.get("token").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email",email,"password","incorrecta")))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email",email,"password","Prueba1234")))).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test void recorridoDeIngresosGastosPresupuestosMetasTraspasosYAnulacion() throws Exception {
        registrar(email);
        long banco=billetera("Banco",1000,"BOB"),caja=billetera("Caja",100,"BOB");
        long ingreso=categoria("Sueldo","INGRESO"),gasto=categoria("Alimentación","GASTO");
        var hoy=LocalDate.now();
        long presupuesto=crear("presupuestos",Map.of("categoriaId",gasto,"montoLimite",200,"anio",hoy.getYear(),"mes",hoy.getMonthValue())).get("id").asLong();
        var datosMeta=Map.of("nombre","Computadora QA","montoObjetivo",3000,"porcentajeAutomatico",10,"fechaLimite",hoy.plusMonths(8).toString());
        long meta=crear("metas",datosMeta).get("id").asLong();
        long entrada=crear("transacciones",movimiento(banco,ingreso,"INGRESO",1000,hoy)).get("id").asLong();
        long salida=crear("transacciones",movimiento(banco,gasto,"GASTO",250,hoy)).get("id").asLong();
        assertThat(leer("metas").get(0).get("montoActual").decimalValue()).isEqualByComparingTo("100");
        var p=leer("presupuestos?anio="+hoy.getYear()+"&mes="+hoy.getMonthValue()).get(0);
        assertThat(p.get("saldoConsumido").asInt()).isEqualTo(250);assertThat(p.get("nivel").asText()).isEqualTo("EXCEDIDO");
        var totalAntes=leer("dashboard/resumen").get("saldoTotal").decimalValue();
        crear("traspasos",Map.of("billeteraOrigenId",banco,"billeteraDestinoId",caja,"monto",100,"fecha",hoy.toString()));
        assertThat(leer("dashboard/resumen").get("saldoTotal").decimalValue()).isEqualByComparingTo(totalAntes);
        mvc.perform(post("/api/metas/"+meta+"/aportes").with(user(email)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("billeteraId",banco,"monto",50)))).andExpect(status().isOk());
        mvc.perform(put("/api/metas/"+meta).with(user(email)).contentType("application/json").content(json.writeValueAsString(datosMeta)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.montoActual").value(150));
        mvc.perform(delete("/api/transacciones/"+entrada).with(user(email))).andExpect(status().isNoContent());
        mvc.perform(delete("/api/transacciones/"+salida).with(user(email))).andExpect(status().isNoContent());
        assertThat(leer("metas").get(0).get("montoActual").asInt()).isEqualTo(50);
        assertThat(leer("presupuestos?anio="+hoy.getYear()+"&mes="+hoy.getMonthValue()).get(0).get("saldoConsumido").asInt()).isZero();
        mvc.perform(put("/api/presupuestos/"+presupuesto).with(user(email)).contentType("application/json").content("{\"montoLimite\":500}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/metas/"+meta).with(user(email))).andExpect(status().isNoContent());
        var resumen=leer("dashboard/resumen");
        assertThat(resumen.get("saldoTotal").asInt()).isEqualTo(1100);
        assertThat(resumen.get("ingresosMes").asInt()).isZero(); assertThat(resumen.get("gastosMes").asInt()).isZero();
        assertThat(leer("transacciones").size()).isZero();assertThat(leer("traspasos").size()).isEqualTo(1);
        mvc.perform(delete("/api/presupuestos/"+presupuesto).with(user(email))).andExpect(status().isNoContent());
        mvc.perform(delete("/api/categorias/"+gasto).with(user(email))).andExpect(status().isNoContent());
        mvc.perform(delete("/api/billeteras/"+caja).with(user(email)).contentType("application/json").content("{\"password\":\"Prueba1234\"}"))
                .andExpect(status().isNoContent());
        assertThat(leer("billeteras").size()).isEqualTo(1);
    }

    @Test void rechazaGastosSinSaldoTransferenciasInvalidasYAccesoAjenoSinCambios() throws Exception {
        registrar(email);registrar("qa.otro@finanzas.test");
        long b=billetera("Banco",100,"BOB"),usd=billetera("Dólares",100,"USD");
        long gasto=categoria("Comida","GASTO");
        mvc.perform(post("/api/transacciones").with(user(email)).contentType("application/json")
                .content(json.writeValueAsString(movimiento(b,gasto,"GASTO",101,LocalDate.now())))).andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/traspasos").with(user(email)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("billeteraOrigenId",b,"billeteraDestinoId",usd,"monto",10,"fecha",LocalDate.now().toString()))))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/categorias/"+gasto).with(user("qa.otro@finanzas.test"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/billeteras").with(user("qa.otro@finanzas.test"))).andExpect(jsonPath("$.length()").value(0));
        assertThat(leer("transacciones").size()).isZero();assertThat(leer("traspasos").size()).isZero();
        assertThat(leer("billeteras").get(0).get("saldoActual").asInt()).isEqualTo(100);
    }

    @Test void doceMesesGeneranMarkovYDosCasosVmeConGanadoresDistintos() throws Exception {
        registrar(email);long b=billetera("Banco",5000,"BOB"),i=categoria("Sueldo","INGRESO"),g=categoria("Comida","GASTO");
        var actual=YearMonth.now();String estados="EDE EDEE DDEDE".replace(" ","");
        for(int n=0;n<12;n++) {
            var fecha=actual.minusMonths(12-n).atDay(1);
            crear("transacciones",movimiento(b,i,"INGRESO",1000,fecha));
            crear("transacciones",movimiento(b,g,"GASTO",estados.charAt(n)=='E'?800:1200,fecha));
        }
        crear("transacciones",movimiento(b,i,"INGRESO",100,LocalDate.now()));
        var m=leer("dashboard/analisis/markov?divisa=BOB");
        assertThat(m.get("historial").size()).isEqualTo(12);
        assertThat(m.get("matriz").get(0).get("aEstable").asInt()).isEqualTo(2);
        assertThat(m.get("matriz").get(0).get("aDeficit").asInt()).isEqualTo(4);
        var saldo=leer("dashboard/analisis/disponible?divisa=BOB").decimalValue();
        int[][][] casos={{{0,0},{60,10},{20,-200}},{{0,0},{10,0},{200,-20}}};
        for(int c=0;c<2;c++) {
            var alternativas=new ArrayList<Map<String,Object>>();String[] nombres={"Guardar","Ahorrar","Invertir"};
            for(int a=0;a<3;a++) alternativas.add(Map.of("nombre",nombres[a],"asignacion",a==0?0:500,
                    "resultadoEstable",casos[c][a][0],"resultadoDeficit",casos[c][a][1]));
            var v=respuesta(mvc.perform(post("/api/dashboard/analisis/vme").with(user(email)).contentType("application/json")
                    .content(json.writeValueAsString(Map.of("divisa","BOB","reservaMinima",saldo.subtract(java.math.BigDecimal.valueOf(500)),"alternativas",alternativas))))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertThat(v.get("recomendadas").get(0).asText()).isEqualTo(c==0?"Ahorrar":"Invertir");
            assertThat(v.get("markov").get("probabilidades")).isEqualTo(m.get("probabilidades"));
        }
        assertThat(leer("transacciones").size()).isEqualTo(25);
        assertThat(leer("dashboard/analisis/disponible?divisa=BOB").decimalValue()).isEqualByComparingTo(saldo);
    }

    @Test void dashboardNoSumaDolaresYBolivianosSinConversion() throws Exception {
        registrar(email);billetera("Bolivianos",100,"BOB");billetera("Dólares",300,"USD");
        assertThat(leer("dashboard/resumen").get("saldoTotal").asInt()).isEqualTo(100);
        assertThat(leer("dashboard/resumen?divisa=USD").get("saldoTotal").asInt()).isEqualTo(300);
        assertThat(leer("dashboard/resumen").get("divisa").asText()).isEqualTo("BOB");
        mvc.perform(get("/api/dashboard/resumen?divisa=incorrecta").with(user(email))).andExpect(status().isBadRequest());
    }
}
