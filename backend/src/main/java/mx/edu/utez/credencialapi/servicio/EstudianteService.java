package mx.edu.utez.credencialapi.servicio;

import mx.edu.utez.credencialapi.dto.EstudianteResponse;
import mx.edu.utez.credencialapi.dto.PerfilRequest;
import mx.edu.utez.credencialapi.modelo.Estudiante;
import mx.edu.utez.credencialapi.repositorio.EstudianteRepository;
import mx.edu.utez.credencialapi.repositorio.RegistroAccesoRepository;
import mx.edu.utez.credencialapi.repositorio.SesionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class EstudianteService {

    private final EstudianteRepository estudiantes;
    private final RegistroAccesoRepository accesos;
    private final SesionRepository sesiones;
    private final FotoService fotos;

    public EstudianteService(EstudianteRepository estudiantes, RegistroAccesoRepository accesos,
                             SesionRepository sesiones, FotoService fotos) {
        this.estudiantes = estudiantes;
        this.accesos = accesos;
        this.sesiones = sesiones;
        this.fotos = fotos;
    }

    @Transactional(readOnly = true)
    public EstudianteResponse obtener(Long id) {
        return EstudianteResponse.de(buscar(id));
    }

    @Transactional
    public EstudianteResponse actualizar(Long id, PerfilRequest req) {
        Estudiante e = buscar(id);
        e.setNombreCompleto(req.nombreCompleto().trim());
        e.setCarrera(req.carrera());
        e.setGustos(req.gustos() == null ? List.of() : req.gustos());
        e.setIntereses(req.intereses() == null ? List.of() : req.intereses());
        e.setDescripcion(req.descripcion() == null ? "" : req.descripcion().trim());
        return EstudianteResponse.de(estudiantes.save(e));
    }

    @Transactional
    public EstudianteResponse cambiarFoto(Long id, MultipartFile archivo) {
        Estudiante e = buscar(id);
        String anterior = e.getFoto();
        e.setFoto(fotos.guardar(archivo));
        estudiantes.save(e);
        fotos.borrar(anterior);
        return EstudianteResponse.de(e);
    }

    @Transactional
    public EstudianteResponse eliminarFoto(Long id) {
        Estudiante e = buscar(id);
        fotos.borrar(e.getFoto());
        e.setFoto(null);
        return EstudianteResponse.de(estudiantes.save(e));
    }

    @Transactional
    public void eliminarCuenta(Long id) {
        Estudiante e = buscar(id);
        fotos.borrar(e.getFoto());
        accesos.deleteByEstudianteId(id);
        sesiones.deleteByEstudianteId(id);
        estudiantes.delete(e);
    }

    Estudiante buscar(Long id) {
        return estudiantes.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No se encontró el perfil."));
    }
}
