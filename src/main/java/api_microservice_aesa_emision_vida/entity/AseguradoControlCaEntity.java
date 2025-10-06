package api_microservice_aesa_emision_vida.entity;

import api_microservice_aesa_emision_vida.repository.PrepagaCaRepository;
import jakarta.persistence.*;
import org.springframework.context.annotation.AnnotationScopeMetadataResolver;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "asegurado_control_ca")
public class AseguradoControlCaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_id")
    private Long codId;

    @ManyToOne
    @JoinColumn(name = "cod_id_persona")
    private PersonasCaEntity persona;

    @ManyToOne
    @JoinColumn(name = "cod_id_categoria")
    private CategoriasCAEntity categoria;

    @ManyToOne
    @JoinColumn(name = "cod_id_grupo_generales")
    private GrupoGeneralesCaEntity grupoGeneral;

    @ManyToOne
    @JoinColumn(name = "cod_id_prepaga")
    private PrepagaCaEntity prepaga;

    @Column(name = "ci")
    private Integer ci;

    @Column(name = "es_titular")
    private Boolean esTitular;

    @Column(name = "fecha_inicio_inclusion")
    private LocalDate fechaInicioInclusion;

    @Column(name = "mes_vigencia")
    private Integer mesVigencia;

    @Column(name = "edad_actual")
    private Integer edadActual;

    @Column(name = "cuota_total")
    private Double cuotaTotal;

    @Column(name = "absorbe_banco")
    private Double  absorbeBanco;

    @Column(name = "absorbe_funcionario")
    private Double  absorbeFuncionario;

    @Column(name = "estado")
    private Boolean estado;

    @Column(name = "franquicia")
    private Double franquicia;

    @Column(name = "operador")
    private String operador;

    @Column(name = "fecha_creacion")
    private LocalDate fechaCreacion;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechanacimiento;

    @Column (name = "ci_titular")
    private Integer ciTitular;

    @Column (name = "fecha_inicio_vigencia")
    private LocalDate fechaInicioVigencia;

    @Column (name = "fecha_fin_vigencia")
    private LocalDate fechaFinVigencia;

    @Column (name = "email")
    private String mail;

    @Column (name = "anio_vigencia")
    private Integer anioVigencia;



    public LocalDate getFechaInicioVigencia() {
        return fechaInicioVigencia;
    }

    public void setFechaInicioVigencia(LocalDate fechaInicioVigencia) {
        this.fechaInicioVigencia = fechaInicioVigencia;
    }

    public LocalDate getFechaFinVigencia() {
        return fechaFinVigencia;
    }

    public void setFechaFinVigencia(LocalDate fechaFinVigencia) {
        this.fechaFinVigencia = fechaFinVigencia;
    }

    public LocalDate getFechanacimiento() {
        return fechanacimiento;
    }

    public void setFechanacimiento(LocalDate fechanacimiento) {
        this.fechanacimiento = fechanacimiento;
    }

    public Integer getCiTitular() {
        return ciTitular;
    }

    public void setCiTitular(Integer ciTitular) {
        this.ciTitular = ciTitular;
    }

    public Long getCodId() {
        return codId;
    }

    public void setCodId(Long codId) {
        this.codId = codId;
    }

    public PersonasCaEntity getPersona() {
        return persona;
    }

    public void setPersona(PersonasCaEntity persona) {
        this.persona = persona;
    }

    public CategoriasCAEntity getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriasCAEntity categoria) {
        this.categoria = categoria;
    }

    public GrupoGeneralesCaEntity getGrupoGeneral() {
        return grupoGeneral;
    }

    public void setGrupoGeneral(GrupoGeneralesCaEntity grupoGeneral) {
        this.grupoGeneral = grupoGeneral;
    }

    public PrepagaCaEntity getPrepaga() {
        return prepaga;
    }

    public void setPrepaga(PrepagaCaEntity prepaga) {
        this.prepaga = prepaga;
    }

    public Integer getCi() {
        return ci;
    }

    public void setCi(Integer ci) {
        this.ci = ci;
    }

    public Boolean getEsTitular() {
        return esTitular;
    }

    public void setEsTitular(Boolean esTitular) {
        this.esTitular = esTitular;
    }

    public LocalDate getFechaInicioInclusion() {
        return fechaInicioInclusion;
    }

    public void setFechaInicioInclusion(LocalDate fechaInicioInclusion) {
        this.fechaInicioInclusion = fechaInicioInclusion;
    }

    public Integer getMesVigencia() {
        return mesVigencia;
    }

    public void setMesVigencia(Integer mesVigencia) {
        this.mesVigencia = mesVigencia;
    }

    public Integer getEdadActual() {
        return edadActual;
    }

    public void setEdadActual(Integer edadActual) {
        this.edadActual = edadActual;
    }

    public Double getCuotaTotal() {
        return cuotaTotal;
    }

    public void setCuotaTotal(Double cuotaTotal) {
        this.cuotaTotal = cuotaTotal;
    }

    public Double getAbsorbeBanco() {
        return absorbeBanco;
    }

    public void setAbsorbeBanco(Double absorbeBanco) {
        this.absorbeBanco = absorbeBanco;
    }

    public Double  getAbsorbeFuncionario() {
        return absorbeFuncionario;
    }

    public void setAbsorbeFuncionario(Double absorbeFuncionario) {
        this.absorbeFuncionario = absorbeFuncionario;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }

    public Double getFranquicia() {
        return franquicia;
    }

    public void setFranquicia(Double franquicia) {
        this.franquicia = franquicia;
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

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public Integer getAnioVigencia() {
        return anioVigencia;
    }

    public void setAnioVigencia(Integer anioVigencia) {
        this.anioVigencia = anioVigencia;
    }
}
