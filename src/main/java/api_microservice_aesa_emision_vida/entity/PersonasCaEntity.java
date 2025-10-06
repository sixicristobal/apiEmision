package api_microservice_aesa_emision_vida.entity;


import jakarta.persistence.*;
import java.time.LocalDate;


@Entity
@Table(name = "personas_ca")
public class PersonasCaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_id" )
    private Long codId;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "apellido")
    private String apellido;

    @Column(name = "ci_nro")
    private Integer ci;

    @Column(name = "ruc")
    private String ruc;

    @Column(name = "fecha_nac")
    private LocalDate fechaNac;

    @Column(name = "edad_actual")
    private Integer edadActual;

    @Column(name = "direccion_particular")
    private String direccionParticular;

    @Column(name = "emails")
    private String emails;

    @Column(name="ciudad")
    private String ciudad;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "estado")
    private String estado;

    @Column(name = "posible_cliente")
    private Boolean posibleCliente;

    @Column(name = "es_titular")
    private Boolean esTitular;

    @Column(name = "operador")
    private String operador;

    @Column(name = "fecha_create")
    private LocalDate fechaCreate;




    public Boolean getEsTitular() {
        return esTitular;
    }

    public void setEsTitular(Boolean esTitular) {
        this.esTitular = esTitular;
    }

    public Long getCodId() {
        return codId;
    }

    public void setCodId(Long codId) {
        this.codId = codId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public Integer getCi() {
        return ci;
    }

    public void setCi(Integer ci) {
        this.ci = ci;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public LocalDate getFechaNac() {
        return fechaNac;
    }

    public void setFechaNac(LocalDate fechaNac) {
        this.fechaNac = fechaNac;
    }

    public Integer getEdadActual() {
        return edadActual;
    }

    public void setEdadActual(Integer edadActual) {
        this.edadActual = edadActual;
    }

    public String getDireccionParticular() {
        return direccionParticular;
    }

    public void setDireccionParticular(String direccionParticular) {
        this.direccionParticular = direccionParticular;
    }

    public String getEmails() {
        return emails;
    }

    public void setEmails(String emails) {
        this.emails = emails;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Boolean getPosibleCliente() {
        return posibleCliente;
    }

    public void setPosibleCliente(Boolean posibleCliente) {
        this.posibleCliente = posibleCliente;
    }


    public String getOperador() {
        return operador;
    }

    public void setOperador(String operador) {
        this.operador = operador;
    }

    public LocalDate getFechaCreate() {
        return fechaCreate;
    }

    public void setFechaCreate(LocalDate fechaCreate) {
        this.fechaCreate = fechaCreate;
    }
}
