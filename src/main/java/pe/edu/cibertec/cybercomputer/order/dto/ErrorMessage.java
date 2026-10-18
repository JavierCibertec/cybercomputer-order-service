package pe.edu.cibertec.cybercomputer.order.dto;

import java.util.Map;

public record ErrorMessage(String message, Map<String, String> fields) {
    public ErrorMessage(String message) {
        this(message, Map.of());
    }
}
