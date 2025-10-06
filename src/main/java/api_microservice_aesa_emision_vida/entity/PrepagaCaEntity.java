package api_microservice_aesa_emision_vida.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "prepaga_ca")
public class PrepagaCaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_id")
    private Integer codId;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "fecha_creacion")
    private LocalDate fechaCreacion;

    @Column(name = "operador")
    private String operador;

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

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getOperador() {
        return operador;
    }

    public void setOperador(String operador) {
        this.operador = operador;
    }
}
