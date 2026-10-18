package pe.edu.cibertec.cybercomputer.order.security;

import java.security.Principal;
import java.util.List;

public record UsuarioAutenticado(Integer idusuario, String nomusuario,
                                 List<String> authorities) implements Principal {
    public UsuarioAutenticado {
        authorities = List.copyOf(authorities);
    }

    @Override
    public String getName() {
        return nomusuario;
    }
}
