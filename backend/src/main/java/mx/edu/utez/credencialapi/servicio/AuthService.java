package mx.edu.utez.credencialapi.servicio;

import mx.edu.utez.credencialapi.dto.AuthResponse;
import mx.edu.utez.credencialapi.dto.EstudianteResponse;
import mx.edu.utez.credencialapi.dto.RegistroRequest;
import mx.edu.utez.credencialapi.modelo.Estudiante;
import mx.edu.utez.credencialapi.modelo.Sesion;
import mx.edu.utez.credencialapi.repositorio.EstudianteRepository;
import mx.edu.utez.credencialapi.repositorio.MatriculaPermitidaRepository;
import mx.edu.utez.credencialapi.repositorio.SesionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final EstudianteRepository estudiantes;
    private final MatriculaPermitidaRepository matriculas;
    private final SesionRepository sesiones;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AuthService(EstudianteRepository estudiantes, MatriculaPermitidaRepository matriculas, SesionRepository sesiones) {
        this.estudiantes = estudiantes;
        this.matriculas = matriculas;
        this.sesiones = sesiones;
    }

    public void verificarDisponibilidad(String matricula, String correo) {
        String m = normalizarMatricula(matricula);
        String c = normalizarCorreo(correo);
        if (!m.matches("^20253DS\\d{3}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La matrícula debe tener el formato 20253DSxxx.");
        }
        if (!matriculas.existsByMatriculaAndActivaTrue(m)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Esta matrícula no está en el listado de la universidad.");
        }
        if (estudiantes.existsByMatricula(m)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con esta matrícula.");
        }
        if (estudiantes.existsByCorreo(c)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con este correo.");
        }
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest req) {
        verificarDisponibilidad(req.matricula(), req.correo());
        String password = req.password();
        if (password.chars().noneMatch(Character::isLetter) || password.chars().noneMatch(Character::isDigit)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña debe incluir letras y números.");
        }

        Estudiante e = new Estudiante();
        e.setMatricula(normalizarMatricula(req.matricula()));
        e.setCorreo(normalizarCorreo(req.correo()));
        e.setPasswordHash(encoder.encode(password));
        e.setNombreCompleto(req.nombreCompleto().trim());
        e.setCarrera(req.carrera());
        e.setGustos(req.gustos() == null ? List.of() : req.gustos());
        e.setIntereses(req.intereses() == null ? List.of() : req.intereses());
        e.setDescripcion(req.descripcion() == null ? "" : req.descripcion().trim());
        estudiantes.save(e);

        return new AuthResponse(crearSesion(e.getId()), EstudianteResponse.de(e));
    }

    @Transactional
    public AuthResponse iniciarSesion(String usuario, String password) {
        String dato = usuario.trim();
        var encontrado = dato.contains("@")
                ? estudiantes.findByCorreo(normalizarCorreo(dato))
                : estudiantes.findByMatricula(normalizarMatricula(dato));
        Estudiante e = encontrado
                .filter(est -> encoder.matches(password, est.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Correo, matrícula o contraseña incorrectos."));
        return new AuthResponse(crearSesion(e.getId()), EstudianteResponse.de(e));
    }

    @Transactional
    public void cerrarSesion(String token) {
        sesiones.deleteById(token);
    }

    public Long estudianteDelToken(String token) {
        return sesiones.findById(token)
                .map(Sesion::getEstudianteId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sesión no válida. Inicia sesión de nuevo."));
    }

    private String crearSesion(Long estudianteId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        sesiones.save(new Sesion(token, estudianteId));
        return token;
    }

    private static String normalizarMatricula(String m) {
        return m == null ? "" : m.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizarCorreo(String c) {
        return c == null ? "" : c.trim().toLowerCase(Locale.ROOT);
    }
}
