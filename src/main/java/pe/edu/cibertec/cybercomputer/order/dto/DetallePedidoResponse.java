package pe.edu.cibertec.cybercomputer.order.dto;

import java.math.BigDecimal;

public record DetallePedidoResponse(Integer idproducto, String nombreProducto, Integer cantidad,
                                     BigDecimal precioUnitario, BigDecimal subtotal) {
}
