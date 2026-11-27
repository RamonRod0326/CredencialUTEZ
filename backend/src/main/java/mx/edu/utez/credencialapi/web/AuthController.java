package mx.edu.utez.credencialapi.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import mx.edu.utez.credencialapi.config.AutenticacionInterceptor;
import mx.edu.utez.credencialapi.dto.AuthResponse;
import mx.edu.utez.credencialapi.dto.LoginRequest;
import mx.edu.utez.credencialapi.dto.RegistroRequest;
import mx.edu.utez.credencialapi.dto.VerificarRequest;
import mx.edu.utez.credencialapi.servicio.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping("/salud")
    public Map<String, String> salud() {
        return Map.of("estado", "ok");
    }

    @PostMapping("/auth/verificar")
    public ResponseEntity<Void> verificar(@Valid @RequestBody VerificarRequest req) {
        auth.verificarDisponibilidad(req.matricula(), req.correo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/registro")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.registrar(req));
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.iniciarSesion(req.usuario(), req.password());
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        auth.cerrarSesion((String) request.getAttribute(AutenticacionInterceptor.TOKEN));
        return ResponseEntity.noContent().build();
    }
}
