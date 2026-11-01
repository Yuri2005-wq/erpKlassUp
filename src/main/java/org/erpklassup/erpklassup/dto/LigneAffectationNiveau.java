package org.erpklassup.erpklassup.dto;

import java.math.BigDecimal;

// Une ligne de l'accordeon "Matieres et programmes" : un niveau (eventuellement
// une filiere pour le secondaire), le coefficient saisi et l'etat de la case
// a cocher. coche=true => la matiere est affectee (et obligatoire) a ce niveau ;
// coche=false => aucune affectation ne doit exister pour ce niveau.
public class LigneAffectationNiveau {

    private final String idNiveau;
    private final String idFiliere;
    private final BigDecimal coefficient;
    private final boolean coche;

    public LigneAffectationNiveau(String idNiveau, String idFiliere, BigDecimal coefficient, boolean coche) {
        this.idNiveau = idNiveau;
        this.idFiliere = idFiliere;
        this.coefficient = coefficient;
        this.coche = coche;
    }

    public String getIdNiveau() { return idNiveau; }
    public String getIdFiliere() { return idFiliere; }
    public BigDecimal getCoefficient() { return coefficient; }
    public boolean isCoche() { return coche; }
}