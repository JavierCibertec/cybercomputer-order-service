package pe.edu.cibertec.cybercomputer.order.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.edu.cibertec.cybercomputer.order.dto.*;
import pe.edu.cibertec.cybercomputer.order.rabbitmq.PedidoProductor;
import pe.edu.cibertec.cybercomputer.order.security.UsuarioAutenticado;
import pe.edu.cibertec.cybercomputer.order.service.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController {
    private final PedidoCompraService compra;
    private final PedidoService pedidos;
    private final PedidoProductor productor;

    @PostMapping
    public ResponseEntity<GenericResponse<PedidoResponse>> crear(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody CompraRequest request) {
        var pedido = compra.crear(usuario.idusuario(), request);
        productor.enviar(pedido);
        return ResponseEntity.created(URI.create("/api/v1/pedidos/" + pedido.idpedido()))
                .body(GenericResponse.ok(pedido));
    }

    @GetMapping("/mis")
    public GenericResponse<List<PedidoResponse>> misPedidos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return GenericResponse.ok(pedidos.misPedidos(usuario.idusuario()));
    }

    @GetMapping
    public GenericResponse<List<PedidoResponse>> listar() {
        return GenericResponse.ok(pedidos.listarTodos());
    }

    @GetMapping("/{id}")
    public GenericResponse<PedidoResponse> obtener(@PathVariable @Positive Integer id,
                                                  @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return GenericResponse.ok(pedidos.obtener(id, usuario));
    }

    @PutMapping("/{id}/estado")
    public GenericResponse<PedidoResponse> estado(@PathVariable @Positive Integer id,
                                                 @Valid @RequestBody CambiarEstadoRequest request) {
        return GenericResponse.ok(pedidos.cambiarEstado(id, request.estado()));
    }
}
