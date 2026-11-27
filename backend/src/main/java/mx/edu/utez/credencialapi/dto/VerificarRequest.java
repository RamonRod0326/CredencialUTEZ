package mx.edu.utez.credencialapi.dto;

import jakarta.validation.constraints.NotBlank;

public record VerificarRequest(@NotBlank String matricula, @NotBlank String correo) {
}
