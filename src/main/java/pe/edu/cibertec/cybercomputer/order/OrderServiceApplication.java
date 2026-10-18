package pe.edu.cibertec.cybercomputer.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import pe.edu.cibertec.cybercomputer.order.remoteservice.InventarioClient;

@SpringBootApplication
@EnableFeignClients(clients = InventarioClient.class)
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
