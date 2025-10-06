package api_microservice_aesa_emision_vida.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "categorias_ca")
public class CategoriasCAEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codId;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "operador")
    private String operador;

    @Column(name = "fecha_creacion")
    private LocalDate fechaCreacion;

    public Integer getCodId() {
        return codId;
    }

    public void setCodId(Integer codId) {
        this.codId = codId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getOperador() {
        return operador;
    }

    public void setOperador(String operador) {
        this.operador = operador;
    }

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
