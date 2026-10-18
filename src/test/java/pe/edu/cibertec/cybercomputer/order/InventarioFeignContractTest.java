package pe.edu.cibertec.cybercomputer.order;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import feign.FeignException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import pe.edu.cibertec.cybercomputer.order.dto.*;
import pe.edu.cibertec.cybercomputer.order.remoteservice.InventarioClient;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:cybercomputer-feign-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InventarioFeignContractTest {
    private static final AtomicInteger llamadas = new AtomicInteger();
    private static final AtomicInteger codigo = new AtomicInteger(200);
    private static final AtomicReference<String> cuerpo = new AtomicReference<>();
    private static final AtomicReference<String> authorization = new AtomicReference<>();
    private static final HttpServer servidor = iniciarServidor();
    @Autowired InventarioClient client;

    private static HttpServer iniciarServidor() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/internal/stock", exchange -> {
                llamadas.incrementAndGet();
                cuerpo.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                if (exchange.getRequestURI().getPath().endsWith("/reponer")) {
                    exchange.sendResponseHeaders(204, -1);
                } else {
                    byte[] data = "{\"response\":[{\"idproducto\":101,\"nombre\":\"Teclado\",\"cantidad\":2,\"precio\":89.90,\"stock\":3}],\"error\":null}"
                            .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(codigo.get(), data.length);
                    exchange.getResponseBody().write(data);
                }
                exchange.close();
            });
            server.start(); return server;
        } catch (IOException ex) { throw new IllegalStateException(ex); }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.openfeign.client.config.inventory-service.url",
                () -> "http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    @BeforeEach
    void preparar() { llamadas.set(0); codigo.set(200); cuerpo.set(null); authorization.set(null); }
    @AfterAll
    static void detener() { servidor.stop(0); }
    private StockRequest request() { return new StockRequest(List.of(new ItemCompraRequest(101, 2))); }

    @Test
    void enviaJsonYDecodificaRespuestaRealDeHttp() {
        var r = client.descontar("Bearer prueba-contrato", request());
        assertThat(r.response()).hasSize(1);
        assertThat(r.response().getFirst().precio()).isEqualByComparingTo("89.90");
        assertThat(authorization.get()).isEqualTo("Bearer prueba-contrato");
        assertThat((Integer) JsonPath.read(cuerpo.get(), "$.items[0].cantidad")).isEqualTo(2);
        assertThat(cuerpo.get()).doesNotContain("entrega", "telefono", "direccion");
        assertThat(llamadas.get()).isEqualTo(1);
    }

    @Test
    void procesaRespuesta204DeReposicion() {
        client.reponer("Bearer prueba-contrato", request());
        assertThat(cuerpo.get()).doesNotContain("entrega", "telefono", "direccion");
        assertThat(llamadas.get()).isEqualTo(1);
        assertThat((Integer) JsonPath.read(cuerpo.get(), "$.items[0].idproducto")).isEqualTo(101);
    }

    @Test
    void postFallidoNoSeRepiteAutomaticamente() {
        codigo.set(500);
        assertThatThrownBy(() -> client.descontar("Bearer prueba-contrato", request())).isInstanceOf(FeignException.class);
        assertThat(cuerpo.get()).doesNotContain("entrega", "telefono", "direccion");
        assertThat(llamadas.get()).isEqualTo(1);
    }
}
