package org.erpklassup.erpklassup.models;

import java.time.LocalDateTime;

public class SyncClient {

    private String idClient;
    private String idEcole;
    private String nomMachine;
    private LocalDateTime derniereSynchro;
    private Long dernierSyncLogId;
    private String derniereAdresseIP;
    private boolean estActif;

    public SyncClient() {
        this.dernierSyncLogId = 0L;
        this.estActif = true;
    }

    public String getIdClient() { return idClient; }
    public void setIdClient(String idClient) { this.idClient = idClient; }

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getNomMachine() { return nomMachine; }
    public void setNomMachine(String nomMachine) { this.nomMachine = nomMachine; }

    public LocalDateTime getDerniereSynchro() { return derniereSynchro; }
    public void setDerniereSynchro(LocalDateTime date) { this.derniereSynchro = date; }

    public Long getDernierSyncLogId() { return dernierSyncLogId; }
    public void setDernierSyncLogId(Long dernierSyncLogId) { this.dernierSyncLogId = dernierSyncLogId; }

    public String getDerniereAdresseIP() { return derniereAdresseIP; }
    public void setDerniereAdresseIP(String ip) { this.derniereAdresseIP = ip; }

    public boolean isEstActif() { return estActif; }
    public void setEstActif(boolean estActif) { this.estActif = estActif; }
}