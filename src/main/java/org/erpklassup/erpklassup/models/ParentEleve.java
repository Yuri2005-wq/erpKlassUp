package org.erpklassup.erpklassup.models;

public class ParentEleve extends BaseEntity {

    public enum LienParente {
        PERE, MERE, TUTEUR_LEGAL, AUTRE
    }

    private String idEcole;
    private String idParent;
    private String idEleve;
    private LienParente lienParente = LienParente.PERE;
    private boolean contactPrincipal;
    private boolean responsableFinancier;

    public ParentEleve() {
        super();
    }

    public ParentEleve(String idEcole, String idParent, String idEleve, LienParente lienParente) {
        super();
        this.idEcole = idEcole;
        this.idParent = idParent;
        this.idEleve = idEleve;
        this.lienParente = lienParente;
    }

    public String getIdParentEleve() { return getId(); }
    public void setIdParentEleve(String idParentEleve) { setId(idParentEleve); }

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getIdParent() { return idParent; }
    public void setIdParent(String idParent) { this.idParent = idParent; }

    public String getIdEleve() { return idEleve; }
    public void setIdEleve(String idEleve) { this.idEleve = idEleve; }

    public LienParente getLienParente() { return lienParente; }
    public void setLienParente(LienParente lienParente) { this.lienParente = lienParente; }

    public boolean isContactPrincipal() { return contactPrincipal; }
    public void setContactPrincipal(boolean contactPrincipal) { this.contactPrincipal = contactPrincipal; }

    public boolean isResponsableFinancier() { return responsableFinancier; }
    public void setResponsableFinancier(boolean responsableFinancier) { this.responsableFinancier = responsableFinancier; }
}