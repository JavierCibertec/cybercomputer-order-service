package pe.edu.cibertec.cybercomputer.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido",
        indexes = @Index(name = "idx_pedido_usuario_fecha", columnList = "idusuario, fecha_creacion"),
        check = {
                @CheckConstraint(name = "ck_pedido_usuario", constraint = "idusuario > 0"),
                @CheckConstraint(name = "ck_pedido_total", constraint = "total > 0"),
                @CheckConstraint(name = "ck_pedido_estado",
                        constraint = "estado IN ('PENDIENTE', 'EN_PREPARACION', 'ENTREGADO')")
        })
@Getter @Setter @NoArgsConstructor
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idpedido")
    private Integer idpedido;

    @Column(name = "idusuario", nullable = false)
    private Integer idusuario;

    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @ColumnDefault("'PENDIENTE'")
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "destinatario", length = 150)
    private String destinatario;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "direccion", length = 300)
    private String direccion;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("iddetalle ASC")
    private List<DetallePedido> detalles = new ArrayList<>();

    public void agregarDetalle(DetallePedido detalle) {
        detalles.add(detalle);
        detalle.setPedido(this);
    }
}
