package org.erpklassup.erpklassup.dto;

public record FiltreUtilisateur(String idEcole, String idRole, StatutCompte statut, String terme, int page, int taillePage) {
    public static final int TAILLE_PAGE_DEFAUT = 20;
    public FiltreUtilisateur {
        if (page < 1) page = 1;
        if (taillePage < 1) taillePage = TAILLE_PAGE_DEFAUT;
    }
    public int offset() { return (page - 1) * taillePage; }
}