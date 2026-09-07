package org.erpklassup.erpklassup.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class SyncConflit {

    private String idConflit;
    private String idEcole;
    private String nomTable;
    private String idEntity;
    private String clientSourceId;
    private Long versionLocale;
    private Long versionCloud;
    private String donneesLocalesJSON;
    private String donneesCloudJSON;
    private String statut;
    private LocalDateTime dateDetection;
    private String resoluPar;
    private LocalDateTime dateResolution;

    public SyncConflit() {
        this.idConflit = UUID.randomUUID().toString();
        this.statut = "EN_ATTENTE";
        this.dateDetection = LocalDateTime.now();
    }

    public String getIdConflit() { return idConflit; }
    public void setIdConflit(String idConflit) { this.idConflit = idConflit; }

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getNomTable() { return nomTable; }
    public void setNomTable(String nomTable) { this.nomTable = nomTable; }

    public String getIdEntity() { return idEntity; }
    public void setIdEntity(String idEntity) { this.idEntity = idEntity; }

    public String getClientSourceId() { return clientSourceId; }
    public void setClientSourceId(String clientSourceId) { this.clientSourceId = clientSourceId; }

    public Long getVersionLocale() { return versionLocale; }
    public void setVersionLocale(Long versionLocale) { this.versionLocale = versionLocale; }

    public Long getVersionCloud() { return versionCloud; }
    public void setVersionCloud(Long versionCloud) { this.versionCloud = versionCloud; }

    public String getDonneesLocalesJSON() { return donneesLocalesJSON; }
    public void setDonneesLocalesJSON(String json) { this.donneesLocalesJSON = json; }

    public String getDonneesCloudJSON() { return donneesCloudJSON; }
    public void setDonneesCloudJSON(String json) { this.donneesCloudJSON = json; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateDetection() { return dateDetection; }
    public void setDateDetection(LocalDateTime date) { this.dateDetection = date; }

    public String getResoluPar() { return resoluPar; }
    public void setResoluPar(String resoluPar) { this.resoluPar = resoluPar; }

    public LocalDateTime getDateResolution() { return dateResolution; }
    public void setDateResolution(LocalDateTime date) { this.dateResolution = date; }
}