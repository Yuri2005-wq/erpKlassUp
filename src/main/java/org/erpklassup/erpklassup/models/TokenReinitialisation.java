package org.erpklassup.erpklassup.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class TokenReinitialisation {

    private String idToken;
    private String idUtilisateur;
    private String tokenHash;
    private LocalDateTime dateCreation;
    private LocalDateTime dateExpiration;
    private boolean estUtilise;
    private LocalDateTime dateUtilisation;
    private String adresseIpDemande;

    public TokenReinitialisation() {
        this.idToken = UUID.randomUUID().toString();
        this.dateCreation = LocalDateTime.now();
        this.estUtilise = false;
    }

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }

    public String getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(String idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime date) { this.dateCreation = date; }

    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDateTime date) { this.dateExpiration = date; }

    public boolean isEstUtilise() { return estUtilise; }
    public void setEstUtilise(boolean estUtilise) { this.estUtilise = estUtilise; }

    public LocalDateTime getDateUtilisation() { return dateUtilisation; }
    public void setDateUtilisation(LocalDateTime date) { this.dateUtilisation = date; }

    public String getAdresseIpDemande() { return adresseIpDemande; }
    public void setAdresseIpDemande(String ip) { this.adresseIpDemande = ip; }

    public boolean estExpire() {
        return dateExpiration != null && LocalDateTime.now().isAfter(dateExpiration);
    }
}