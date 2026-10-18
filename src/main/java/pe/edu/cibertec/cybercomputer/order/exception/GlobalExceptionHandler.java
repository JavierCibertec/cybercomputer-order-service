package pe.edu.cibertec.cybercomputer.order.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.edu.cibertec.cybercomputer.order.dto.ErrorMessage;
import pe.edu.cibertec.cybercomputer.order.dto.GenericResponse;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(OperacionPedidoException.class)
    public ResponseEntity<GenericResponse<Void>> operacion(OperacionPedidoException ex) {
        return ResponseEntity.status(ex.getStatus()).body(GenericResponse.fail(new ErrorMessage(ex.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Void>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(GenericResponse.fail(
                new ErrorMessage("Revisa los datos ingresados", fields)));
    }

    @ExceptionHandler({HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<GenericResponse<Void>> parametros(Exception ex) {
        return ResponseEntity.badRequest().body(GenericResponse.fail(
                new ErrorMessage("Revisa el identificador y los datos ingresados")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse<Void>> json(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(GenericResponse.fail(
                new ErrorMessage("El cuerpo de la solicitud debe ser un JSON valido")));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse<Void>> integridad(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(GenericResponse.fail(
                new ErrorMessage("La operacion entra en conflicto con los datos registrados")));
    }
}
