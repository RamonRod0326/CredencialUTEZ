package mx.edu.utez.credencialapi.dto;

import mx.edu.utez.credencialapi.modelo.Estudiante;

import java.util.List;

public record EstudianteResponse(
        Long id,
        String nombreCompleto,
        String matricula,
        String correo,
        String carrera,
        String fotoUrl,
        List<String> gustos,
        List<String> intereses,
        String descripcion,
        long fechaRegistro
) {
    public static EstudianteResponse de(Estudiante e) {
        return new EstudianteResponse(
                e.getId(),
                e.getNombreCompleto(),
                e.getMatricula(),
                e.getCorreo(),
                e.getCarrera(),
                e.getFoto() == null ? null : "/api/fotos/" + e.getFoto(),
                List.copyOf(e.getGustos()),
                List.copyOf(e.getIntereses()),
                e.getDescripcion() == null ? "" : e.getDescripcion(),
                e.getFechaRegistro().toEpochMilli()
        );
    }
}
