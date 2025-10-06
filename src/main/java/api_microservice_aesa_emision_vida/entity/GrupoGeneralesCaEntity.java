package api_microservice_aesa_emision_vida.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "grupo_generales_ca")
public class GrupoGeneralesCaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codId;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "monto_cuota")
    private Double montoCuota;

    @Column(name = "franquicia")
    private Double franquicia;

    @Column(name = "dias_carencia")
    private Integer diasCarencia;

    @Column(name="operador")
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

    public Double getMontoCuota() {
        return montoCuota;
    }

    public void setMontoCuota(Double montoCuota) {
        this.montoCuota = montoCuota;
    }

    public Double getFranquicia() {
        return franquicia;
    }

    public void setFranquicia(Double franquicia) {
        this.franquicia = franquicia;
    }

    public Integer getDiasCarencia() {
        return diasCarencia;
    }

    public void setDiasCarencia(Integer diasCarencia) {
        this.diasCarencia = diasCarencia;
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
