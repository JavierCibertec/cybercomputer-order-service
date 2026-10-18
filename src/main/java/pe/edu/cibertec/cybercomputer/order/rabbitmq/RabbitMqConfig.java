package pe.edu.cibertec.cybercomputer.order.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String EXCHANGE = "cybercomputer.pedidos.exchange";
    public static final String QUEUE = "cybercomputer.pedidos.queue";
    public static final String ROUTING_KEY = "cybercomputer.pedido.creado";

    @Bean
    DirectExchange pedidosExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue pedidosQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    Binding pedidosBinding(Queue pedidosQueue, DirectExchange pedidosExchange) {
        return BindingBuilder.bind(pedidosQueue).to(pedidosExchange).with(ROUTING_KEY);
    }
}
