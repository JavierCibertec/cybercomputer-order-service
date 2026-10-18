package pe.edu.cibertec.cybercomputer.order;

import com.jayway.jsonpath.JsonPath;
import feign.FeignException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.cibertec.cybercomputer.order.dto.*;
import pe.edu.cibertec.cybercomputer.order.model.*;
import pe.edu.cibertec.cybercomputer.order.repository.PedidoRepository;
import pe.edu.cibertec.cybercomputer.order.remoteservice.InventarioClient;
import pe.edu.cibertec.cybercomputer.order.service.PedidoPersistenciaService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import pe.edu.cibertec.cybercomputer.order.exception.GuardadoPedidoException;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:cybercomputer-order-integration;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired PedidoRepository pedidos;
    @Autowired PedidoPersistenciaService persistencia;
    @MockitoBean InventarioClient inventario;
    @Value("${app.jwt.secret-base64}") String secret;
    private String cliente, otro, admin;
    private static final String API = "/api/v1/pedidos";
    private static final String BODY = "{\"items\":[{\"idproducto\":101,\"cantidad\":2},{\"idproducto\":102,\"cantidad\":3}],\"entrega\":{\"destinatario\":\"Ana Perez\",\"telefono\":\"987654321\",\"direccion\":\"Av. Los Olivos 123, Lima\"}}";

    @BeforeEach
    void preparar() {
        pedidos.deleteAll();
        cliente = token(2, "cliente", "ROLE_CLIENTE", "cybercomputer", 600);
        otro = token(3, "otro", "ROLE_CLIENTE", "cybercomputer", 600);
        admin = token(1, "admin", "ROLE_ADMIN", "cybercomputer", 600);
        when(inventario.descontar(anyString(), any())).thenReturn(GenericResponse.ok(List.of(
                new ProductoCompraResponse(101, "Teclado", 2, new BigDecimal("89.90"), 8),
                new ProductoCompraResponse(102, "Mouse", 3, new BigDecimal("35.50"), 7))));
    }

    private String token(int id, String user, String role, String issuer, int seconds) {
        Instant now = Instant.now();
        return "Bearer " + Jwts.builder().issuer(issuer).subject(user).claim("idusuario", id)
                .claim("authorities", List.of(role)).issuedAt(Date.from(now.minusSeconds(60)))
                .expiration(Date.from(now.plusSeconds(seconds)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)), Jwts.SIG.HS256).compact();
    }

    private PedidoResponse guardado(int idusuario) {
        return persistencia.guardar(idusuario, List.of(new ProductoCompraResponse(101, "Teclado", 2,
                new BigDecimal("89.90"), 8)), entrega());
    }
    private String estado(String value) { return "{\"estado\":\"" + value + "\"}"; }
    private FeignException errorFeign(int status) {
        var error = mock(FeignException.class); when(error.status()).thenReturn(status); return error;
    }

    @Test
    void clienteCreaYTotalSeCalculaEnBackend() throws Exception {
        String json = mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.response.idusuario").value(2))
                .andExpect(jsonPath("$.response.total").value(286.3))
                .andExpect(jsonPath("$.response.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.response.detalles", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(json, "$.response.idpedido");
        var pedido = pedidos.findById(id).orElseThrow();
        assertThat(pedido.getTotal()).isEqualByComparingTo("286.30");
        assertThat(pedido.getDetalles()).hasSize(2);
        verify(inventario, times(1)).descontar(anyString(), any());
    }

    @Test
    void misPedidosSoloIncluyePropios() throws Exception {
        guardado(2); guardado(3);
        mvc.perform(get(API + "/mis").header("Authorization", cliente)).andExpect(status().isOk())
                .andExpect(jsonPath("$.response", hasSize(1))).andExpect(jsonPath("$.response[0].idusuario").value(2));
    }

    @Test
    void clienteConsultaDetallePropio() throws Exception {
        var p = guardado(2);
        mvc.perform(get(API + "/" + p.idpedido()).header("Authorization", cliente)).andExpect(status().isOk())
                .andExpect(jsonPath("$.response.detalles[0].precioUnitario").value(89.9));
    }

    @Test
    void clienteNoConsultaPedidoAjeno() throws Exception {
        var p = guardado(2);
        mvc.perform(get(API + "/" + p.idpedido()).header("Authorization", otro)).andExpect(status().isNotFound());
    }

    @Test
    void adminListaPedidosDeTodos() throws Exception {
        guardado(2); guardado(3);
        mvc.perform(get(API).header("Authorization", admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.response", hasSize(2)));
    }

    @Test
    void adminConsultaDetalleDeCliente() throws Exception {
        var p = guardado(2);
        mvc.perform(get(API + "/" + p.idpedido()).header("Authorization", admin)).andExpect(status().isOk());
    }

    @Test
    void clienteNoPuedeListarTodos() throws Exception {
        mvc.perform(get(API).header("Authorization", cliente)).andExpect(status().isForbidden());
    }

    @Test
    void clienteNoPuedeCambiarEstado() throws Exception {
        var p = guardado(2);
        mvc.perform(put(API + "/" + p.idpedido() + "/estado").header("Authorization", cliente)
                .contentType(MediaType.APPLICATION_JSON).content(estado("EN_PREPARACION"))).andExpect(status().isForbidden());
    }

    @Test
    void adminAvanzaEstadosYSePersisten() throws Exception {
        var p = guardado(2); String uri = API + "/" + p.idpedido() + "/estado";
        mvc.perform(put(uri).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(estado("EN_PREPARACION")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response.estado").value("EN_PREPARACION"));
        mvc.perform(put(uri).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(estado("ENTREGADO")))
                .andExpect(status().isOk());
        assertThat(pedidos.findById(p.idpedido()).orElseThrow().getEstado()).isEqualTo(EstadoPedido.ENTREGADO);
    }

    @Test
    void noPermiteSaltarPreparacion() throws Exception {
        var p = guardado(2);
        mvc.perform(put(API + "/" + p.idpedido() + "/estado").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(estado("ENTREGADO"))).andExpect(status().isConflict());
    }

    @Test
    void noPermiteRetrocederEstado() throws Exception {
        var p = guardado(2); String uri = API + "/" + p.idpedido() + "/estado";
        mvc.perform(put(uri).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(estado("EN_PREPARACION")))
                .andExpect(status().isOk());
        mvc.perform(put(uri).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(estado("PENDIENTE")))
                .andExpect(status().isConflict());
    }

    @Test
    void repetirMismoEstadoEsIdempotente() throws Exception {
        var p = guardado(2);
        mvc.perform(put(API + "/" + p.idpedido() + "/estado").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(estado("PENDIENTE"))).andExpect(status().isOk());
    }

    @Test
    void estadoInexistente() throws Exception {
        var p = guardado(2);
        mvc.perform(put(API + "/" + p.idpedido() + "/estado").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(estado("CANCELADO"))).andExpect(status().isBadRequest());
    }

    @Test
    void estadoNulo() throws Exception {
        var p = guardado(2);
        mvc.perform(put(API + "/" + p.idpedido() + "/estado").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":null}")).andExpect(status().isBadRequest());
    }

    @Test
    void carritoVacio() throws Exception {
        mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content("{\"items\":[],\"entrega\":{\"destinatario\":\"Ana Perez\",\"telefono\":\"987654321\",\"direccion\":\"Av. Los Olivos 123, Lima\"}}"))
                .andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero(); verify(inventario, never()).descontar(anyString(), any());
    }

    @Test
    void itemNulo() throws Exception {
        mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content("{\"items\":[null],\"entrega\":{\"destinatario\":\"Ana Perez\",\"telefono\":\"987654321\",\"direccion\":\"Av. Los Olivos 123, Lima\"}}"))
                .andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero(); verify(inventario, never()).descontar(anyString(), any());
    }

    @Test
    void cantidadCero() throws Exception {
        mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"idproducto\":101,\"cantidad\":0}],\"entrega\":{\"destinatario\":\"Ana Perez\",\"telefono\":\"987654321\",\"direccion\":\"Av. Los Olivos 123, Lima\"}}"))
                .andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero(); verify(inventario, never()).descontar(anyString(), any());
    }

    @Test
    void cantidadNegativa() throws Exception {
        mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"idproducto\":101,\"cantidad\":-1}],\"entrega\":{\"destinatario\":\"Ana Perez\",\"telefono\":\"987654321\",\"direccion\":\"Av. Los Olivos 123, Lima\"}}"))
                .andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero(); verify(inventario, never()).descontar(anyString(), any());
    }

    @Test
    void cantidadDecimal() throws Exception {
        mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"idproducto\":101,\"cantidad\":1.5}],\"entrega\":{\"destinatario\":\"Ana Perez\",\"telefono\":\"987654321\",\"direccion\":\"Av. Los Olivos 123, Lima\"}}"))
                .andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero(); verify(inventario, never()).descontar(anyString(), any());
    }

    @Test
    void cantidadExcesiva() throws Exception {
        mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"idproducto\":101,\"cantidad\":10000}],\"entrega\":{\"destinatario\":\"Ana Perez\",\"telefono\":\"987654321\",\"direccion\":\"Av. Los Olivos 123, Lima\"}}"))
                .andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero(); verify(inventario, never()).descontar(anyString(), any());
    }

    @Test
    void productoRepetido() throws Exception {
        mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"idproducto\":101,\"cantidad\":1},{\"idproducto\":101,\"cantidad\":2}],\"entrega\":{\"destinatario\":\"Ana Perez\",\"telefono\":\"987654321\",\"direccion\":\"Av. Los Olivos 123, Lima\"}}"))
                .andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero(); verify(inventario, never()).descontar(anyString(), any());
    }

    @Test
    void productoSinId() throws Exception {
        mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"cantidad\":1}],\"entrega\":{\"destinatario\":\"Ana Perez\",\"telefono\":\"987654321\",\"direccion\":\"Av. Los Olivos 123, Lima\"}}"))
                .andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero(); verify(inventario, never()).descontar(anyString(), any());
    }

    @Test
    void clienteNoEnviaSuplantacion() throws Exception {
        mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content("{\"idusuario\":1,\"items\":[{\"idproducto\":101,\"cantidad\":1}],\"entrega\":{\"destinatario\":\"Ana Perez\",\"telefono\":\"987654321\",\"direccion\":\"Av. Los Olivos 123, Lima\"}}"))
                .andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero(); verify(inventario, never()).descontar(anyString(), any());
    }

    @Test
    void clienteNoEnviaPrecio() throws Exception {
        mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"idproducto\":101,\"cantidad\":1,\"precio\":0.01}],\"entrega\":{\"destinatario\":\"Ana Perez\",\"telefono\":\"987654321\",\"direccion\":\"Av. Los Olivos 123, Lima\"}}"))
                .andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero(); verify(inventario, never()).descontar(anyString(), any());
    }

    @Test
    void sinTokenEsRechazado() throws Exception {
        mvc.perform(get(API + "/mis")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoEsRechazado() throws Exception {
        mvc.perform(get(API + "/mis").header("Authorization", "Bearer invalido")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoEsRechazado() throws Exception {
        mvc.perform(get(API + "/mis").header("Authorization", token(2, "cliente", "ROLE_CLIENTE", "cybercomputer", -1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emisorIncorrectoEsRechazado() throws Exception {
        mvc.perform(get(API + "/mis").header("Authorization", token(2, "cliente", "ROLE_CLIENTE", "otro", 600)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pedidoInexistenteDevuelve404() throws Exception {
        mvc.perform(get(API + "/2147483647").header("Authorization", admin)).andExpect(status().isNotFound());
    }

    @Test
    void sinStockNoGuardaPedido() throws Exception {
        var error = errorFeign(409);
        when(inventario.descontar(anyString(), any())).thenThrow(error);
        mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
        assertThat(pedidos.count()).isZero();
    }

    @Test
    void productoEliminadoNoGuardaPedido() throws Exception {
        var error = errorFeign(404);
        when(inventario.descontar(anyString(), any())).thenThrow(error);
        mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());
        assertThat(pedidos.count()).isZero();
    }

    @Test
    void inventarioNoDisponibleNoGuardaNiReintenta() throws Exception {
        var error = errorFeign(-1);
        when(inventario.descontar(anyString(), any())).thenThrow(error);
        mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isServiceUnavailable());
        assertThat(pedidos.count()).isZero(); verify(inventario, times(1)).descontar(anyString(), any());
    }

    @Test
    void contratoRemotoInvalidoNoGuarda() throws Exception {
        when(inventario.descontar(anyString(), any())).thenReturn(GenericResponse.ok(List.of()));
        mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadGateway());
        assertThat(pedidos.count()).isZero();
    }

    @Test
    void noSeExponeDeleteDePedidos() throws Exception {
        var p = guardado(2);
        mvc.perform(delete(API + "/" + p.idpedido()).header("Authorization", admin)).andExpect(status().isForbidden());
        assertThat(pedidos.existsById(p.idpedido())).isTrue();
    }

    @Test
    void tokenInternoNoListaPedidos() throws Exception {
        mvc.perform(get(API).header("Authorization", token(2, "order-service", "ROLE_ORDER_SERVICE", "cybercomputer", 600)))
                .andExpect(status().isForbidden());
    }

    @Test
    void persistenciaReviertePedidoYDetallesCuandoFallaRestriccion() {
        var duplicado = new ProductoCompraResponse(101, "Teclado", 1, new BigDecimal("89.90"), 3);
        assertThatThrownBy(() -> persistencia.guardar(2, List.of(duplicado, duplicado), entrega()))
                .isInstanceOf(GuardadoPedidoException.class)
                .satisfies(ex -> assertThat(((GuardadoPedidoException) ex).isRollbackConfirmado()).isTrue());
        assertThat(pedidos.count()).isZero();
    }

    private DatosEntregaRequest entrega() {
        return new DatosEntregaRequest("Ana Perez", "987654321", "Av. Los Olivos 123, Lima");
    }

    @Test
    void pedidoGuardaDatosDeEntregaYAdminLosConsulta() throws Exception {
        String json = mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.response.entrega.destinatario").value("Ana Perez"))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(json, "$.response.idpedido");
        assertThat(pedidos.findById(id).orElseThrow().getDireccion()).isEqualTo("Av. Los Olivos 123, Lima");
        mvc.perform(get(API + "/" + id).header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response.entrega.telefono").value("987654321"))
                .andExpect(jsonPath("$.response.entrega.direccion").value("Av. Los Olivos 123, Lima"));
        mvc.perform(get(API + "/" + id).header("Authorization", otro)).andExpect(status().isNotFound());
    }

    @Test
    void sinEntregaNoDescuentaStockNiCreaPedido() throws Exception {
        mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"idproducto":101,"cantidad":2}]}
                                """))
                .andExpect(status().isBadRequest());
        verify(inventario, never()).descontar(anyString(), any());
        assertThat(pedidos.count()).isZero();
    }

    @Test
    void entregaInvalidaNoDescuentaStock() throws Exception {
        for (String entrega : new String[]{
                "null",
                """
                {"destinatario":"   ","telefono":"987654321","direccion":"Av. Los Olivos 123"}
                """,
                "{\"destinatario\":\"Ana Perez\",\"telefono\":\"abcdefghi\",\"direccion\":\"Av. Los Olivos 123\"}",
                "{\"destinatario\":\"Ana Perez\",\"telefono\":\"987654321\",\"direccion\":\"  corta  \"}"}) {
            mvc.perform(post(API).header("Authorization", cliente).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"items\":[{\"idproducto\":101,\"cantidad\":2}],\"entrega\":" + entrega + "}"))
                    .andExpect(status().isBadRequest());
        }
        verify(inventario, never()).descontar(anyString(), any());
        assertThat(pedidos.count()).isZero();
    }
}
