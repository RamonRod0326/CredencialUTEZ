package mx.edu.utez.credencialapi.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String usuario, @NotBlank String password) {
}
