package pe.edu.cibertec.cybercomputer.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CompraRequest(
        @NotEmpty(message = "Agrega al menos un producto")
        @Size(max = 100, message = "Se permiten hasta 100 productos distintos")
        List<@NotNull(message = "El detalle no puede ser nulo") @Valid ItemCompraRequest> items,
        @NotNull(message = "Completa los datos de entrega") @Valid DatosEntregaRequest entrega) {
}
