package org.erpklassup.erpklassup.models;

import java.time.LocalDateTime;

public class Utilisateur extends BaseEntity {

    private String idEcole;
    private String idGroupePrincipal;
    // ❌ idRole ENLEVÉ ! (remplacé par UtilisateurRole)
    private String username;
    private String passwordHash;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private boolean emailVerifie;
    private String hashTokenVerificationEmail;
    private LocalDateTime dateVerificationEmail;
    private LocalDateTime dateChangementMotDePasse;
    private boolean doitChangerMotDePasse;
    private String languePreference;
    private String fuseauHoraire;
    private String secret2FA;
    private String telephone2FA;
    private boolean deuxFacteursActive;
    private String typeUtilisateur;
    private boolean compteVerrouille;
    private LocalDateTime verrouilleJusqua;
    private int nombreTentativesEchec;
    private LocalDateTime dateDernierEchec;
    private boolean estActif;

    public Utilisateur() {
        super();
        this.emailVerifie = false;
        this.doitChangerMotDePasse = false;
        this.deuxFacteursActive = false;
        this.typeUtilisateur = "PERSONNEL";
        this.compteVerrouille = false;
        this.nombreTentativesEchec = 0;
        this.languePreference = "FR";
        this.fuseauHoraire = "Africa/Douala";
        this.dateChangementMotDePasse = LocalDateTime.now();
        this.estActif = true;
    }

    public Utilisateur(String username, String nom) {
        this();
        this.username = username;
        this.nom = nom;
    }

    // Alias ID
    public String getIdUtilisateur() { return getId(); }
    public void setIdUtilisateur(String idUtilisateur) { setId(idUtilisateur); }

    // Getters et Setters
    public String getIdEcole() { return idEcole; }
    public void setIdEcole(String idEcole) { this.idEcole = idEcole; }

    public String getIdGroupePrincipal() { return idGroupePrincipal; }
    public void setIdGroupePrincipal(String idGroupePrincipal) { this.idGroupePrincipal = idGroupePrincipal; }

    // ❌ getIdRole() / setIdRole() ENLEVÉS !

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public boolean isEmailVerifie() { return emailVerifie; }
    public void setEmailVerifie(boolean emailVerifie) { this.emailVerifie = emailVerifie; }

    public String getHashTokenVerificationEmail() { return hashTokenVerificationEmail; }
    public void setHashTokenVerificationEmail(String hashToken) { this.hashTokenVerificationEmail = hashToken; }

    public LocalDateTime getDateVerificationEmail() { return dateVerificationEmail; }
    public void setDateVerificationEmail(LocalDateTime date) { this.dateVerificationEmail = date; }

    public LocalDateTime getDateChangementMotDePasse() { return dateChangementMotDePasse; }
    public void setDateChangementMotDePasse(LocalDateTime date) { this.dateChangementMotDePasse = date; }

    public boolean isDoitChangerMotDePasse() { return doitChangerMotDePasse; }
    public void setDoitChangerMotDePasse(boolean doitChanger) { this.doitChangerMotDePasse = doitChanger; }

    public String getLanguePreference() { return languePreference; }
    public void setLanguePreference(String languePreference) { this.languePreference = languePreference; }

    public String getFuseauHoraire() { return fuseauHoraire; }
    public void setFuseauHoraire(String fuseauHoraire) { this.fuseauHoraire = fuseauHoraire; }

    public String getSecret2FA() { return secret2FA; }
    public void setSecret2FA(String secret2FA) { this.secret2FA = secret2FA; }

    public String getTelephone2FA() { return telephone2FA; }
    public void setTelephone2FA(String telephone2FA) { this.telephone2FA = telephone2FA; }

    public boolean isDeuxFacteursActive() { return deuxFacteursActive; }
    public void setDeuxFacteursActive(boolean deuxFacteursActive) { this.deuxFacteursActive = deuxFacteursActive; }

    public String getTypeUtilisateur() { return typeUtilisateur; }
    public void setTypeUtilisateur(String typeUtilisateur) { this.typeUtilisateur = typeUtilisateur; }

    public boolean isCompteVerrouille() { return compteVerrouille; }
    public void setCompteVerrouille(boolean compteVerrouille) { this.compteVerrouille = compteVerrouille; }

    public LocalDateTime getVerrouilleJusqua() { return verrouilleJusqua; }
    public void setVerrouilleJusqua(LocalDateTime verrouilleJusqua) { this.verrouilleJusqua = verrouilleJusqua; }

    public int getNombreTentativesEchec() { return nombreTentativesEchec; }
    public void setNombreTentativesEchec(int nombre) { this.nombreTentativesEchec = nombre; }

    public LocalDateTime getDateDernierEchec() { return dateDernierEchec; }
    public void setDateDernierEchec(LocalDateTime date) { this.dateDernierEchec = date; }

    public boolean isEstActif() { return estActif; }
    public void setEstActif(boolean estActif) { this.estActif = estActif; }

    public String getNomComplet() {
        if (prenom != null && !prenom.isEmpty()) return prenom + " " + nom;
        return nom;
    }

    @Override
    public String toString() { return getNomComplet(); }
}