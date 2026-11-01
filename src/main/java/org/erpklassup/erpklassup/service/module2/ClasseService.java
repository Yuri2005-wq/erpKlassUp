package org.erpklassup.erpklassup.service.module2;

import org.erpklassup.erpklassup.dao.module2.ClasseDAO;
import org.erpklassup.erpklassup.dao.module2.SalleDAO;
import org.erpklassup.erpklassup.dto.ResultatCreationMasse;
import org.erpklassup.erpklassup.models.module2.Classe;
import org.erpklassup.erpklassup.service.ServiceAsyncBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class ClasseService extends ServiceAsyncBase {

    private final ClasseDAO classeDAO;
    private final SalleDAO  salleDAO;

    /* ============================================================
       CONSTRUCTEURS
       ============================================================ */
    public ClasseService() {
        this(new ClasseDAO(), new SalleDAO());
    }

    public ClasseService(ClasseDAO classeDAO) {
        this(classeDAO, new SalleDAO());
    }

    public ClasseService(ClasseDAO classeDAO, SalleDAO salleDAO) {
        this.classeDAO = classeDAO;
        this.salleDAO  = salleDAO;
    }

    /* ============================================================
       RECHERCHE / CHARGEMENT
       ============================================================ */
    public void chargerParAnneeAsync(String idAnnee,
                                     Consumer<List<Classe>> onSucces,
                                     Consumer<Throwable> onErreur) {
        executer(() -> classeDAO.findByAnnee(idAnnee), onSucces, onErreur);
    }

    public void rechercherAsync(String idEcole, String idAnnee,
                                String idNiveau, String terme,
                                Consumer<List<Classe>> onSucces,
                                Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(() -> classeDAO.rechercher(idEcole, idAnnee, idNiveau, terme),
                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
                erreur -> { if (estRequeteActuelle(jeton)) onErreur.accept(erreur); });
    }

    public void chargerParIdAsync(String idClasse,
                                  Consumer<Classe> onSucces,
                                  Consumer<Throwable> onErreur) {
        executer(() -> classeDAO.findById(idClasse).orElse(null), onSucces, onErreur);
    }

    /* ============================================================
       CRÉATION
       ============================================================ */
    public void creerAsync(Classe classe,
                           Runnable onSucces,
                           Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            // 1) Validation de base
            if (classe.getIdEcole() == null || classe.getIdEcole().isBlank()) {
                throw new IllegalArgumentException("L'identifiant de l'école est obligatoire.");
            }
            if (classe.getIdAnnee() == null || classe.getIdAnnee().isBlank()) {
                throw new IllegalArgumentException("Aucune année scolaire active.");
            }
            if (classe.getVacation() == null) {
                throw new IllegalArgumentException("La vacation est obligatoire.");
            }

            if (classe.getId() == null || classe.getId().isBlank()) {
                classe.setId(java.util.UUID.randomUUID().toString());
            }

            // 2) Unicité (nom + année + vacation + école)
            if (classeDAO.nomClasseExiste(
                    classe.getIdEcole(),
                    classe.getIdAnnee(),
                    classe.getNomClasse(),
                    classe.getVacation().name(),
                    null)) {
                throw new IllegalStateException(
                        "Une classe portant le nom « " + classe.getNomClasse()
                                + " » existe déjà pour cette vacation.");
            }

            // 3) Vérification de la salle (si affectée)
            if (classe.getIdSalle() != null && !classe.getIdSalle().isBlank()) {
                boolean conflit = classeDAO.conflitSalleVacation(
                        classe.getIdSalle(),
                        classe.getVacation().name(),
                        null);

                if (conflit) {
                    String libelleVac = classe.getVacation().getLibelle();
                    throw new IllegalStateException(
                            "La salle sélectionnée est déjà occupée pour la vacation « "
                                    + libelleVac + " ». Choisissez une autre salle ou une autre vacation.");
                }
            }

            // 4) Création
            if (!classeDAO.create(classe)) {
                throw new IllegalStateException(
                        "Création impossible. Vérifiez les informations saisies.");
            }

            // 5) Recalcul du statut d'occupation de la salle
            if (classe.getIdSalle() != null && !classe.getIdSalle().isBlank()) {
                recalculerOccupationSalle(classe.getIdSalle());
            }
        }, onSucces, onErreur);
    }

    /* ============================================================
       MODIFICATION
       ============================================================ */
    public void modifierAsync(Classe classe,
                              Runnable onSucces,
                              Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            if (classe.getId() == null || classe.getId().isBlank()) {
                throw new IllegalArgumentException("L'identifiant de la classe est obligatoire.");
            }
            if (classe.getVacation() == null) {
                throw new IllegalArgumentException("La vacation est obligatoire.");
            }

            // 1) Récupérer l'ancienne version
            Classe ancienne = classeDAO.findById(classe.getId()).orElse(null);
            String ancienneSalleId = (ancienne != null) ? ancienne.getIdSalle() : null;
            String ancienneVacation = (ancienne != null && ancienne.getVacation() != null)
                    ? ancienne.getVacation().name() : null;

            // 2) Unicité
            if (classeDAO.nomClasseExiste(
                    classe.getIdEcole(),
                    classe.getIdAnnee(),
                    classe.getNomClasse(),
                    classe.getVacation().name(),
                    classe.getId())) {
                throw new IllegalStateException(
                        "Une autre classe porte déjà le nom « " + classe.getNomClasse()
                                + " » pour cette vacation.");
            }

            // 3) Vérification salle (si affectée)
            if (classe.getIdSalle() != null && !classe.getIdSalle().isBlank()) {
                boolean conflit = classeDAO.conflitSalleVacation(
                        classe.getIdSalle(),
                        classe.getVacation().name(),
                        classe.getId());

                if (conflit) {
                    String libelleVac = classe.getVacation().getLibelle();
                    throw new IllegalStateException(
                            "La salle sélectionnée est déjà occupée pour la vacation « "
                                    + libelleVac + " ». Choisissez une autre salle ou une autre vacation.");
                }
            }

            // 4) Mise à jour
            if (!classeDAO.update(classe)) {
                throw new IllegalStateException("Modification impossible (classe introuvable).");
            }

            // 5) Recalcul du statut d'occupation pour les salles impactées
            if (ancienneSalleId != null && !ancienneSalleId.isBlank()) {
                recalculerOccupationSalle(ancienneSalleId);
            }
            if (classe.getIdSalle() != null && !classe.getIdSalle().isBlank()
                    && !classe.getIdSalle().equals(ancienneSalleId)) {
                recalculerOccupationSalle(classe.getIdSalle());
            }
        }, onSucces, onErreur);
    }

    /* ============================================================
       SUPPRESSION
       ============================================================ */
    public void supprimerAsync(String idClasse,
                               Runnable onSucces,
                               Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            if (idClasse == null || idClasse.isBlank()) {
                throw new IllegalArgumentException("L'identifiant de la classe est obligatoire.");
            }

            // 1) Récupérer la salle
            Classe classe = classeDAO.findById(idClasse).orElse(null);
            String salleId = (classe != null) ? classe.getIdSalle() : null;

            // 2) Soft delete
            if (!classeDAO.softDelete(idClasse)) {
                throw new IllegalStateException("Suppression impossible (classe introuvable).");
            }

            // 3) Recalcul occupation
            if (salleId != null && !salleId.isBlank()) {
                recalculerOccupationSalle(salleId);
            }
        }, onSucces, onErreur);
    }

    public void supprimerEnMasseAsync(Set<String> ids,
                                      Runnable onSucces,
                                      Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            if (ids == null || ids.isEmpty()) {
                throw new IllegalArgumentException("Aucune classe sélectionnée.");
            }

            java.util.Set<String> sallesImpactees = new java.util.HashSet<>();

            for (String id : ids) {
                Classe classe = classeDAO.findById(id).orElse(null);
                if (classe != null && classe.getIdSalle() != null
                        && !classe.getIdSalle().isBlank()) {
                    sallesImpactees.add(classe.getIdSalle());
                }
                classeDAO.softDelete(id);
            }

            for (String salleId : sallesImpactees) {
                recalculerOccupationSalle(salleId);
            }
        }, onSucces, onErreur);
    }

    /* ============================================================
       CRÉATION MULTIPLE
       ============================================================ */
    public void creerEnMasseAsync(String idEcole,
                                  String idAnnee,
                                  List<Classe> classes,
                                  Consumer<ResultatCreationMasse> onSucces,
                                  Consumer<Throwable> onErreur) {
        executer(() -> {
            if (idEcole == null || idEcole.isBlank()) {
                throw new IllegalArgumentException("L'identifiant de l'école est obligatoire.");
            }
            if (idAnnee == null || idAnnee.isBlank()) {
                throw new IllegalArgumentException("Aucune année scolaire active.");
            }
            if (classes == null || classes.isEmpty()) {
                throw new IllegalArgumentException("Aucune classe à créer.");
            }

            int creees = 0;
            int ignorees = 0;
            List<String> erreurs = new ArrayList<>();

            java.util.Set<String> sallesImpactees = new java.util.HashSet<>();

            for (Classe classe : classes) {
                try {
                    classe.setIdEcole(idEcole);
                    classe.setIdAnnee(idAnnee);

                    if (classe.getId() == null || classe.getId().isBlank()) {
                        classe.setId(java.util.UUID.randomUUID().toString());
                    }

                    // Doublon nom ?
                    if (classeDAO.nomClasseExiste(
                            idEcole, idAnnee,
                            classe.getNomClasse(),
                            classe.getVacation().name(),
                            null)) {
                        ignorees++;
                        continue;
                    }

                    // Conflit salle/vacation ?
                    if (classe.getIdSalle() != null && !classe.getIdSalle().isBlank()
                            && classeDAO.conflitSalleVacation(
                            classe.getIdSalle(),
                            classe.getVacation().name(),
                            null)) {
                        erreurs.add("Salle déjà occupée pour « " + classe.getNomClasse()
                                + " » (vacation " + classe.getVacation().getLibelle() + ")");
                        continue;
                    }

                    // Création
                    if (classeDAO.create(classe)) {
                        creees++;
                        if (classe.getIdSalle() != null && !classe.getIdSalle().isBlank()) {
                            sallesImpactees.add(classe.getIdSalle());
                        }
                    } else {
                        erreurs.add("Échec pour : " + classe.getNomClasse());
                    }
                } catch (Exception e) {
                    erreurs.add(classe.getNomClasse() + " : " + e.getMessage());
                }
            }

            // Recalculer les occupations
            for (String salleId : sallesImpactees) {
                recalculerOccupationSalle(salleId);
            }

            return new ResultatCreationMasse(creees, ignorees, erreurs);
        }, onSucces, onErreur);
    }

    /* ============================================================
       MÉTHODE PRIVÉE — Recalcul occupation salle
       ============================================================ */
    /**
     * Recalcule le booléen `occupee` de la salle :
     * true si au moins une classe active l'utilise, false sinon.
     */
    private void recalculerOccupationSalle(String idSalle) {
        if (idSalle == null || idSalle.isBlank()) return;

        long reste = classeDAO.compterClassesDansSalle(idSalle);
        salleDAO.setOccupation(idSalle, reste > 0);
    }

    /* ============================================================
       MÉTIER
       ============================================================ */
    public void compterEffectifAsync(String idClasse,
                                     Consumer<Integer> onSucces,
                                     Consumer<Throwable> onErreur) {
        executer(() -> classeDAO.getEffectifActuel(idClasse), onSucces, onErreur);
    }

    public void dupliquerDepuisAnneeAsync(String idEcole,
                                          String idAnneeSource,
                                          String idAnneeCible,
                                          Consumer<Integer> onSucces,
                                          Consumer<Throwable> onErreur) {
        executer(() -> classeDAO.dupliquerDepuisAnnee(idEcole, idAnneeSource, idAnneeCible),
                onSucces, onErreur);
    }

    public void verifierNomExisteAsync(String idEcole,
                                       String idAnnee,
                                       String nomClasse,
                                       String vacation,
                                       String idExclu,
                                       Consumer<Boolean> onSucces,
                                       Consumer<Throwable> onErreur) {
        executer(() -> classeDAO.nomClasseExiste(idEcole, idAnnee, nomClasse, vacation, idExclu),
                onSucces, onErreur);
    }

    public List<Classe> rechercher(String idEcole,
                                   String idAnnee,
                                   String idNiveau,
                                   String terme) {
        if (idEcole == null || idAnnee == null) {
            return List.of();
        }
        return classeDAO.rechercher(idEcole, idAnnee, idNiveau, terme);
    }
}