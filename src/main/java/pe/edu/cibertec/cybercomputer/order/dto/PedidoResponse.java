package pe.edu.cibertec.cybercomputer.order.dto;

import pe.edu.cibertec.cybercomputer.order.model.EstadoPedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(Integer idpedido, Integer idusuario, LocalDateTime fechaCreacion,
                              EstadoPedido estado, BigDecimal total, List<DetallePedidoResponse> detalles,
                              DatosEntregaResponse entrega) {
}
