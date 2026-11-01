package org.erpklassup.erpklassup.dto;

import java.util.List;

// Bilan d'une operation d'enregistrement/retrait en masse sur matiereNiveau.
// Le controller s'en sert pour afficher un toast de succes ou la liste des
// erreurs a l'utilisateur, sans avoir a interpreter des booleens epars.
public class ResultatAffectationsNiveau {

    private final int enregistrees;
    private final int retirees;
    private final List<String> erreurs;

    public ResultatAffectationsNiveau(int enregistrees, int retirees, List<String> erreurs) {
        this.enregistrees = enregistrees;
        this.retirees = retirees;
        this.erreurs = erreurs;
    }

    public int getEnregistrees() { return enregistrees; }
    public int getRetirees() { return retirees; }
    public List<String> getErreurs() { return erreurs; }

    public boolean estSucces() { return erreurs.isEmpty(); }
}