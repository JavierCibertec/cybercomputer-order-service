package pe.edu.cibertec.cybercomputer.order.rabbitmq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.cibertec.cybercomputer.order.dto.PedidoResponse;
import pe.edu.cibertec.cybercomputer.order.dto.PedidoCreadoEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoProductor {
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.publicacion-habilitada:true}")
    private boolean publicacionHabilitada;

    public boolean enviar(PedidoResponse pedido) {
        if (!publicacionHabilitada) {
            return false;
        }
        var evento = new PedidoCreadoEvent(
                pedido.idpedido(), pedido.idusuario(), pedido.total(), pedido.fechaCreacion());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE,
                    RabbitMqConfig.ROUTING_KEY,
                    evento);
            log.info("Pedido publicado en RabbitMQ: id={}, usuario={}, total={}",
                    evento.pedidoId(), evento.usuarioId(), evento.total());
            return true;
        } catch (AmqpException ex) {
            log.error("Pedido {} guardado, pero no se pudo publicar en RabbitMQ: {}",
                    pedido.idpedido(), ex.getMessage());
            return false;
        }
    }
}
