package mx.edu.utez.credencialapi.config;

import mx.edu.utez.credencialapi.modelo.Estudiante;
import mx.edu.utez.credencialapi.modelo.MatriculaPermitida;
import mx.edu.utez.credencialapi.repositorio.EstudianteRepository;
import mx.edu.utez.credencialapi.repositorio.MatriculaPermitidaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private static final String PASSWORD_INICIAL = "proyecto123";
    private static final String CARRERA = "TSU en Desarrollo de Software Multiplataforma";

    private record Integrante(String nombre, String matricula) {
    }

    private static final List<Integrante> INTEGRANTES = List.of(
            new Integrante("Ramón Rodríguez Memije", "20253DS086"),
            new Integrante("Gerardo García Rodríguez", "20253DS066"),
            new Integrante("Angel Said Brito Ibarra", "20253DS090")
    );

    private final MatriculaPermitidaRepository matriculas;
    private final EstudianteRepository estudiantes;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public DatosIniciales(MatriculaPermitidaRepository matriculas, EstudianteRepository estudiantes) {
        this.matriculas = matriculas;
        this.estudiantes = estudiantes;
    }

    @Override
    @Transactional
    public void run(String... args) {
        cargarMatriculas();
        crearCuentasDelEquipo();
    }

    private void cargarMatriculas() {
        Set<String> existentes = matriculas.findAll().stream()
                .map(MatriculaPermitida::getMatricula)
                .collect(Collectors.toSet());
        List<MatriculaPermitida> nuevas = new ArrayList<>();
        for (int n = 0; n <= 999; n++) {
            String matricula = String.format("20253DS%03d", n);
            if (!existentes.contains(matricula)) {
                nuevas.add(new MatriculaPermitida(matricula));
            }
        }
        if (!nuevas.isEmpty()) {
            matriculas.saveAll(nuevas);
            log.info("Se cargaron {} matrículas permitidas", nuevas.size());
        }
    }

    private void crearCuentasDelEquipo() {
        for (Integrante i : INTEGRANTES) {
            if (!estudiantes.existsByMatricula(i.matricula())) {
                Estudiante e = new Estudiante();
                e.setMatricula(i.matricula());
                e.setCorreo(i.matricula().toLowerCase() + "@utez.edu.mx");
                e.setPasswordHash(encoder.encode(PASSWORD_INICIAL));
                e.setNombreCompleto(i.nombre());
                e.setCarrera(CARRERA);
                estudiantes.save(e);
                log.info("Cuenta creada: {} ({})", i.nombre(), i.matricula());
            }
        }
    }
}
