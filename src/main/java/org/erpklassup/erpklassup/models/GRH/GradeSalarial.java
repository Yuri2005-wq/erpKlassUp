package org.erpklassup.erpklassup.models.GRH;
import org.erpklassup.erpklassup.models.BaseEntity;

import java.math.BigDecimal;

public class GradeSalarial extends BaseEntity {

    private String idEcole;
    private String code;
    private String libelle;
    private String categorie;
    private String echelon;
    private BigDecimal salaireBaseReference;

    public GradeSalarial() {
        super();
        this.salaireBaseReference = BigDecimal.ZERO;
    }

    public String getIdGrade() { return getId(); }
    public void setIdGrade(String id) { setId(id); }

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getEchelon() { return echelon; }
    public void setEchelon(String echelon) { this.echelon = echelon; }

    public BigDecimal getSalaireBaseReference() { return salaireBaseReference; }
    public void setSalaireBaseReference(BigDecimal s) { this.salaireBaseReference = s; }

    public String getCodeLibelle() {
        return (code != null ? code + " — " : "") + (libelle != null ? libelle : "");
    }

    @Override
    public String toString() { return getCodeLibelle(); }
}
