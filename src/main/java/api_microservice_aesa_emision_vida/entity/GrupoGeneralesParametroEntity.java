package api_microservice_aesa_emision_vida.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "grupo_generales_parametro_ca")
public class GrupoGeneralesParametroEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "banco")
    private String banco;

    @Column(name = "monto_cuota")
    private double montoCuota;

    @Column(name = "grupo_id")
    private int grupoId;

    @Column(name = "capital_asegurado")
    private double capitalAsegurado;

//    @Column(name = "absorbe_funcionario_100")
//    private boolean absorbeFuncionario100;
//
//    @Column(name = "")
//
//    public Boolean isAbsorbeFuncionario100(){
//        return absorbeFuncionario100;
//    }
//
//    public void setAbsorbeFuncionario100(boolean absorbeFuncionario100) {
//        this.absorbeFuncionario100 = absorbeFuncionario100;
//    }


    public double getCapitalAsegurado() {
        return capitalAsegurado;
    }

    public void setCapitalAsegurado(double capitalAsegurado) {
        this.capitalAsegurado = capitalAsegurado;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public double getMontoCuota() {
        return montoCuota;
    }

    public void setMontoCuota(double montoCuota) {
        this.montoCuota = montoCuota;
    }

    public int getGrupoId() {
        return grupoId;
    }

    public void setGrupoId(int grupoId) {
        this.grupoId = grupoId;
    }
}