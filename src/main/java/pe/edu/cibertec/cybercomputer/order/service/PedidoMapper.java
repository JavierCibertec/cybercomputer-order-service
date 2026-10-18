package pe.edu.cibertec.cybercomputer.order.service;

import pe.edu.cibertec.cybercomputer.order.dto.*;
import pe.edu.cibertec.cybercomputer.order.model.Pedido;

public final class PedidoMapper {
    private PedidoMapper() { }

    public static PedidoResponse respuesta(Pedido p) {
        return new PedidoResponse(p.getIdpedido(), p.getIdusuario(), p.getFechaCreacion(),
                p.getEstado(), p.getTotal(), p.getDetalles().stream()
                .map(d -> new DetallePedidoResponse(d.getIdproducto(), d.getNombreProducto(),
                        d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal())).toList(),
                p.getDestinatario() == null ? null : new DatosEntregaResponse(
                        p.getDestinatario(), p.getTelefono(), p.getDireccion()));
    }
}
