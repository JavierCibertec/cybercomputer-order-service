package pe.edu.cibertec.cybercomputer.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.cybercomputer.order.dto.PedidoResponse;
import pe.edu.cibertec.cybercomputer.order.exception.OperacionPedidoException;
import pe.edu.cibertec.cybercomputer.order.model.EstadoPedido;
import pe.edu.cibertec.cybercomputer.order.repository.PedidoRepository;
import pe.edu.cibertec.cybercomputer.order.security.UsuarioAutenticado;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoService {
    private final PedidoRepository pedidos;

    public List<PedidoResponse> misPedidos(Integer idusuario) {
        return pedidos.findByIdusuarioOrderByFechaCreacionDescIdpedidoDesc(idusuario).stream()
                .map(PedidoMapper::respuesta).toList();
    }

    public List<PedidoResponse> listarTodos() {
        return pedidos.findAllByOrderByFechaCreacionDescIdpedidoDesc().stream()
                .map(PedidoMapper::respuesta).toList();
    }

    public PedidoResponse obtener(Integer id, UsuarioAutenticado usuario) {
        var p = pedidos.findById(id).orElseThrow(() -> noEncontrado());
        if (!usuario.authorities().contains("ROLE_ADMIN") && !p.getIdusuario().equals(usuario.idusuario())) {
            throw noEncontrado();
        }
        return PedidoMapper.respuesta(p);
    }

    @Transactional
    public PedidoResponse cambiarEstado(Integer id, EstadoPedido nuevo) {
        var p = pedidos.buscarParaActualizar(id).orElseThrow(() -> noEncontrado());
        if (p.getEstado() != nuevo) {
            boolean permitido = (p.getEstado() == EstadoPedido.PENDIENTE && nuevo == EstadoPedido.EN_PREPARACION)
                    || (p.getEstado() == EstadoPedido.EN_PREPARACION && nuevo == EstadoPedido.ENTREGADO);
            if (!permitido) {
                throw new OperacionPedidoException(409, "El cambio de estado no esta permitido");
            }
            p.setEstado(nuevo);
            pedidos.flush();
        }
        return PedidoMapper.respuesta(p);
    }

    private static OperacionPedidoException noEncontrado() {
        return new OperacionPedidoException(404, "El pedido no existe o no esta disponible para este usuario");
    }
}
