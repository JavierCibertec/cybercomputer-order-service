package pe.edu.cibertec.cybercomputer.order.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.cibertec.cybercomputer.order.model.Pedido;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    @Override
    @EntityGraph(attributePaths = "detalles")
    Optional<Pedido> findById(Integer id);

    @EntityGraph(attributePaths = "detalles")
    List<Pedido> findByIdusuarioOrderByFechaCreacionDescIdpedidoDesc(Integer idusuario);

    @EntityGraph(attributePaths = "detalles")
    List<Pedido> findAllByOrderByFechaCreacionDescIdpedidoDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.idpedido = :id")
    Optional<Pedido> buscarParaActualizar(@Param("id") Integer id);
}
