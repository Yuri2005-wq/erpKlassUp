package org.erpklassup.erpklassup.service.module2;

import org.erpklassup.erpklassup.dao.module2.MatiereNiveauDAO;
import org.erpklassup.erpklassup.dto.LigneAffectationNiveau;
import org.erpklassup.erpklassup.dto.ResultatAffectationsNiveau;
import org.erpklassup.erpklassup.models.module2.MatiereNiveau;
import org.erpklassup.erpklassup.service.ServiceAsyncBase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * MatiereNiveau se lit deja avec ses jointures (Matiere/Niveau/Filiere) directement
 * dans MatiereNiveauDAO, donc pas besoin de coupler ce service a MatiereService ou
 * GroupeMatiereService ici : un seul DAO suffit, contrairement a RoleService qui doit
 * orchestrer trois tables etroitement liees.
 */
public class MatiereNiveauService extends ServiceAsyncBase {

    // Un coefficient au-dela de cette valeur est presque certainement une erreur
    // de saisie (les coefficients reels vont de 1 a ~6 en pratique).
    private static final BigDecimal COEFFICIENT_MAX = BigDecimal.valueOf(20);

    private final MatiereNiveauDAO matiereNiveauDAO;

    public MatiereNiveauService() {
        this(new MatiereNiveauDAO());
    }

    public MatiereNiveauService(MatiereNiveauDAO matiereNiveauDAO) {
        this.matiereNiveauDAO = matiereNiveauDAO;
    }

    // ========== LECTURE POUR L'ACCORDEON ==========

    // Regroupe les affectations d'une matiere par cycle. MATERNELLE et PRIMAIRE
    // sont fusionnes sous la cle "PRIMAIRE" car le FXML n'a qu'un seul TitledPane
    // "Maternelle / Primaire" pour les deux - le controller n'a qu'a iterer sur les
    // 3 cles (PREMIER_CYCLE, SECOND_CYCLE, PRIMAIRE) pour remplir ses 3 VBox.
    public void obtenirAffectationsParCycleAsync(String idMatiere,
                                                 Consumer<Map<String, List<MatiereNiveau>>> onSucces,
                                                 Consumer<Throwable> onErreur) {
        executer(() -> {
            if (idMatiere == null || idMatiere.isBlank()) {
                throw new IllegalArgumentException("L'identifiant de la matiere ne peut pas etre nul ou vide.");
            }
            return regrouperParCycle(matiereNiveauDAO.findByMatiere(idMatiere));
        }, onSucces, onErreur);
    }

    private Map<String, List<MatiereNiveau>> regrouperParCycle(List<MatiereNiveau> affectations) {
        Map<String, List<MatiereNiveau>> parCycle = new LinkedHashMap<>();
        parCycle.put("PREMIER_CYCLE", new ArrayList<>());
        parCycle.put("SECOND_CYCLE", new ArrayList<>());
        parCycle.put("PRIMAIRE", new ArrayList<>());

        for (MatiereNiveau mn : affectations) {
            String cle = ("MATERNELLE".equals(mn.getCycle()) || "PRIMAIRE".equals(mn.getCycle()))
                    ? "PRIMAIRE"
                    : mn.getCycle();
            parCycle.computeIfAbsent(cle, c -> new ArrayList<>()).add(mn);
        }
        return parCycle;
    }

    // ========== SAUVEGARDE DEPUIS L'ACCORDEON ==========

    // Point d'entree unique pour le bouton "Enregistrer" : le controller construit
    // une ligne par niveau affiche (coche ou non, avec le coefficient saisi) et
    // envoie tout en un seul appel. Chaque ligne est independante (une erreur sur
    // un niveau n'empeche pas les autres d'etre enregistres), d'ou un resultat
    // agrege plutot qu'une exception qui stopperait tout au premier probleme.
    public void enregistrerAffectationsPourMatiereAsync(String idEcole, String idMatiere,
                                                        List<LigneAffectationNiveau> lignes,
                                                        Consumer<ResultatAffectationsNiveau> onSucces,
                                                        Consumer<Throwable> onErreur) {
        executer(() -> {
            if (idEcole == null || idEcole.isBlank()) {
                throw new IllegalArgumentException("L'identifiant de l'ecole ne peut pas etre nul ou vide.");
            }
            if (idMatiere == null || idMatiere.isBlank()) {
                throw new IllegalArgumentException("L'identifiant de la matiere ne peut pas etre nul ou vide.");
            }
            return enregistrerLignes(idEcole, idMatiere, lignes);
        }, onSucces, onErreur);
    }

    private ResultatAffectationsNiveau enregistrerLignes(String idEcole, String idMatiere,
                                                         List<LigneAffectationNiveau> lignes) {
        int enregistrees = 0;
        int retirees = 0;
        List<String> erreurs = new ArrayList<>();

        for (LigneAffectationNiveau ligne : lignes) {
            if (!ligne.isCoche()) {
                boolean ok = matiereNiveauDAO.retirer(idMatiere, ligne.getIdNiveau(), ligne.getIdFiliere());
                if (ok) retirees++;
                else erreurs.add("Echec du retrait pour le niveau " + ligne.getIdNiveau());
                continue;
            }

            String erreurValidation = validerCoefficient(ligne.getCoefficient());
            if (erreurValidation != null) {
                erreurs.add("Niveau " + ligne.getIdNiveau() + " : " + erreurValidation);
                continue;
            }

            MatiereNiveau mn = new MatiereNiveau(idMatiere, ligne.getIdNiveau(), ligne.getCoefficient());
            mn.setIdEcole(idEcole);
            mn.setIdFiliere(ligne.getIdFiliere());
            mn.setObligatoire(true);

            if (matiereNiveauDAO.enregistrer(mn)) enregistrees++;
            else erreurs.add("Echec de l'enregistrement pour le niveau " + ligne.getIdNiveau());
        }

        return new ResultatAffectationsNiveau(enregistrees, retirees, erreurs);
    }

    // ========== "APPLIQUER A TOUS LES NIVEAUX" ==========

    public void appliquerATousLesNiveauxAsync(String idEcole, String idMatiere, BigDecimal coefficient,
                                              Consumer<ResultatAffectationsNiveau> onSucces,
                                              Consumer<Throwable> onErreur) {
        executer(() -> {
            String erreurValidation = validerCoefficient(coefficient);
            if (erreurValidation != null) {
                return new ResultatAffectationsNiveau(0, 0, List.of(erreurValidation));
            }
            int nombre = matiereNiveauDAO.dupliquerVersTousLesNiveaux(idEcole, idMatiere, coefficient, true);
            return new ResultatAffectationsNiveau(nombre, 0, List.of());
        }, onSucces, onErreur);
    }

    // ========== VALIDATION ==========

    private String validerCoefficient(BigDecimal coefficient) {
        if (coefficient == null) return "coefficient manquant";
        if (coefficient.compareTo(BigDecimal.ZERO) <= 0) return "le coefficient doit etre superieur a 0";
        if (coefficient.compareTo(COEFFICIENT_MAX) > 0) return "coefficient improbable (> " + COEFFICIENT_MAX + "), verifiez la saisie";
        return null;
    }




    /**
     * Renvoie TOUTES les combinaisons Niveau × Filière de l'école, avec un
     * MatiereNiveau rempli si l'affectation existe, ou un MatiereNiveau "vide"
     * (coché = false, coefficient = 1) sinon.
     *
     * Cela permet à l'accordéon d'afficher tous les niveaux disponibles, même
     * ceux qui ne sont pas encore affectés à la matière — sinon l'utilisateur ne
     * pourrait jamais créer de nouvelle affectation.
     */
    public void obtenirAffectationsParCycleAsync(String idEcole, String idMatiere,
                                                 Consumer<Map<String, List<MatiereNiveau>>> onSucces,
                                                 Consumer<Throwable> onErreur) {
        executer(() -> {
            if (idMatiere == null || idMatiere.isBlank()) {
                throw new IllegalArgumentException("L'identifiant de la matière ne peut pas être vide.");
            }
            if (idEcole == null || idEcole.isBlank()) {
                throw new IllegalArgumentException("L'identifiant de l'école ne peut pas être vide.");
            }

            // 1. Toutes les affectations existantes pour cette matière
            List<MatiereNiveau> existantes = matiereNiveauDAO.findByMatiere(idMatiere);

            // 2. Toutes les combinaisons Niveau × Filière disponibles
            //    → à créer dans MatiereNiveauDAO : voir plus bas
            List<MatiereNiveau> toutes = matiereNiveauDAO.toutesLesCombinaisonsPossibles(idEcole);

            // 3. Fusion : si une combinaison existe dans `existantes`, on garde
            //    ses valeurs (coché, coefficient). Sinon on garde la version vide.
            Map<String, MatiereNiveau> index = new LinkedHashMap<>();
            for (MatiereNiveau mn : existantes) {
                index.put(cle(mn.getIdNiveau(), mn.getIdFiliere()), mn);
            }
            for (MatiereNiveau mn : toutes) {
                String k = cle(mn.getIdNiveau(), mn.getIdFiliere());
                if (index.containsKey(k)) {
                    MatiereNiveau ex = index.get(k);
                    // On complète la version vide avec les infos d'affichage et
                    // on écrase par les valeurs persistées.
                    mn.setCoefficient(ex.getCoefficient());
                    mn.setObligatoire(ex.isObligatoire());
                    mn.setIdMatiereNiveau(ex.getIdMatiereNiveau());
                }
                index.put(k, mn);
            }

            // 4. Regroupement par cycle
            return regrouperParCycle(new ArrayList<>(index.values()));
        }, onSucces, onErreur);
    }

    private String cle(String idNiveau, String idFiliere) {
        return idNiveau + "#" + (idFiliere != null ? idFiliere : "_");
    }
}