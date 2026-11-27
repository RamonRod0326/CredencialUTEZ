package mx.edu.utez.credencialapi.repositorio;

import mx.edu.utez.credencialapi.modelo.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SesionRepository extends JpaRepository<Sesion, String> {

    void deleteByEstudianteId(Long estudianteId);
}
