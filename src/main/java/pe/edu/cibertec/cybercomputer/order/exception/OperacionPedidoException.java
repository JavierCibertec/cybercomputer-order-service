package pe.edu.cibertec.cybercomputer.order.exception;

public class OperacionPedidoException extends RuntimeException {
    private final int status;
    public OperacionPedidoException(int status, String message) {
        super(message);
        this.status = status;
    }
    public int getStatus() { return status; }
}
