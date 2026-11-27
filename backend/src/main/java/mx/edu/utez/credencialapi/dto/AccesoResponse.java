package mx.edu.utez.credencialapi.dto;

import mx.edu.utez.credencialapi.modelo.RegistroAcceso;

public record AccesoResponse(
        Long id,
        long fechaHora,
        Double latitud,
        Double longitud,
        Float precisionMetros,
        String lectorId,
        String lectorUbicacion,
        RegistroAcceso.Resultado resultado
) {
    public static AccesoResponse de(RegistroAcceso r) {
        return new AccesoResponse(
                r.getId(),
                r.getFechaHora().toEpochMilli(),
                r.getLatitud(),
                r.getLongitud(),
                r.getPrecisionMetros(),
                r.getLectorId(),
                r.getLectorUbicacion(),
                r.getResultado()
        );
    }
}
