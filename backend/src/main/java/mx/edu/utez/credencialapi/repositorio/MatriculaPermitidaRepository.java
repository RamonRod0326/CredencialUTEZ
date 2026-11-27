package mx.edu.utez.credencialapi.repositorio;

import mx.edu.utez.credencialapi.modelo.MatriculaPermitida;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatriculaPermitidaRepository extends JpaRepository<MatriculaPermitida, String> {

    boolean existsByMatriculaAndActivaTrue(String matricula);
}
