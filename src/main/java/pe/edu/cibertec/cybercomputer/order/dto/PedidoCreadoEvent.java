package pe.edu.cibertec.cybercomputer.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PedidoCreadoEvent(Integer pedidoId, Integer usuarioId,
                                BigDecimal total, LocalDateTime fechaCreacion) {
}
