package org.erpklassup.erpklassup.models;

import java.time.LocalDateTime;

public class SyncLog {

    private Long idSyncLog;
    private String idEcole;
    private String nomTable;
    private String idEntity;
    private String actionEnum;
    private String clientSourceId;
    private String payloadJSON;
    private Long version;
    private LocalDateTime timestampServer;

    public SyncLog() {
        this.timestampServer = LocalDateTime.now();
    }

    public Long getIdSyncLog() { return idSyncLog; }
    public void setIdSyncLog(Long idSyncLog) { this.idSyncLog = idSyncLog; }

    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getNomTable() { return nomTable; }
    public void setNomTable(String nomTable) { this.nomTable = nomTable; }

    public String getIdEntity() { return idEntity; }
    public void setIdEntity(String idEntity) { this.idEntity = idEntity; }

    public String getActionEnum() { return actionEnum; }
    public void setActionEnum(String actionEnum) { this.actionEnum = actionEnum; }

    public String getClientSourceId() { return clientSourceId; }
    public void setClientSourceId(String clientSourceId) { this.clientSourceId = clientSourceId; }

    public String getPayloadJSON() { return payloadJSON; }
    public void setPayloadJSON(String payloadJSON) { this.payloadJSON = payloadJSON; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getTimestampServer() { return timestampServer; }
    public void setTimestampServer(LocalDateTime date) { this.timestampServer = date; }
}