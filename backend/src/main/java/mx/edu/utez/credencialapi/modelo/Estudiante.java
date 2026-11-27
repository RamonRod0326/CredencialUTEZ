package mx.edu.utez.credencialapi.modelo;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "estudiantes")
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String matricula;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 80)
    private String nombreCompleto;

    @Column(nullable = false, length = 120)
    private String carrera;

    private String foto;

    @Column(length = 300)
    private String descripcion = "";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "estudiante_gustos", joinColumns = @JoinColumn(name = "estudiante_id"))
    @Column(name = "gusto", length = 30)
    private List<String> gustos = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "estudiante_intereses", joinColumns = @JoinColumn(name = "estudiante_id"))
    @Column(name = "interes", length = 30)
    private List<String> intereses = new ArrayList<>();

    @Column(nullable = false)
    private Instant fechaRegistro = Instant.now();

    public Long getId() { return id; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getCarrera() { return carrera; }
    public void setCarrera(String carrera) { this.carrera = carrera; }
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public List<String> getGustos() { return gustos; }
    public void setGustos(List<String> gustos) { this.gustos = new ArrayList<>(gustos); }
    public List<String> getIntereses() { return intereses; }
    public void setIntereses(List<String> intereses) { this.intereses = new ArrayList<>(intereses); }
    public Instant getFechaRegistro() { return fechaRegistro; }
}
