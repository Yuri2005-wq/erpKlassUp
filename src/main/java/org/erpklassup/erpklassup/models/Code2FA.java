package org.erpklassup.erpklassup.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Code2FA {

    private String idCode;
    private String idUtilisateur;
    private String codeHash;
    private String typeCode;
    private LocalDateTime dateCreation;
    private LocalDateTime dateExpiration;
    private boolean estUtilise;

    public Code2FA() {
        this.idCode = UUID.randomUUID().toString();
        this.dateCreation = LocalDateTime.now();
        this.estUtilise = false;
    }

    public String getIdCode() { return idCode; }
    public void setIdCode(String idCode) { this.idCode = idCode; }

    public String getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(String idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime date) { this.dateCreation = date; }

    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDateTime date) { this.dateExpiration = date; }

    public boolean isEstUtilise() { return estUtilise; }
    public void setEstUtilise(boolean estUtilise) { this.estUtilise = estUtilise; }

    public boolean estExpire() {
        return dateExpiration != null && LocalDateTime.now().isAfter(dateExpiration);
    }
}