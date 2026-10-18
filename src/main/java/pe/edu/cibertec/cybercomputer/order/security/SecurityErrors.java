package pe.edu.cibertec.cybercomputer.order.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

final class SecurityErrors {
    private SecurityErrors() {
    }

    static void unauthorized(HttpServletResponse response) throws IOException {
        write(response, 401, "{\"response\":null,\"error\":{\"message\":\"Autenticacion requerida o token invalido\",\"fields\":{}}}");
    }

    static void forbidden(HttpServletResponse response) throws IOException {
        write(response, 403, "{\"response\":null,\"error\":{\"message\":\"No tienes permiso para esta operacion\",\"fields\":{}}}");
    }

    private static void write(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }
}
