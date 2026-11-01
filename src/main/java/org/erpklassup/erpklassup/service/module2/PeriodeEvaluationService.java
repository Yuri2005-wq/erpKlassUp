package org.erpklassup.erpklassup.service.module2;

import org.erpklassup.erpklassup.dao.module2.PeriodeEvaluationDAO;
import org.erpklassup.erpklassup.models.module2.PeriodeEvaluation;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.Autorisation;
import org.erpklassup.erpklassup.service.ServiceAsyncBase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Service pour PeriodeEvaluation.
 *
 * <p>Porte les règles métier que le DAO ne doit pas connaître :
 * validation avant écriture, unicité du code, clôture/suppression en
 * cascade, génération de la structure standard (trimestres + séquences,
 * ou semestres) en une seule transaction.</p>
 */
public class PeriodeEvaluationService extends ServiceAsyncBase {

    private final PeriodeEvaluationDAO periodeDAO;
    private final AuditService auditService;

    public PeriodeEvaluationService() {
        this(new PeriodeEvaluationDAO(), new AuditService());
    }

    public PeriodeEvaluationService(PeriodeEvaluationDAO periodeDAO, AuditService auditService) {
        this.periodeDAO = periodeDAO;
        this.auditService = auditService;
    }

    /* ============================================================
       LECTURE
       ============================================================ */
    public void chargerArborescenceAsync(String idAnnee,
                                         Consumer<List<PeriodeEvaluation>> onSucces,
                                         Consumer<Throwable> onErreur) {
        executer(() -> periodeDAO.findArborescence(idAnnee), onSucces, onErreur);
    }

    /* ============================================================
       CRÉATION
       ============================================================ */
    public void creerAsync(PeriodeEvaluation periode, String idEcole,
                           Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("user.creer");

            periode.setIdEcole(idEcole);
            if (periode.getId() == null || periode.getId().isBlank()) {
                periode.setId(UUID.randomUUID().toString());
            }

            List<String> erreurs = periode.validerChamps();
            if (!erreurs.isEmpty()) {
                throw new IllegalArgumentException(String.join(" ", erreurs));
            }

            if (periodeDAO.codeExiste(periode.getIdAnnee(), periode.getCodePeriode(), null)) {
                throw new IllegalStateException(
                        "Le code \"" + periode.getCodePeriode() + "\" est déjà utilisé pour cette année scolaire.");
            }

            if (!periodeDAO.create(periode)) {
                throw new IllegalStateException("Échec de la création de la période.");
            }

            auditService.tracerActionAsync("ACADEMIQUE", "CREATION_PERIODE",
                    "Période créée : " + periode.getLibelle() + " (" + periode.getCodePeriode() + ")");
        }, onSucces, onErreur);
    }

    /* ============================================================
       MODIFICATION
       ============================================================ */
    public void modifierAsync(PeriodeEvaluation periode,
                              Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("periode.modifier");

            List<String> erreurs = periode.validerChamps();
            if (!erreurs.isEmpty()) {
                throw new IllegalArgumentException(String.join(" ", erreurs));
            }

            if (periodeDAO.codeExiste(periode.getIdAnnee(), periode.getCodePeriode(), periode.getId())) {
                throw new IllegalStateException(
                        "Le code \"" + periode.getCodePeriode() + "\" est déjà utilisé pour cette année scolaire.");
            }

            boolean ok = periodeDAO.update(periode);
            if (!ok) {
                // 0 ligne affectée = soit la période a été supprimée entre-temps,
                // soit quelqu'un d'autre l'a modifiée depuis (conflit de version).
                throw new ConflitVersionException(
                        "Cette période a été modifiée par quelqu'un d'autre entre-temps. "
                                + "Rechargez l'arborescence avant de réessayer.");
            }

            auditService.tracerActionAsync("ACADEMIQUE", "MODIFICATION_PERIODE",
                    "Période modifiée : " + periode.getLibelle() + " (" + periode.getCodePeriode() + ")");
        }, onSucces, onErreur);
    }

    /* ============================================================
       CLÔTURE / RÉOUVERTURE
       ============================================================ */

    /**
     * Clôture une période après vérification de la règle de cascade :
     * une période avec des enfants ne peut être clôturée que si tous
     * ses enfants actifs sont déjà clôturés.
     */
    public void cloturerAsync(PeriodeEvaluation periode,
                              Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("periode.cloturer");

            if (!periodeDAO.peutCloturer(periode.getId())) {
                throw new IllegalStateException(
                        "Impossible de clôturer \"" + periode.getLibelle()
                                + "\" : toutes ses sous-périodes doivent être clôturées d'abord.");
            }

            boolean ok = periodeDAO.cloturer(periode.getId(), periode.getVersion());
            if (!ok) {
                throw new ConflitVersionException(
                        "Cette période a été modifiée entre-temps. Rechargez avant de réessayer.");
            }

            auditService.tracerActionAsync("ACADEMIQUE", "CLOTURE_PERIODE",
                    "Période clôturée : " + periode.getLibelle() + " (" + periode.getCodePeriode() + ")");
        }, onSucces, onErreur);
    }

    public void reouvrirAsync(PeriodeEvaluation periode,
                              Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("periode.reouvrir");

            boolean ok = periodeDAO.reouvrir(periode.getId(), periode.getVersion());
            if (!ok) {
                throw new ConflitVersionException(
                        "Cette période a été modifiée entre-temps. Rechargez avant de réessayer.");
            }

            auditService.tracerActionAsync("ACADEMIQUE", "REOUVERTURE_PERIODE",
                    "Période réouverte : " + periode.getLibelle() + " (" + periode.getCodePeriode() + ") "
                            + "— action sensible, la saisie des notes redevient possible.");
        }, onSucces, onErreur);
    }

    /* ============================================================
       SUPPRESSION
       ============================================================ */
    public void supprimerAsync(PeriodeEvaluation periode,
                               Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("periode.supprimer");

            int nbEnfants = periodeDAO.compterEnfants(periode.getId());
            if (nbEnfants > 0) {
                throw new IllegalStateException(
                        "Impossible de supprimer \"" + periode.getLibelle() + "\" : "
                                + nbEnfants + " sous-période(s) en dépendent. Supprimez-les d'abord.");
            }

            if (!periodeDAO.softDelete(periode.getId())) {
                throw new IllegalStateException("Échec de la suppression de la période.");
            }

            auditService.tracerActionAsync("ACADEMIQUE", "SUPPRESSION_PERIODE",
                    "Période supprimée : " + periode.getLibelle() + " (" + periode.getCodePeriode() + ")");
        }, onSucces, onErreur);
    }

    /* ============================================================
       GÉNÉRATION DE LA STRUCTURE STANDARD
       ============================================================ */

    public enum ModeGeneration { TRIMESTRES_SEQUENCES, SEMESTRES }

    /**
     * Génère automatiquement la structure d'évaluation d'une année scolaire.
     *
     * <p>Répartit uniformément la période [debutAnnee, finAnnee] :
     * 3 trimestres de 2 séquences chacun (mode TRIMESTRES_SEQUENCES),
     * ou 2 semestres sans séquences (mode SEMESTRES). Les dates
     * générées sont des valeurs par défaut — l'utilisateur les ajuste
     * ensuite nœud par nœud dans l'arbre.</p>
     *
     * <p>Refuse de générer si des périodes existent déjà pour cette
     * année, pour éviter les doublons de code.</p>
     */
    public void genererStructureStandardAsync(String idEcole, String idAnnee,
                                              LocalDate debutAnnee, LocalDate finAnnee,
                                              ModeGeneration mode,
                                              Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("user.creer");

            if (debutAnnee == null || finAnnee == null || !finAnnee.isAfter(debutAnnee)) {
                throw new IllegalArgumentException("Dates de début/fin d'année invalides.");
            }

            List<PeriodeEvaluation> existantes = periodeDAO.findByAnnee(idAnnee);
            if (!existantes.isEmpty()) {
                throw new IllegalStateException(
                        "Cette année scolaire a déjà " + existantes.size()
                                + " période(s) enregistrée(s). Supprimez-les d'abord pour régénérer.");
            }

            List<PeriodeEvaluation> aCreer = (mode == ModeGeneration.SEMESTRES)
                    ? construireSemestres(idEcole, idAnnee, debutAnnee, finAnnee)
                    : construireTrimestresEtSequences(idEcole, idAnnee, debutAnnee, finAnnee);

            if (!periodeDAO.creerLot(aCreer)) {
                throw new IllegalStateException("Échec de la génération de la structure (transaction annulée).");
            }

            auditService.tracerActionAsync("ACADEMIQUE", "GENERATION_STRUCTURE_PERIODES",
                    "Structure générée pour l'année (" + aCreer.size() + " périodes, mode " + mode + ")");
        }, onSucces, onErreur);
    }

    private List<PeriodeEvaluation> construireTrimestresEtSequences(String idEcole, String idAnnee,
                                                                    LocalDate debut, LocalDate fin) {
        List<PeriodeEvaluation> resultat = new ArrayList<>();
        long joursTotal = java.time.temporal.ChronoUnit.DAYS.between(debut, fin);
        long joursParTrimestre = joursTotal / 3;

        LocalDate curseur = debut;
        for (int t = 1; t <= 3; t++) {
            LocalDate debutTrimestre = curseur;
            LocalDate finTrimestre = (t == 3) ? fin : curseur.plusDays(joursParTrimestre);

            PeriodeEvaluation trimestre = nouvellePeriode(idEcole, idAnnee, null,
                    "T" + t, "Trimestre " + t, PeriodeEvaluation.TYPE_TRIMESTRE, t,
                    debutTrimestre, finTrimestre);
            resultat.add(trimestre);

            long joursParSequence = java.time.temporal.ChronoUnit.DAYS.between(debutTrimestre, finTrimestre) / 2;
            LocalDate finSeq1 = debutTrimestre.plusDays(joursParSequence);

            PeriodeEvaluation seq1 = nouvellePeriode(idEcole, idAnnee, trimestre.getId(),
                    "T" + t + "S1", "Séquence " + (2 * t - 1), PeriodeEvaluation.TYPE_SEQUENCE,
                    2 * t - 1, debutTrimestre, finSeq1);
            PeriodeEvaluation seq2 = nouvellePeriode(idEcole, idAnnee, trimestre.getId(),
                    "T" + t + "S2", "Séquence " + (2 * t), PeriodeEvaluation.TYPE_SEQUENCE,
                    2 * t, finSeq1.plusDays(1), finTrimestre);

            resultat.add(seq1);
            resultat.add(seq2);

            curseur = finTrimestre.plusDays(1);
        }
        return resultat;
    }

    private List<PeriodeEvaluation> construireSemestres(String idEcole, String idAnnee,
                                                        LocalDate debut, LocalDate fin) {
        List<PeriodeEvaluation> resultat = new ArrayList<>();
        long joursTotal = java.time.temporal.ChronoUnit.DAYS.between(debut, fin);
        LocalDate finSemestre1 = debut.plusDays(joursTotal / 2);

        resultat.add(nouvellePeriode(idEcole, idAnnee, null,
                "S1", "Semestre 1", PeriodeEvaluation.TYPE_SEMESTRE, 1, debut, finSemestre1));
        resultat.add(nouvellePeriode(idEcole, idAnnee, null,
                "S2", "Semestre 2", PeriodeEvaluation.TYPE_SEMESTRE, 2, finSemestre1.plusDays(1), fin));

        return resultat;
    }

    private PeriodeEvaluation nouvellePeriode(String idEcole, String idAnnee, String idParent,
                                              String code, String libelle, String type, int ordre,
                                              LocalDate debut, LocalDate fin) {
        PeriodeEvaluation p = new PeriodeEvaluation(code, libelle, type, ordre);
        p.setId(UUID.randomUUID().toString());
        p.setIdEcole(idEcole);
        p.setIdAnnee(idAnnee);
        p.setIdPeriodeParent(idParent);
        p.setPoids(BigDecimal.ONE);
        p.setDateDebut(debut);
        p.setDateFin(fin);
        return p;
    }

    /**
     * Exception dédiée aux conflits de version (concurrence optimiste),
     * pour que le Controller puisse afficher un message différent
     * d'une simple erreur technique.
     */
    public static class ConflitVersionException extends RuntimeException {
        public ConflitVersionException(String message) {
            super(message);
        }
    }

    /**
     * Version synchrone — utilisée par AutoRefreshService (qui tourne déjà
     * sur un thread background).
     */
    public List<PeriodeEvaluation> chargerArborescence(String idAnnee) {
        return periodeDAO.findArborescence(idAnnee);
    }
}
