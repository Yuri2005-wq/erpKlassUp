package org.erpklassup.erpklassup.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class SessionUtilisateur {

    private String idSession;
    private String idUtilisateur;
    private String idEcoleActive;
    private String idGroupeActif;
    private String accessTokenHash;
    private String refreshTokenHash;
    private String familleToken;
    private String typeSession;
    private String adresseIp;
    private String userAgent;
    private String nomAppareil;
    private String typeAppareil;
    private String systemeExploitation;
    private String navigateur;
    private String localisation;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDerniereActivite;
    private LocalDateTime dateExpirationAccess;
    private LocalDateTime dateExpirationRefresh;
    private LocalDateTime dateDeconnexion;
    private String motifDeconnexion;
    private boolean estRevoque;

    public SessionUtilisateur() {
        this.idSession = UUID.randomUUID().toString();
        this.dateCreation = LocalDateTime.now();
        this.dateDerniereActivite = LocalDateTime.now();
        this.estRevoque = false;
        this.typeAppareil = "DESKTOP";
    }

    public String getIdSession() { return idSession; }
    public void setIdSession(String idSession) { this.idSession = idSession; }

    public String getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(String idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getIdEcoleActive() { return idEcoleActive; }
    public void setIdEcoleActive(String idEcoleActive) { this.idEcoleActive = idEcoleActive; }

    public String getIdGroupeActif() { return idGroupeActif; }
    public void setIdGroupeActif(String idGroupeActif) { this.idGroupeActif = idGroupeActif; }

    public String getAccessTokenHash() { return accessTokenHash; }
    public void setAccessTokenHash(String accessTokenHash) { this.accessTokenHash = accessTokenHash; }

    public String getRefreshTokenHash() { return refreshTokenHash; }
    public void setRefreshTokenHash(String refreshTokenHash) { this.refreshTokenHash = refreshTokenHash; }

    public String getFamilleToken() { return familleToken; }
    public void setFamilleToken(String familleToken) { this.familleToken = familleToken; }

    public String getTypeSession() { return typeSession; }
    public void setTypeSession(String typeSession) { this.typeSession = typeSession; }

    public String getAdresseIp() { return adresseIp; }
    public void setAdresseIp(String adresseIp) { this.adresseIp = adresseIp; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getNomAppareil() { return nomAppareil; }
    public void setNomAppareil(String nomAppareil) { this.nomAppareil = nomAppareil; }

    public String getTypeAppareil() { return typeAppareil; }
    public void setTypeAppareil(String typeAppareil) { this.typeAppareil = typeAppareil; }

    public String getSystemeExploitation() { return systemeExploitation; }
    public void setSystemeExploitation(String systemeExploitation) { this.systemeExploitation = systemeExploitation; }

    public String getNavigateur() { return navigateur; }
    public void setNavigateur(String navigateur) { this.navigateur = navigateur; }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime date) { this.dateCreation = date; }

    public LocalDateTime getDateDerniereActivite() { return dateDerniereActivite; }
    public void setDateDerniereActivite(LocalDateTime date) { this.dateDerniereActivite = date; }

    public LocalDateTime getDateExpirationAccess() { return dateExpirationAccess; }
    public void setDateExpirationAccess(LocalDateTime date) { this.dateExpirationAccess = date; }

    public LocalDateTime getDateExpirationRefresh() { return dateExpirationRefresh; }
    public void setDateExpirationRefresh(LocalDateTime date) { this.dateExpirationRefresh = date; }

    public LocalDateTime getDateDeconnexion() { return dateDeconnexion; }
    public void setDateDeconnexion(LocalDateTime date) { this.dateDeconnexion = date; }

    public String getMotifDeconnexion() { return motifDeconnexion; }
    public void setMotifDeconnexion(String motif) { this.motifDeconnexion = motif; }

    public boolean isEstRevoque() { return estRevoque; }
    public void setEstRevoque(boolean estRevoque) { this.estRevoque = estRevoque; }
}