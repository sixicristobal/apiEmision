package api_microservice_aesa_emision_vida.dto;


import java.time.LocalDate;

public class DatosPersonasDto {
    private long cod_id;
    private int ci;
    private boolean esTitular;
    private LocalDate fechaInicioInclusion;
    private int mesVigencia;
    private int edadActual;
    private double cuotaTotal;
    private Double absorbeBanco;
    private Double absorbeFuncionario;
    private boolean estado;
    private double franquicia;
    private String operador;
    private LocalDate fechaCreacion;
    private LocalDate fechaNacimiento;
    private int ciTitular;
    private LocalDate fechaInicioVigencia;
    private LocalDate fechaFinVigencia;
    private String mail;
    private String categoriaNombre;
    private String grupoNombre;
    private String prepagaNombre;
    private String nombre;
    private String apellido;
    private String nombre_titular;
    private double capitalAsegurado;


    public double getCapitalAsegurado() {
        return capitalAsegurado;
    }

    public void setCapitalAsegurado(double capitalAsegurado) {
        this.capitalAsegurado = capitalAsegurado;
    }

    public String getNombre_titular() {
        return nombre_titular;
    }

    public void setNombre_titular(String nombre_titular) {
        this.nombre_titular = nombre_titular;
    }

    public long getCod_id() {
        return cod_id;
    }

    public void setCod_id(long cod_id) {
        this.cod_id = cod_id;
    }

    public int getCi() {
        return ci;
    }

    public void setCi(int ci) {
        this.ci = ci;
    }

    public boolean isEsTitular() {
        return esTitular;
    }

    public void setEsTitular(boolean esTitular) {
        this.esTitular = esTitular;
    }

    public LocalDate getFechaInicioInclusion() {
        return fechaInicioInclusion;
    }

    public void setFechaInicioInclusion(LocalDate fechaInicioInclusion) {
        this.fechaInicioInclusion = fechaInicioInclusion;
    }

    public int getMesVigencia() {
        return mesVigencia;
    }

    public void setMesVigencia(int mesVigencia) {
        this.mesVigencia = mesVigencia;
    }

    public int getEdadActual() {
        return edadActual;
    }

    public void setEdadActual(int edadActual) {
        this.edadActual = edadActual;
    }

    public double getCuotaTotal() {
        return cuotaTotal;
    }

    public void setCuotaTotal(double cuotaTotal) {
        this.cuotaTotal = cuotaTotal;
    }

    public Double getAbsorbeBanco() {
        return absorbeBanco;
    }

    public void setAbsorbeBanco(Double absorbeBanco) {
        this.absorbeBanco = absorbeBanco;
    }

    public Double getAbsorbeFuncionario() {
        return absorbeFuncionario;
    }

    public void setAbsorbeFuncionario(Double absorbeFuncionario) {
        this.absorbeFuncionario = absorbeFuncionario;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

    public double getFranquicia() {
        return franquicia;
    }

    public void setFranquicia(double franquicia) {
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

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public int getCiTitular() {
        return ciTitular;
    }

    public void setCiTitular(int ciTitular) {
        this.ciTitular = ciTitular;
    }

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

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getCategoriaNombre() {
        return categoriaNombre;
    }

    public void setCategoriaNombre(String categoriaNombre) {
        this.categoriaNombre = categoriaNombre;
    }

    public String getGrupoNombre() {
        return grupoNombre;
    }

    public void setGrupoNombre(String grupoNombre) {
        this.grupoNombre = grupoNombre;
    }

    public String getPrepagaNombre() {
        return prepagaNombre;
    }

    public void setPrepagaNombre(String prepagaNombre) {
        this.prepagaNombre = prepagaNombre;
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
}
