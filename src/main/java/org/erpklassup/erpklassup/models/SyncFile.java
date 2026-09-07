package org.erpklassup.erpklassup.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class SyncFile {

    private String idFile;
    private String idClient;
    private String idEcole;
    private String nomTable;
    private String idEntity;
    private String actionEnum;
    private String donneesJSON;
    private Long versionClient;
    private String statut;
    private String messageErreur;
    private LocalDateTime dateCreation;
    private LocalDateTime dateEnvoi;
    private int nombreTentatives;

    public SyncFile() {
        this.idFile = UUID.randomUUID().toString();
        this.statut = "EN_ATTENTE";
        this.dateCreation = LocalDateTime.now();
        this.nombreTentatives = 0;
    }

    public String getIdFile() { return idFile; }
    public void setIdFile(String idFile) { this.idFile = idFile; }

    public String getIdClient() { return idClient; }
    public void setIdClient(String idClient) { this.idClient = idClient; }

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getNomTable() { return nomTable; }
    public void setNomTable(String nomTable) { this.nomTable = nomTable; }

    public String getIdEntity() { return idEntity; }
    public void setIdEntity(String idEntity) { this.idEntity = idEntity; }

    public String getActionEnum() { return actionEnum; }
    public void setActionEnum(String actionEnum) { this.actionEnum = actionEnum; }

    public String getDonneesJSON() { return donneesJSON; }
    public void setDonneesJSON(String donneesJSON) { this.donneesJSON = donneesJSON; }

    public Long getVersionClient() { return versionClient; }
    public void setVersionClient(Long versionClient) { this.versionClient = versionClient; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getMessageErreur() { return messageErreur; }
    public void setMessageErreur(String messageErreur) { this.messageErreur = messageErreur; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime date) { this.dateCreation = date; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime date) { this.dateEnvoi = date; }

    public int getNombreTentatives() { return nombreTentatives; }
    public void setNombreTentatives(int nombreTentatives) { this.nombreTentatives = nombreTentatives; }
}