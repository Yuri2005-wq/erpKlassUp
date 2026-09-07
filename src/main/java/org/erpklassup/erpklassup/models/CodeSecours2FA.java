package org.erpklassup.erpklassup.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class CodeSecours2FA {

    private String idCodeSecours;
    private String idUtilisateur;
    private String codeHash;
    private boolean estUtilise;
    private LocalDateTime dateUtilisation;

    public CodeSecours2FA() {
        this.idCodeSecours = UUID.randomUUID().toString();
        this.estUtilise = false;
    }

    public String getIdCodeSecours() { return idCodeSecours; }
    public void setIdCodeSecours(String idCodeSecours) { this.idCodeSecours = idCodeSecours; }

    public String getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(String idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }

    public boolean isEstUtilise() { return estUtilise; }
    public void setEstUtilise(boolean estUtilise) { this.estUtilise = estUtilise; }

    public LocalDateTime getDateUtilisation() { return dateUtilisation; }
    public void setDateUtilisation(LocalDateTime date) { this.dateUtilisation = date; }
}