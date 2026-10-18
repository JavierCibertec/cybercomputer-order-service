package pe.edu.cibertec.cybercomputer.order.exception;

public class GuardadoPedidoException extends RuntimeException {
    private final boolean rollbackConfirmado;
    public GuardadoPedidoException(boolean rollbackConfirmado, Throwable cause) {
        super("No se pudo completar el guardado del pedido", cause);
        this.rollbackConfirmado = rollbackConfirmado;
    }
    public boolean isRollbackConfirmado() { return rollbackConfirmado; }
}
