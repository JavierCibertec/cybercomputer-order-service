package pe.edu.cibertec.cybercomputer.order;

import feign.FeignException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.cibertec.cybercomputer.order.dto.*;
import pe.edu.cibertec.cybercomputer.order.exception.*;
import pe.edu.cibertec.cybercomputer.order.remoteservice.InventarioClient;
import pe.edu.cibertec.cybercomputer.order.security.TokenInventarioService;
import pe.edu.cibertec.cybercomputer.order.service.*;
import java.math.BigDecimal;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PedidoCompraServiceTest {
    @Mock InventarioClient inventario;
    @Mock PedidoPersistenciaService persistencia;
    @Mock TokenInventarioService tokens;
    private PedidoCompraService compras;
    private final CompraRequest request = new CompraRequest(List.of(new ItemCompraRequest(101, 2)),
            new DatosEntregaRequest("Ana Perez", "987654321", "Av. Los Olivos 123, Lima"));
    private final StockRequest stock = new StockRequest(request.items());
    private final List<ProductoCompraResponse> respuesta = List.of(
            new ProductoCompraResponse(101, "Teclado", 2, new BigDecimal("89.90"), 3));

    @BeforeEach
    void preparar() {
        compras = new PedidoCompraService(inventario, persistencia, tokens);
        when(tokens.authorization(2)).thenReturn("Bearer interno");
        when(inventario.descontar("Bearer interno", stock)).thenReturn(GenericResponse.ok(respuesta));
    }

    @Test
    void compraExitosaNoReponeStock() {
        var esperado = new PedidoResponse(1, 2, null, null, new BigDecimal("179.80"), List.of(), null);
        when(persistencia.guardar(2, respuesta, request.entrega())).thenReturn(esperado);
        assertThat(compras.crear(2, request)).isSameAs(esperado);
        verify(inventario, never()).reponer(anyString(), any());
    }

    @Test
    void rollbackConfirmadoReponeUnaVez() {
        when(persistencia.guardar(2, respuesta, request.entrega())).thenThrow(new GuardadoPedidoException(true, new RuntimeException()));
        assertThatThrownBy(() -> compras.crear(2, request)).isInstanceOf(OperacionPedidoException.class)
                .hasMessageContaining("stock fue repuesto");
        verify(inventario, times(1)).reponer("Bearer interno", stock);
    }

    @Test
    void commitInciertoNoRepone() {
        when(persistencia.guardar(2, respuesta, request.entrega())).thenThrow(new GuardadoPedidoException(false, new RuntimeException()));
        assertThatThrownBy(() -> compras.crear(2, request)).isInstanceOf(OperacionPedidoException.class)
                .hasMessageContaining("requiere revision");
        verify(inventario, never()).reponer(anyString(), any());
    }

    @Test
    void reposicionFallidaNoSeReintenta() {
        when(persistencia.guardar(2, respuesta, request.entrega())).thenThrow(new GuardadoPedidoException(true, new RuntimeException()));
        FeignException error = mock(FeignException.class); when(error.status()).thenReturn(-1);
        doThrow(error).when(inventario).reponer("Bearer interno", stock);
        assertThatThrownBy(() -> compras.crear(2, request)).hasMessageContaining("requiere revision");
        verify(inventario, times(1)).descontar("Bearer interno", stock);
        verify(inventario, times(1)).reponer("Bearer interno", stock);
    }

    @Test
    void respuestaConPrecioInvalidoNoGuardaNiCompensaACiegas() {
        when(inventario.descontar("Bearer interno", stock)).thenReturn(GenericResponse.ok(List.of(
                new ProductoCompraResponse(101, "Teclado", 2, BigDecimal.ZERO, 3))));
        assertThatThrownBy(() -> compras.crear(2, request)).hasMessageContaining("Respuesta de inventario no valida");
        verifyNoInteractions(persistencia);
        verify(inventario, never()).reponer(anyString(), any());
    }
}
