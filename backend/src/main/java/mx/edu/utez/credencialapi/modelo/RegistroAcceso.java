package mx.edu.utez.credencialapi.modelo;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "registros_acceso")
public class RegistroAcceso {

    public enum Resultado { PERMITIDO, DENEGADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @Column(nullable = false)
    private Instant fechaHora;

    private Double latitud;
    private Double longitud;
    private Float precisionMetros;

    @Column(length = 20)
    private String lectorId;

    @Column(length = 80)
    private String lectorUbicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Resultado resultado;

    public Long getId() { return id; }
    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }
    public Instant getFechaHora() { return fechaHora; }
    public void setFechaHora(Instant fechaHora) { this.fechaHora = fechaHora; }
    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
    public Float getPrecisionMetros() { return precisionMetros; }
    public void setPrecisionMetros(Float precisionMetros) { this.precisionMetros = precisionMetros; }
    public String getLectorId() { return lectorId; }
    public void setLectorId(String lectorId) { this.lectorId = lectorId; }
    public String getLectorUbicacion() { return lectorUbicacion; }
    public void setLectorUbicacion(String lectorUbicacion) { this.lectorUbicacion = lectorUbicacion; }
    public Resultado getResultado() { return resultado; }
    public void setResultado(Resultado resultado) { this.resultado = resultado; }
}
