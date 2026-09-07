package org.erpklassup.erpklassup.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class TentativeConnexion {

    private String idTentative;
    private String usernameSaisi;
    private String adresseIp;
    private String userAgent;
    private boolean succes;
    private String motifEchec;
    private LocalDateTime dateTentative;

    public TentativeConnexion() {
        this.idTentative = UUID.randomUUID().toString();
        this.dateTentative = LocalDateTime.now();
    }

    public String getIdTentative() { return idTentative; }
    public void setIdTentative(String idTentative) { this.idTentative = idTentative; }

    public String getUsernameSaisi() { return usernameSaisi; }
    public void setUsernameSaisi(String usernameSaisi) { this.usernameSaisi = usernameSaisi; }

    public String getAdresseIp() { return adresseIp; }
    public void setAdresseIp(String adresseIp) { this.adresseIp = adresseIp; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public boolean isSucces() { return succes; }
    public void setSucces(boolean succes) { this.succes = succes; }

    public String getMotifEchec() { return motifEchec; }
    public void setMotifEchec(String motifEchec) { this.motifEchec = motifEchec; }

    public LocalDateTime getDateTentative() { return dateTentative; }
    public void setDateTentative(LocalDateTime date) { this.dateTentative = date; }
}