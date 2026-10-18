package pe.edu.cibertec.cybercomputer.order.remoteservice;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import pe.edu.cibertec.cybercomputer.order.dto.*;
import java.util.List;

@FeignClient(name = "inventory-service", configuration = InventarioFeignConfig.class)
public interface InventarioClient {
    @PostMapping(value = "/api/v1/internal/stock/descontar", consumes = "application/json")
    GenericResponse<List<ProductoCompraResponse>> descontar(
            @RequestHeader("Authorization") String authorization, @RequestBody StockRequest request);

    @PostMapping(value = "/api/v1/internal/stock/reponer", consumes = "application/json")
    void reponer(@RequestHeader("Authorization") String authorization, @RequestBody StockRequest request);
}
