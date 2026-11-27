package mx.edu.utez.credencialapi.web;

import jakarta.validation.Valid;
import mx.edu.utez.credencialapi.config.AutenticacionInterceptor;
import mx.edu.utez.credencialapi.dto.AccesoRequest;
import mx.edu.utez.credencialapi.dto.AccesoResponse;
import mx.edu.utez.credencialapi.servicio.AccesoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accesos")
public class AccesoController {

    private final AccesoService accesos;

    public AccesoController(AccesoService accesos) {
        this.accesos = accesos;
    }

    @GetMapping
    public List<AccesoResponse> listar(@RequestAttribute(AutenticacionInterceptor.ESTUDIANTE_ID) Long id) {
        return accesos.listar(id);
    }

    @PostMapping
    public ResponseEntity<AccesoResponse> registrar(@RequestAttribute(AutenticacionInterceptor.ESTUDIANTE_ID) Long id,
                                                    @Valid @RequestBody AccesoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accesos.registrar(id, req));
    }

    @DeleteMapping("/{accesoId}")
    public ResponseEntity<Void> eliminar(@RequestAttribute(AutenticacionInterceptor.ESTUDIANTE_ID) Long id,
                                         @PathVariable Long accesoId) {
        accesos.eliminar(id, accesoId);
        return ResponseEntity.noContent().build();
    }
}
