package org.erpklassup.erpklassup.service.GRH;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.erpklassup.erpklassup.dao.GRH.DepartementDAO;
import org.erpklassup.erpklassup.dao.PersonnelDAO;
import org.erpklassup.erpklassup.dao.RoleDAO;
import org.erpklassup.erpklassup.dao.UtilisateurDAO;
import org.erpklassup.erpklassup.dao.UtilisateurRoleDAO;
import org.erpklassup.erpklassup.dto.FiltrePersonnel;
import org.erpklassup.erpklassup.dto.PersonnelLigne;
import org.erpklassup.erpklassup.dto.ResultatPagine;
import org.erpklassup.erpklassup.models.GRH.Personnel;
import org.erpklassup.erpklassup.service.Autorisation;
import org.erpklassup.erpklassup.service.PasswordService;
import org.erpklassup.erpklassup.service.RafraichisseurPermissions;
import org.erpklassup.erpklassup.service.ServiceAsyncBase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Service métier du module GRH — gestion du Personnel.
 *
 * Respecte le pattern UtilisateurService :
 *  - Hérite de ServiceAsyncBase (exécution asynchrone).
 *  - 3 constructeurs (par défaut, partiel, complet).
 *  - Toutes les méthodes publiques sont async.
 *  - Autorisation.exigerAction() au début de chaque mutation.
 *  - Rafraîchisseur après toute modification.
 */

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class PersonnelService extends ServiceAsyncBase {

    // ==========================================
    // DÉPENDANCES
    // ==========================================
    private final PersonnelDAO personnelDAO;
    private final DepartementDAO departementDAO;
    private final UtilisateurDAO utilisateurDAO;
    private final UtilisateurRoleDAO utilisateurRoleDAO;
    private final RoleDAO roleDAO;
    private final RafraichisseurPermissions rafraichisseur;

    // ==========================================
    // CONSTRUCTEURS
    // ==========================================

    /** Constructeur par défaut : câble toutes les dépendances. */
    public PersonnelService() {
        this(new PersonnelDAO(), new DepartementDAO(),
                new UtilisateurDAO(), new UtilisateurRoleDAO(), new RoleDAO(),
                new RafraichisseurPermissions());
    }

    /**
     * Constructeur partiel (utile pour les tests).
     * Fournit un rafraîchisseur par défaut.
     */
    public PersonnelService(PersonnelDAO personnelDAO,
                            DepartementDAO departementDAO,
                            UtilisateurDAO utilisateurDAO) {
        this(personnelDAO, departementDAO,
                utilisateurDAO, new UtilisateurRoleDAO(), new RoleDAO(),
                new RafraichisseurPermissions());
    }

    /** Constructeur complet (injection intégrale). */
    public PersonnelService(PersonnelDAO personnelDAO,
                            DepartementDAO departementDAO,
                            UtilisateurDAO utilisateurDAO,
                            UtilisateurRoleDAO utilisateurRoleDAO,
                            RoleDAO roleDAO,
                            RafraichisseurPermissions rafraichisseur) {
        this.personnelDAO = personnelDAO;
        this.departementDAO = departementDAO;
        this.utilisateurDAO = utilisateurDAO;
        this.utilisateurRoleDAO = utilisateurRoleDAO;
        this.roleDAO = roleDAO;
        this.rafraichisseur = rafraichisseur;
    }

    // ==========================================
    // RECHERCHE
    // ==========================================
    public ResultatPagine<PersonnelLigne> rechercher(FiltrePersonnel filtre) {
        return personnelDAO.rechercherPagine(filtre);
    }

    public void rechercherAsync(FiltrePersonnel filtre,
                                Consumer<ResultatPagine<PersonnelLigne>> onSucces,
                                Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(() -> personnelDAO.rechercherPagine(filtre),
                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
                erreur -> { if (estRequeteActuelle(jeton)) onErreur.accept(erreur); });
    }

    public void chargerParIdAsync(String idPersonnel,
                                  Consumer<Personnel> onSucces,
                                  Consumer<Throwable> onErreur) {
        executer(() -> personnelDAO.findById(idPersonnel)
                        .orElseThrow(() -> new IllegalStateException("Personnel introuvable.")),
                onSucces, onErreur);
    }

    public void chargerParEcoleAsync(String idEcole,
                                     Consumer<List<Personnel>> onSucces,
                                     Consumer<Throwable> onErreur) {
        executer(() -> personnelDAO.findByEcole(idEcole), onSucces, onErreur);
    }

    public void chargerParDepartementAsync(String idEcole, String idDepartement,
                                           Consumer<List<Personnel>> onSucces,
                                           Consumer<Throwable> onErreur) {
        executer(() -> personnelDAO.findByDepartement(idEcole, idDepartement),
                onSucces, onErreur);
    }

    public void chargerParStatutAsync(String idEcole, String statutEmploi,
                                      Consumer<List<Personnel>> onSucces,
                                      Consumer<Throwable> onErreur) {
        executer(() -> personnelDAO.findByEcoleEtStatut(idEcole, statutEmploi),
                onSucces, onErreur);
    }

    public void chargerSubordonnesAsync(String idResponsable,
                                        Consumer<List<Personnel>> onSucces,
                                        Consumer<Throwable> onErreur) {
        executer(() -> personnelDAO.findByResponsable(idResponsable), onSucces, onErreur);
    }

    public void rechercherSimpleAsync(String idEcole, String terme,
                                      Consumer<List<Personnel>> onSucces,
                                      Consumer<Throwable> onErreur) {
        executer(() -> personnelDAO.rechercher(idEcole, terme), onSucces, onErreur);
    }

    public void chargerSansCompteAsync(String idEcole,
                                       Consumer<List<Personnel>> onSucces,
                                       Consumer<Throwable> onErreur) {
        executer(() -> personnelDAO.findSansCompte(idEcole), onSucces, onErreur);
    }

    /** ObservableList<Personnel> pour binding JavaFX direct (ComboBox, ListView). */
    public ObservableList<Personnel> chargerParEcoleObservable(String idEcole) {
        return FXCollections.observableArrayList(personnelDAO.findByEcole(idEcole));
    }

    // ==========================================
    // CRÉATION
    // ==========================================
    public void creerAsync(Personnel p, String idEcole, String idUtilisateurCreateur,
                           Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("personnel.creer");

            // Contrôles métier
            if (p.getMatriculeInterne() == null || p.getMatriculeInterne().isBlank()) {
                throw new IllegalArgumentException("Le matricule est obligatoire.");
            }
            if (p.getNom() == null || p.getNom().isBlank()) {
                throw new IllegalArgumentException("Le nom est obligatoire.");
            }
            if (p.getTypePersonnel() == null || p.getTypePersonnel().isBlank()) {
                throw new IllegalArgumentException("Le type de personnel est obligatoire.");
            }
            if (p.getPosteOuFonction() == null || p.getPosteOuFonction().isBlank()) {
                throw new IllegalArgumentException("Le poste est obligatoire.");
            }

            if (personnelDAO.existeMatricule(idEcole, p.getMatriculeInterne(), null)) {
                throw new IllegalStateException("Ce matricule existe déjà dans l'école.");
            }
            if (p.getNumCnps() != null && !p.getNumCnps().isBlank()
                    && personnelDAO.existeNumCnps(idEcole, p.getNumCnps(), null)) {
                throw new IllegalStateException("Ce numéro CNPS est déjà utilisé.");
            }

            p.setIdEcole(idEcole);
            if (p.getStatutEmploi() == null) p.setStatutEmploi("ACTIF");
            if (p.getModePaiement() == null) p.setModePaiement("ESPECES");
            if (p.getDechargeHeures() == null) p.setDechargeHeures(BigDecimal.ZERO);

            if (!personnelDAO.insert(p)) {
                throw new IllegalStateException("Création du personnel impossible.");
            }

            // Si lié à un utilisateur, rafraîchir ses permissions
            if (p.getIdUtilisateur() != null) {
                rafraichisseur.rafraichirPourUtilisateur(p.getIdUtilisateur());
            }
        }, onSucces, onErreur);
    }

    /** Crée un Personnel en même temps qu'un compte Utilisateur, si demandé. */
    public void creerAvecCompteAsync(Personnel p, String idEcole,
                                     String username, String motDePasse, String idRole,
                                     Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("personnel.creer");
            Autorisation.exigerAction("user.creer");

            if (personnelDAO.existeMatricule(idEcole, p.getMatriculeInterne(), null)) {
                throw new IllegalStateException("Ce matricule existe déjà dans l'école.");
            }

            // 1) Créer le compte utilisateur
            org.erpklassup.erpklassup.models.Utilisateur u =
                    new org.erpklassup.erpklassup.models.Utilisateur(username, p.getNom());
            u.setIdEcole(idEcole);
            u.setPrenom(p.getPrenom());
            u.setEmail(p.getEmail());
            u.setPasswordHash(PasswordService.hacher(motDePasse));
            if (!utilisateurDAO.create(u)) {
                throw new IllegalStateException("Identifiant déjà utilisé ou création impossible.");
            }

            // 2) Lier le Personnel au compte
            p.setIdEcole(idEcole);
            p.setIdUtilisateur(u.getIdUtilisateur());
            if (p.getStatutEmploi() == null) p.setStatutEmploi("ACTIF");
            if (p.getModePaiement() == null) p.setModePaiement("ESPECES");
            if (p.getDechargeHeures() == null) p.setDechargeHeures(BigDecimal.ZERO);

            if (!personnelDAO.insert(p)) {
                // Compensation : supprimer le compte créé
                utilisateurDAO.softDelete(u.getIdUtilisateur());
                throw new IllegalStateException("Création du personnel impossible — compte annulé.");
            }

            // 3) Assigner le rôle
            if (idRole != null && !idRole.isBlank()) {
                utilisateurRoleDAO.assignerRole(u.getIdUtilisateur(), idRole, idEcole);
            }

            rafraichisseur.rafraichirPourUtilisateur(u.getIdUtilisateur());
        }, onSucces, onErreur);
    }

    // ==========================================
    // MODIFICATION
    // ==========================================
    public void modifierAsync(Personnel p,
                              Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("personnel.modifier");

            Personnel existant = personnelDAO.findById(p.getIdPersonnel())
                    .orElseThrow(() -> new IllegalStateException("Personnel introuvable."));

            if (p.getMatriculeInterne() == null || p.getMatriculeInterne().isBlank()) {
                throw new IllegalArgumentException("Le matricule est obligatoire.");
            }
            if (!p.getMatriculeInterne().equals(existant.getMatriculeInterne())
                    && personnelDAO.existeMatricule(p.getIdEcole(), p.getMatriculeInterne(),
                    p.getIdPersonnel())) {
                throw new IllegalStateException("Ce matricule est déjà utilisé par un autre employé.");
            }
            if (p.getNumCnps() != null && !p.getNumCnps().isBlank()
                    && !p.getNumCnps().equals(existant.getNumCnps())
                    && personnelDAO.existeNumCnps(p.getIdEcole(), p.getNumCnps(),
                    p.getIdPersonnel())) {
                throw new IllegalStateException("Ce numéro CNPS est déjà utilisé.");
            }

            if (!personnelDAO.update(p)) {
                throw new IllegalStateException(
                        "Modification impossible : version obsolète ou fiche supprimée.");
            }

            if (p.getIdUtilisateur() != null) {
                rafraichisseur.rafraichirPourUtilisateur(p.getIdUtilisateur());
            }
        }, onSucces, onErreur);
    }

    /** Enregistre une sortie (départ) : date, motif, statut SORTI. */
    public void enregistrerDepartAsync(String idPersonnel, LocalDate dateDepart,
                                       String motifDepart,
                                       Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("personnel.modifier");

            Personnel p = personnelDAO.findById(idPersonnel)
                    .orElseThrow(() -> new IllegalStateException("Personnel introuvable."));

            if (dateDepart == null) {
                throw new IllegalArgumentException("La date de départ est obligatoire.");
            }
            if (p.getDateEmbauche() != null && dateDepart.isBefore(p.getDateEmbauche())) {
                throw new IllegalArgumentException("La date de départ ne peut pas précéder l'embauche.");
            }
            if (motifDepart == null || motifDepart.isBlank()) {
                throw new IllegalArgumentException("Le motif de départ est obligatoire.");
            }

            p.setDateDepart(dateDepart);
            p.setMotifDepart(motifDepart);
            p.setStatutEmploi("SORTI");
            p.setEstActif(false);

            if (!personnelDAO.update(p)) {
                throw new IllegalStateException("Enregistrement du départ impossible.");
            }

            if (p.getIdUtilisateur() != null) {
                rafraichisseur.rafraichirPourUtilisateur(p.getIdUtilisateur());
            }
        }, onSucces, onErreur);
    }

    /** Change uniquement le statut d'emploi (ACTIF, SUSPENDU, SORTI). */
    public void changerStatutEmploiAsync(String idPersonnel, String nouveauStatut,
                                         Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("personnel.modifier");

            if (!Set.of("ACTIF", "SUSPENDU", "SORTI").contains(nouveauStatut)) {
                throw new IllegalArgumentException("Statut d'emploi invalide : " + nouveauStatut);
            }

            Personnel p = personnelDAO.findById(idPersonnel)
                    .orElseThrow(() -> new IllegalStateException("Personnel introuvable."));

            p.setStatutEmploi(nouveauStatut);
            if ("SORTI".equals(nouveauStatut)) {
                p.setEstActif(false);
            }

            if (!personnelDAO.update(p)) {
                throw new IllegalStateException("Changement de statut impossible.");
            }

            if (p.getIdUtilisateur() != null) {
                rafraichisseur.rafraichirPourUtilisateur(p.getIdUtilisateur());
            }
        }, onSucces, onErreur);
    }

    /** Réactive un employé suspendu ou sorti. */
    public void reactiverAsync(String idPersonnel,
                               Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("personnel.modifier");

            Personnel p = personnelDAO.findById(idPersonnel)
                    .orElseThrow(() -> new IllegalStateException("Personnel introuvable."));

            p.setStatutEmploi("ACTIF");
            p.setEstActif(true);
            p.setDateDepart(null);
            p.setMotifDepart(null);

            if (!personnelDAO.update(p)) {
                throw new IllegalStateException("Réactivation impossible.");
            }

            if (p.getIdUtilisateur() != null) {
                rafraichisseur.rafraichirPourUtilisateur(p.getIdUtilisateur());
            }
        }, onSucces, onErreur);
    }

    // ==========================================
    // LIAISON AVEC UN COMPTE UTILISATEUR
    // ==========================================
    public void lierUtilisateurAsync(String idPersonnel, String idUtilisateur,
                                     Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("personnel.modifier");
            Autorisation.exigerAction("user.creer");

            Personnel p = personnelDAO.findById(idPersonnel)
                    .orElseThrow(() -> new IllegalStateException("Personnel introuvable."));

            if (p.getIdUtilisateur() != null) {
                throw new IllegalStateException(
                        "Ce personnel est déjà lié à un compte utilisateur.");
            }

            if (utilisateurDAO.findById(idUtilisateur).isEmpty()) {
                throw new IllegalStateException("Utilisateur introuvable.");
            }

            if (!personnelDAO.lierUtilisateur(idPersonnel, idUtilisateur)) {
                throw new IllegalStateException("Liaison impossible.");
            }

            rafraichisseur.rafraichirPourUtilisateur(idUtilisateur);
        }, onSucces, onErreur);
    }

    // ==========================================
    // SUPPRESSION (SOFT)
    // ==========================================
    public void supprimerAsync(String idPersonnel,
                               Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("personnel.supprimer");

            Personnel p = personnelDAO.findById(idPersonnel)
                    .orElseThrow(() -> new IllegalStateException("Personnel introuvable."));

            // Vérifier qu'aucun subordonné actif ne dépend de lui
            List<Personnel> subordonnes = personnelDAO.findByResponsable(idPersonnel);
            if (!subordonnes.isEmpty()) {
                throw new IllegalStateException(
                        "Impossible de supprimer : " + subordonnes.size()
                                + " employé(s) sont sous sa responsabilité.");
            }

            if (!personnelDAO.softDelete(idPersonnel)) {
                throw new IllegalStateException("Suppression impossible.");
            }

            if (p.getIdUtilisateur() != null) {
                rafraichisseur.rafraichirPourUtilisateur(p.getIdUtilisateur());
            }
        }, onSucces, onErreur);
    }

    // ==========================================
    // ACTIONS PAR LOT
    // ==========================================
    public void changerStatutEmploiEnMasseAsync(Set<String> ids, String statut,
                                                Runnable onSucces,
                                                Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("personnel.modifier");

            if (!Set.of("ACTIF", "SUSPENDU", "SORTI").contains(statut)) {
                throw new IllegalArgumentException("Statut d'emploi invalide : " + statut);
            }

            for (String id : ids) {
                personnelDAO.findById(id).ifPresent(p -> {
                    p.setStatutEmploi(statut);
                    if ("SORTI".equals(statut)) p.setEstActif(false);
                    personnelDAO.update(p);
                    if (p.getIdUtilisateur() != null) {
                        rafraichisseur.rafraichirPourUtilisateur(p.getIdUtilisateur());
                    }
                });
            }
        }, onSucces, onErreur);
    }

    public void supprimerEnMasseAsync(Set<String> ids,
                                      Runnable onSucces,
                                      Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("personnel.supprimer");

            for (String id : ids) {
                Optional<Personnel> opt = personnelDAO.findById(id);
                if (opt.isEmpty()) continue;

                // Refus si subordonnés actifs
                if (!personnelDAO.findByResponsable(id).isEmpty()) continue;

                personnelDAO.softDelete(id);
                if (opt.get().getIdUtilisateur() != null) {
                    rafraichisseur.rafraichirPourUtilisateur(opt.get().getIdUtilisateur());
                }
            }
        }, onSucces, onErreur);
    }

    // ==========================================
    // VALIDATION / CONTRÔLES MÉTIER
    // ==========================================
    public void verifierMatriculeDisponibleAsync(String idEcole, String matricule,
                                                 String idExclu,
                                                 Consumer<Boolean> onSucces,
                                                 Consumer<Throwable> onErreur) {
        executer(() -> !personnelDAO.existeMatricule(idEcole, matricule, idExclu),
                onSucces, onErreur);
    }

    public void verifierNumCnpsDisponibleAsync(String idEcole, String numCnps,
                                               String idExclu,
                                               Consumer<Boolean> onSucces,
                                               Consumer<Throwable> onErreur) {
        executer(() -> !personnelDAO.existeNumCnps(idEcole, numCnps, idExclu),
                onSucces, onErreur);
    }

    // ==========================================
    // STATISTIQUES (pour le tableau de bord)
    // ==========================================
    public void compterEffectifAsync(String idEcole,
                                     Consumer<Integer> onSucces,
                                     Consumer<Throwable> onErreur) {
        executer(() -> personnelDAO.findByEcole(idEcole).size(), onSucces, onErreur);
    }

    public void compterParStatutAsync(String idEcole, String statutEmploi,
                                      Consumer<Integer> onSucces,
                                      Consumer<Throwable> onErreur) {
        executer(() -> personnelDAO.findByEcoleEtStatut(idEcole, statutEmploi).size(),
                onSucces, onErreur);
    }

    public void compterParDepartementAsync(String idEcole, String idDepartement,
                                           Consumer<Integer> onSucces,
                                           Consumer<Throwable> onErreur) {
        executer(() -> personnelDAO.findByDepartement(idEcole, idDepartement).size(),
                onSucces, onErreur);
    }

    public void chargerSansCompteObservable(String idEcole) {
        // méthode utilitaire synchrone utilisée par certains écrans (legacy)
        FXCollections.observableArrayList(personnelDAO.findSansCompte(idEcole));
    }
}