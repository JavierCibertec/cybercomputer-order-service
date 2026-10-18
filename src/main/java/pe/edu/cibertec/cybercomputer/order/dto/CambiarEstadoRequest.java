package pe.edu.cibertec.cybercomputer.order.dto;

import jakarta.validation.constraints.NotNull;
import pe.edu.cibertec.cybercomputer.order.model.EstadoPedido;

public record CambiarEstadoRequest(@NotNull(message = "El estado es obligatorio") EstadoPedido estado) {
}
