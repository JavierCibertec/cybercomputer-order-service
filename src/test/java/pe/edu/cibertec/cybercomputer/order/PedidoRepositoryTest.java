package pe.edu.cibertec.cybercomputer.order;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pe.edu.cibertec.cybercomputer.order.model.*;
import pe.edu.cibertec.cybercomputer.order.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
class PedidoRepositoryTest {
    @Autowired PedidoRepository pedidos;
    @Autowired EntityManager em;

    private Pedido nuevo(int idusuario) {
        var p = new Pedido(); p.setIdusuario(idusuario); p.setFechaCreacion(LocalDateTime.now());
        p.setEstado(EstadoPedido.PENDIENTE); p.setTotal(new BigDecimal("179.80"));
        var d = new DetallePedido(); d.setIdproducto(101); d.setNombreProducto("Teclado historico");
        d.setCantidad(2); d.setPrecioUnitario(new BigDecimal("89.90")); d.setSubtotal(new BigDecimal("179.80"));
        p.agregarDetalle(d); return p;
    }

    @Test
    void insertarPedidoYDetalle() {
        var p = pedidos.saveAndFlush(nuevo(2)); int id = p.getIdpedido(); em.clear();
        var guardado = pedidos.findById(id).orElseThrow();
        assertThat(guardado.getDetalles()).hasSize(1);
        assertThat(guardado.getDetalles().getFirst().getNombreProducto()).isEqualTo("Teclado historico");
        assertThat(guardado.getTotal()).isEqualByComparingTo("179.80");
    }

    @Test
    void actualizarEstado() {
        var p = pedidos.saveAndFlush(nuevo(2));
        p.setEstado(EstadoPedido.EN_PREPARACION); pedidos.saveAndFlush(p); int id = p.getIdpedido(); em.clear();
        assertThat(pedidos.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoPedido.EN_PREPARACION);
    }

    @Test
    void eliminarEnCapaDeDatos() {
        var p = pedidos.saveAndFlush(nuevo(2)); int id = p.getIdpedido();
        pedidos.delete(p); pedidos.flush(); em.clear();
        assertThat(pedidos.findById(id)).isEmpty();
        assertThat(em.createQuery("select count(d) from DetallePedido d", Long.class).getSingleResult()).isZero();
    }

    @Test
    void listarPorUsuario() {
        pedidos.saveAndFlush(nuevo(2)); pedidos.saveAndFlush(nuevo(3)); em.clear();
        assertThat(pedidos.findByIdusuarioOrderByFechaCreacionDescIdpedidoDesc(2)).hasSize(1)
                .allMatch(p -> p.getIdusuario().equals(2));
        assertThat(pedidos.findAllByOrderByFechaCreacionDescIdpedidoDesc()).hasSize(2);
    }
}
