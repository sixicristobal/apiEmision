package api_microservice_aesa_emision_vida.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_cargas")
public class AuditoriaCargaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "id")
    private Long id;

    @Column(name = "usuario")
    private String usuario;

    @Column(name = "archivo")
    private String archivo;

    @Column(name=("fechaCarga"))
    private LocalDateTime fechaCarga;

    @Column(name="resultado_api")
    private String resultadoApi;

    @Column(name="estado")
    private String estado;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getArchivo() {
        return archivo;
    }

    public void setArchivo(String archivo) {
        this.archivo = archivo;
    }

    public LocalDateTime getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(LocalDateTime fechaCarga) {
        this.fechaCarga = fechaCarga;
    }

    public String getResultadoApi() {
        return resultadoApi;
    }

    public void setResultadoApi(String resultadoApi) {
        this.resultadoApi = resultadoApi;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
