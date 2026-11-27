package mx.edu.utez.credencialapi.web;

import mx.edu.utez.credencialapi.dto.ErrorResponse;
import mx.edu.utez.credencialapi.servicio.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> api(ApiException ex) {
        return respuesta(ex.getEstado(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("Datos no válidos.");
        return respuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoInvalido(HttpMessageNotReadableException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> archivoGrande(MaxUploadSizeExceededException ex) {
        return respuesta(HttpStatus.PAYLOAD_TOO_LARGE, "La foto pesa más de 5 MB.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(NoResourceFoundException ex) {
        return respuesta(HttpStatus.NOT_FOUND, "Ruta no encontrada.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> general(Exception ex) {
        log.error("Error no controlado", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error en el servidor.");
    }

    private ResponseEntity<ErrorResponse> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(new ErrorResponse(estado.value(), mensaje));
    }
}
