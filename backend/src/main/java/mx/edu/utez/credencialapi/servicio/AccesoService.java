package mx.edu.utez.credencialapi.servicio;

import mx.edu.utez.credencialapi.dto.AccesoRequest;
import mx.edu.utez.credencialapi.dto.AccesoResponse;
import mx.edu.utez.credencialapi.modelo.RegistroAcceso;
import mx.edu.utez.credencialapi.repositorio.RegistroAccesoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AccesoService {

    private final RegistroAccesoRepository accesos;
    private final EstudianteService estudiantes;

    public AccesoService(RegistroAccesoRepository accesos, EstudianteService estudiantes) {
        this.accesos = accesos;
        this.estudiantes = estudiantes;
    }

    @Transactional(readOnly = true)
    public List<AccesoResponse> listar(Long estudianteId) {
        return accesos.findByEstudianteIdOrderByFechaHoraDesc(estudianteId).stream()
                .map(AccesoResponse::de)
                .toList();
    }

    @Transactional
    public AccesoResponse registrar(Long estudianteId, AccesoRequest req) {
        RegistroAcceso r = new RegistroAcceso();
        r.setEstudiante(estudiantes.buscar(estudianteId));
        r.setFechaHora(Instant.ofEpochMilli(req.fechaHora()));
        r.setLatitud(req.latitud());
        r.setLongitud(req.longitud());
        r.setPrecisionMetros(req.precisionMetros());
        r.setLectorId(req.lectorId());
        r.setLectorUbicacion(req.lectorUbicacion());
        r.setResultado(req.resultado());
        return AccesoResponse.de(accesos.save(r));
    }

    @Transactional
    public void eliminar(Long estudianteId, Long accesoId) {
        RegistroAcceso r = accesos.findByIdAndEstudianteId(accesoId, estudianteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el registro."));
        accesos.delete(r);
    }
}
