package mx.edu.utez.credencialapi.dto;

public record AuthResponse(String token, EstudianteResponse estudiante) {
}
