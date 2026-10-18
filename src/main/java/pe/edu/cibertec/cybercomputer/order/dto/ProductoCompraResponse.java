package pe.edu.cibertec.cybercomputer.order.dto;

import java.math.BigDecimal;

public record ProductoCompraResponse(Integer idproducto, String nombre, Integer cantidad,
                                      BigDecimal precio, Integer stock) {
}
