package pe.edu.cibertec.cybercomputer.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.cibertec.cybercomputer.order.dto.*;
import pe.edu.cibertec.cybercomputer.order.exception.*;
import pe.edu.cibertec.cybercomputer.order.remoteservice.InventarioClient;
import pe.edu.cibertec.cybercomputer.order.security.TokenInventarioService;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoCompraService {
    private final InventarioClient inventario;
    private final PedidoPersistenciaService persistencia;
    private final TokenInventarioService tokenInterno;

    public PedidoResponse crear(Integer idusuario, CompraRequest request) {
        validarCarrito(request);
        var stock = new StockRequest(request.items());
        String operacion = UUID.randomUUID().toString();
        GenericResponse<List<ProductoCompraResponse>> respuesta;
        try {
            respuesta = inventario.descontar(tokenInterno.authorization(idusuario), stock);
        } catch (FeignException ex) {
            if (ex.status() == 409) {
                throw new OperacionPedidoException(409, "Stock insuficiente o productos modificados. Revisa el carrito");
            }
            if (ex.status() == 400 || ex.status() == 404) {
                throw new OperacionPedidoException(400, "Revisa los productos y las cantidades del carrito");
            }
            log.error("Compra {}: respuesta de inventario no confirmada (HTTP {}). No se reintenta", operacion, ex.status());
            throw new OperacionPedidoException(503,
                    "No se pudo confirmar el inventario. No repitas la compra sin revisar su resultado. Referencia: " + operacion);
        }
        if (!respuestaValida(request, respuesta)) {
            log.error("Compra {}: contrato de inventario invalido; requiere revision antes de reintentar", operacion);
            throw new OperacionPedidoException(502, "Respuesta de inventario no valida. Solicita revision. Referencia: " + operacion);
        }
        try {
            return persistencia.guardar(idusuario, respuesta.response(), request.entrega());
        } catch (GuardadoPedidoException ex) {
            if (ex.isRollbackConfirmado()) {
                try {
                    inventario.reponer(tokenInterno.authorization(idusuario), stock);
                    log.warn("Compra {}: pedido revertido y stock repuesto", operacion);
                    throw new OperacionPedidoException(503, "No se pudo guardar el pedido. El stock fue repuesto");
                } catch (FeignException reposicion) {
                    log.error("Compra {}: reposicion de stock no confirmada (HTTP {}); requiere revision", operacion, reposicion.status());
                }
            } else {
                log.error("Compra {}: resultado del guardado incierto; no se repone stock automaticamente", operacion);
            }
            throw new OperacionPedidoException(503, "La operacion requiere revision antes de volver a comprar. Referencia: " + operacion);
        }
    }

    private void validarCarrito(CompraRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty() || request.items().size() > 100) {
            throw new OperacionPedidoException(400, "El carrito debe contener entre 1 y 100 productos");
        }
        if (request.entrega() == null) {
            throw new OperacionPedidoException(400, "Completa los datos de entrega");
        }
        Set<Integer> ids = new HashSet<>();
        for (var item : request.items()) {
            if (item == null || item.idproducto() == null || item.idproducto() <= 0
                    || item.cantidad() == null || item.cantidad() <= 0 || item.cantidad() > 9999
                    || !ids.add(item.idproducto())) {
                throw new OperacionPedidoException(400, "Revisa las cantidades y evita productos repetidos en el carrito");
            }
        }
    }

    private boolean respuestaValida(CompraRequest request, GenericResponse<List<ProductoCompraResponse>> r) {
        if (r == null || r.error() != null || r.response() == null || r.response().size() != request.items().size()) {
            return false;
        }
        Map<Integer, Integer> cantidades = new HashMap<>();
        request.items().forEach(i -> cantidades.put(i.idproducto(), i.cantidad()));
        BigDecimal total = BigDecimal.ZERO;
        for (var p : r.response()) {
            if (p == null || p.idproducto() == null || p.nombre() == null || p.nombre().isBlank() || p.nombre().length() > 150
                    || p.cantidad() == null || !p.cantidad().equals(cantidades.remove(p.idproducto()))
                    || p.precio() == null || p.precio().signum() <= 0 || p.precio().stripTrailingZeros().scale() > 2
                    || p.precio().compareTo(new BigDecimal("99999999.99")) > 0 || p.stock() == null || p.stock() < 0) {
                return false;
            }
            total = total.add(p.precio().multiply(BigDecimal.valueOf(p.cantidad())));
        }
        return cantidades.isEmpty() && total.compareTo(new BigDecimal("999999999999.99")) <= 0;
    }
}
