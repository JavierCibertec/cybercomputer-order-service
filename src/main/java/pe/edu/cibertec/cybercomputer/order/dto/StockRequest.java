package pe.edu.cibertec.cybercomputer.order.dto;

import java.util.List;

public record StockRequest(List<ItemCompraRequest> items) {
}
