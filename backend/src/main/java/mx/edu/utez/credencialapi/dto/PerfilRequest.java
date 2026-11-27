package mx.edu.utez.credencialapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PerfilRequest(
        @NotBlank @Size(max = 80) String nombreCompleto,
        @NotBlank String carrera,
        @Size(max = 10) List<@Size(max = 30) String> gustos,
        @Size(max = 10) List<@Size(max = 30) String> intereses,
        @Size(max = 300) String descripcion
) {
}
