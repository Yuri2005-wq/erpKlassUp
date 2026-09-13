package org.erpklassup.erpklassup.dto;

import java.time.LocalDateTime;

public record UtilisateurLigne(
        String idUtilisateur,
        String username,
        String nom,
        String prenom,
        String nomComplet,
        String email,
        String telephone,
        String typeUtilisateur,
        boolean doitChangerMotDePasse, // Ajout du champ manquant
        String roles,
        String photoPath,
        boolean actif,
        boolean verrouille,
        boolean deuxFacteursActif,
        LocalDateTime derniereConnexion
) {
    public StatutCompte statut() {
        if (verrouille) return StatutCompte.VERROUILLE;
        if (!actif) return StatutCompte.INACTIF;
        return StatutCompte.ACTIF;
    }
}
//package org.erpklassup.erpklassup.dto;
//
//public record UtilisateurLigne(
//        String idUtilisateur, String username, String nom, String prenom, String nomComplet,
//        String email, String telephone, String roles, String photoPath,
//        boolean actif, boolean verrouille, boolean deuxFacteursActif, String derniereConnexion, String typeUtilisateur
//) {
//    public StatutCompte statut() {
//        if (verrouille) return StatutCompte.VERROUILLE;
//        if (!actif) return StatutCompte.INACTIF;
//        return StatutCompte.ACTIF;
//    }
//}