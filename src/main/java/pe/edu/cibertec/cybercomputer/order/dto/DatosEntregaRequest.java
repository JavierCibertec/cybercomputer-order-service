package pe.edu.cibertec.cybercomputer.order.dto;

import jakarta.validation.constraints.*;

public record DatosEntregaRequest(
        @NotBlank @Size(min = 3, max = 150) String destinatario,
        @NotBlank @Size(max = 20) @Pattern(regexp = "\\+?[0-9][0-9 ()-]{5,18}[0-9]",
                message = "Ingresa un telefono valido") String telefono,
        @NotBlank @Size(min = 10, max = 300) String direccion) {
    public DatosEntregaRequest {
        destinatario = destinatario == null ? null : destinatario.strip();
        telefono = telefono == null ? null : telefono.strip();
        direccion = direccion == null ? null : direccion.strip();
    }
}
