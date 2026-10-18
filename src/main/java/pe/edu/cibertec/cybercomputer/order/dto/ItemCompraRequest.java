package pe.edu.cibertec.cybercomputer.order.dto;

import jakarta.validation.constraints.*;

public record ItemCompraRequest(
        @NotNull(message = "El producto es obligatorio")
        @Positive(message = "El identificador del producto debe ser positivo") Integer idproducto,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor que cero")
        @Max(value = 9999, message = "La cantidad maxima por producto es 9999") Integer cantidad) {
}
