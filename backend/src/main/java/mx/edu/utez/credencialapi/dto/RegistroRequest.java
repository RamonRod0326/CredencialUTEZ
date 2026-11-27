package mx.edu.utez.credencialapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegistroRequest(
        @NotBlank @Size(max = 80) String nombreCompleto,
        @NotBlank @Pattern(regexp = "(?i)^20253DS\\d{3}$", message = "La matrícula debe tener el formato 20253DSxxx.") String matricula,
        @NotBlank @Email(message = "Escribe un correo válido.") String correo,
        @NotBlank @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres.") String password,
        @NotBlank String carrera,
        @Size(max = 10) List<@Size(max = 30) String> gustos,
        @Size(max = 10) List<@Size(max = 30) String> intereses,
        @Size(max = 300) String descripcion
) {
}
