package pe.edu.cibertec.cybercomputer.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_pedido", uniqueConstraints =
        @UniqueConstraint(name = "uk_detalle_pedido_producto", columnNames = {"idpedido", "idproducto"}),
        check = {
                @CheckConstraint(name = "ck_detalle_producto", constraint = "idproducto > 0"),
                @CheckConstraint(name = "ck_detalle_cantidad", constraint = "cantidad > 0"),
                @CheckConstraint(name = "ck_detalle_precio", constraint = "precio_unitario > 0"),
                @CheckConstraint(name = "ck_detalle_subtotal",
                        constraint = "subtotal > 0 AND subtotal = cantidad * precio_unitario")
        })
@Getter @Setter @NoArgsConstructor
public class DetallePedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "iddetalle")
    private Integer iddetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idpedido", nullable = false,
            foreignKey = @ForeignKey(name = "fk_detalle_pedido"))
    private Pedido pedido;

    @Column(name = "idproducto", nullable = false)
    private Integer idproducto;

    @Column(name = "nombre_producto", nullable = false, length = 150)
    private String nombreProducto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;
}
