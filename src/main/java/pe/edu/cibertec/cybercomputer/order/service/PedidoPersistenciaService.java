package pe.edu.cibertec.cybercomputer.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import pe.edu.cibertec.cybercomputer.order.dto.*;
import pe.edu.cibertec.cybercomputer.order.exception.GuardadoPedidoException;
import pe.edu.cibertec.cybercomputer.order.model.*;
import pe.edu.cibertec.cybercomputer.order.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PedidoPersistenciaService {
    private final PedidoRepository pedidos;
    private final TransactionTemplate transaccion;

    public PedidoPersistenciaService(PedidoRepository pedidos, PlatformTransactionManager manager) {
        this.pedidos = pedidos;
        this.transaccion = new TransactionTemplate(manager);
    }

    public PedidoResponse guardar(Integer idusuario, List<ProductoCompraResponse> productos, DatosEntregaRequest entrega) {
        AtomicInteger resultado = new AtomicInteger(-1);
        try {
            return transaccion.execute(status -> {
                resultado.set(TransactionSynchronization.STATUS_UNKNOWN);
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int completionStatus) { resultado.set(completionStatus); }
                });
                var pedido = new Pedido();
                pedido.setIdusuario(idusuario);
                pedido.setDestinatario(entrega.destinatario());
                pedido.setTelefono(entrega.telefono());
                pedido.setDireccion(entrega.direccion());
                pedido.setEstado(EstadoPedido.PENDIENTE);
                pedido.setFechaCreacion(LocalDateTime.now(ZoneOffset.UTC));
                BigDecimal total = BigDecimal.ZERO.setScale(2);
                for (var p : productos) {
                    var detalle = new DetallePedido();
                    detalle.setIdproducto(p.idproducto());
                    detalle.setNombreProducto(p.nombre());
                    detalle.setCantidad(p.cantidad());
                    detalle.setPrecioUnitario(p.precio());
                    detalle.setSubtotal(p.precio().multiply(BigDecimal.valueOf(p.cantidad())));
                    pedido.agregarDetalle(detalle);
                    total = total.add(detalle.getSubtotal());
                }
                pedido.setTotal(total);
                return PedidoMapper.respuesta(pedidos.saveAndFlush(pedido));
            });
        } catch (RuntimeException ex) {
            boolean rollback = resultado.get() == -1
                    || resultado.get() == TransactionSynchronization.STATUS_ROLLED_BACK;
            throw new GuardadoPedidoException(rollback, ex);
        }
    }
}
