package mx.edu.utez.credencialapi.modelo;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "sesiones")
public class Sesion {

    @Id
    @Column(length = 64)
    private String token;

    @Column(nullable = false)
    private Long estudianteId;

    @Column(nullable = false)
    private Instant creada = Instant.now();

    protected Sesion() {
    }

    public Sesion(String token, Long estudianteId) {
        this.token = token;
        this.estudianteId = estudianteId;
    }

    public String getToken() { return token; }
    public Long getEstudianteId() { return estudianteId; }
    public Instant getCreada() { return creada; }
}
