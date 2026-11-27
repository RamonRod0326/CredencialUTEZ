package mx.edu.utez.credencialapi.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "matriculas_permitidas")
public class MatriculaPermitida {

    @Id
    @Column(length = 10)
    private String matricula;

    @Column(nullable = false)
    private boolean activa = true;

    protected MatriculaPermitida() {
    }

    public MatriculaPermitida(String matricula) {
        this.matricula = matricula;
    }

    public String getMatricula() { return matricula; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
