package mx.edu.utez.credencialapi.repositorio;

import mx.edu.utez.credencialapi.modelo.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    Optional<Estudiante> findByMatricula(String matricula);

    Optional<Estudiante> findByCorreo(String correo);

    boolean existsByMatricula(String matricula);

    boolean existsByCorreo(String correo);
}
