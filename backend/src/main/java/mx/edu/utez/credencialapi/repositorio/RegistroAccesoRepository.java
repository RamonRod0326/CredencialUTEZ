package mx.edu.utez.credencialapi.repositorio;

import mx.edu.utez.credencialapi.modelo.RegistroAcceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistroAccesoRepository extends JpaRepository<RegistroAcceso, Long> {

    List<RegistroAcceso> findByEstudianteIdOrderByFechaHoraDesc(Long estudianteId);

    Optional<RegistroAcceso> findByIdAndEstudianteId(Long id, Long estudianteId);

    void deleteByEstudianteId(Long estudianteId);
}
