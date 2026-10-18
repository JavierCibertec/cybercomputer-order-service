package pe.edu.cibertec.cybercomputer.order.remoteservice;

import feign.Retryer;
import org.springframework.context.annotation.Bean;

public class InventarioFeignConfig {
    @Bean
    Retryer noReintentarDescuentos() {
        return Retryer.NEVER_RETRY;
    }
}
