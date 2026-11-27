package mx.edu.utez.credencialapi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mx.edu.utez.credencialapi.modelo.RegistroAcceso;

public record AccesoRequest(
        @NotNull Long fechaHora,
        Double latitud,
        Double longitud,
        Float precisionMetros,
        @Size(max = 20) String lectorId,
        @Size(max = 80) String lectorUbicacion,
        @NotNull RegistroAcceso.Resultado resultado
) {
}
