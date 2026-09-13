package org.erpklassup.erpklassup.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.erpklassup.erpklassup.dao.EleveDAO;
import org.erpklassup.erpklassup.dao.ParentDAO;
import org.erpklassup.erpklassup.dao.PersonnelDAO;
import org.erpklassup.erpklassup.dao.RoleDAO;
import org.erpklassup.erpklassup.dao.UtilisateurDAO;
import org.erpklassup.erpklassup.dao.UtilisateurRoleDAO;
import org.erpklassup.erpklassup.dto.*;
import org.erpklassup.erpklassup.models.Utilisateur;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class UtilisateurService extends ServiceAsyncBase {

    private final UtilisateurDAO utilisateurDAO;
    private final RoleDAO roleDAO;
    private final UtilisateurRoleDAO utilisateurRoleDAO;
    private final RafraichisseurPermissions rafraichisseur;

    private final PersonnelDAO personnelDAO;
    private final EleveDAO eleveDAO;
    private final ParentDAO parentDAO;

    public UtilisateurService() {
        this(new UtilisateurDAO(), new RoleDAO(), new UtilisateurRoleDAO(), new RafraichisseurPermissions(),
                new PersonnelDAO(), new EleveDAO(), new ParentDAO());
    }

    /**
     * Conservé pour compatibilité avec le code existant qui construit le service
     * avec seulement ces 4 dépendances (ex : tests).
     */
    public UtilisateurService(UtilisateurDAO utilisateurDAO, RoleDAO roleDAO, UtilisateurRoleDAO utilisateurRoleDAO,
                              RafraichisseurPermissions rafraichisseur) {
        this(utilisateurDAO, roleDAO, utilisateurRoleDAO, rafraichisseur,
                new PersonnelDAO(), new EleveDAO(), new ParentDAO());
    }

    public UtilisateurService(UtilisateurDAO utilisateurDAO, RoleDAO roleDAO, UtilisateurRoleDAO utilisateurRoleDAO,
                              RafraichisseurPermissions rafraichisseur, PersonnelDAO personnelDAO,
                              EleveDAO eleveDAO, ParentDAO parentDAO) {
        this.utilisateurDAO = utilisateurDAO;
        this.roleDAO = roleDAO;
        this.utilisateurRoleDAO = utilisateurRoleDAO;
        this.rafraichisseur = rafraichisseur;
        this.personnelDAO = personnelDAO;
        this.eleveDAO = eleveDAO;
        this.parentDAO = parentDAO;
    }

    public void rechercherAsync(FiltreUtilisateur filtre,
                                Consumer<ResultatPagine<UtilisateurLigne>> onSucces,
                                Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(() -> utilisateurDAO.rechercherPagine(filtre),
                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
                erreur -> { if (estRequeteActuelle(jeton)) onErreur.accept(erreur); });
    }

    public void chargerRolesAsync(String idEcole, Consumer<List<RoleOption>> onSucces, Consumer<Throwable> onErreur) {
        executer(() -> roleDAO.listerParEcole(idEcole), onSucces, onErreur);
    }

    public void creerUtilisateurAsync(String idEcole, String username, String motDePasse, String nom, String prenom,
                                      String email, String idRole, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("user.creer");
            Utilisateur u = new Utilisateur(username, nom);
            u.setIdEcole(idEcole);
            u.setPrenom(prenom);
            u.setEmail(email);
            u.setPasswordHash(PasswordService.hacher(motDePasse));
            if (!utilisateurDAO.create(u)) throw new IllegalStateException("Identifiant déjà utilisé ou création impossible.");
            if (idRole != null) {
                utilisateurRoleDAO.assignerRole(u.getIdUtilisateur(), idRole, idEcole);
                rafraichisseur.rafraichirPourUtilisateur(u.getIdUtilisateur());
            }
        }, onSucces, onErreur);
    }

    public void modifierUtilisateurAsync(String idUtilisateur, String nom, String prenom, String email, String numero,
                                         Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("user.modifier");
            Utilisateur u = utilisateurDAO.findById(idUtilisateur)
                    .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable."));
            u.setNom(nom);
            u.setPrenom(prenom);
            u.setEmail(email);
            u.setTelephone(numero);
            utilisateurDAO.update(u);
            rafraichisseur.rafraichirPourUtilisateur(idUtilisateur);
        }, onSucces, onErreur);
    }

    public void reinitialiserMotDePasseAsync(String idUtilisateur, String nouveauMotDePasse,
                                             Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("user.reset_mdp");
            utilisateurDAO.updatePassword(idUtilisateur, PasswordService.hacher(nouveauMotDePasse));
        }, onSucces, onErreur);
    }

    public void verrouillerAsync(String idUtilisateur, int dureeMinutes, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("user.verrouiller");
            utilisateurDAO.verrouillerCompte(idUtilisateur, dureeMinutes);
            rafraichisseur.rafraichirPourUtilisateur(idUtilisateur);
        }, onSucces, onErreur);
    }

    public void deverrouillerAsync(String idUtilisateur, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("user.deverouiller");
            utilisateurDAO.deverrouillerCompte(idUtilisateur);
            rafraichisseur.rafraichirPourUtilisateur(idUtilisateur);
        }, onSucces, onErreur);
    }

    public void changerStatutActifAsync(String idUtilisateur, boolean actif, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("user.activer");
            Utilisateur u = utilisateurDAO.findById(idUtilisateur)
                    .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable."));
            u.setEstActif(actif);
            utilisateurDAO.update(u);
            rafraichisseur.rafraichirPourUtilisateur(idUtilisateur);
        }, onSucces, onErreur);
    }

    public void supprimerAsync(String idUtilisateur, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("user.supprimer");
            utilisateurDAO.softDelete(idUtilisateur);
            rafraichisseur.rafraichirPourUtilisateur(idUtilisateur);
        }, onSucces, onErreur);
    }

    // ---------- Actions par lot ----------

    public void changerStatutActifEnMasseAsync(Set<String> ids, boolean actif, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("user.activer");
            for (String id : ids) {
                utilisateurDAO.findById(id).ifPresent(u -> {
                    u.setEstActif(actif);
                    utilisateurDAO.update(u);
                    rafraichisseur.rafraichirPourUtilisateur(id);
                });
            }
        }, onSucces, onErreur);
    }

    public void supprimerEnMasseAsync(Set<String> ids, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("user.supprimer");
            for (String id : ids) {
                utilisateurDAO.softDelete(id);
                rafraichisseur.rafraichirPourUtilisateur(id);
            }
        }, onSucces, onErreur);
    }

    public void mettreAJourRolesAsync(String idUtilisateur, String idEcole, List<String> idsRoles,
                                      Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("user.modifier");

            utilisateurRoleDAO.reinitialiserRolesUtilisateur(idUtilisateur, idEcole);

            for (String idRole : idsRoles) {
                utilisateurRoleDAO.assignerRole(idUtilisateur, idRole, idEcole);
            }

            rafraichisseur.rafraichirPourUtilisateur(idUtilisateur);
        }, onSucces, onErreur);
    }

    /**
     * Recherche les personnes (Personnel, Élève ou Parent) sans compte utilisateur.
     */
    public ObservableList<PersonneDTO> rechercherPersonnesSansCompte(String idEcole, String type, String query) throws SQLException {
        List<PersonneDTO> resultats = switch (type) {
            case "PERSONNEL" -> personnelDAO.findSansCompte(idEcole).stream()
                    .map(p -> new PersonneDTO(p.getMatriculeInterne(), p.getNom(), p.getPrenom(), p.getPosteOuFonction()))
                    .toList();
            case "ELEVE" -> eleveDAO.findSansCompte(idEcole).stream()
                    .map(e -> new PersonneDTO(e.getMatricule(), e.getNom(), e.getPrenom(),
                            e.getNomTuteur() != null ? "Tuteur : " + e.getNomTuteur() : ""))
                    .toList();
            case "PARENT" -> parentDAO.findSansCompte(idEcole).stream()
                    .map(p -> new PersonneDTO(p.getIdParent(), p.getNom(), p.getPrenom(), p.getTelephone()))
                    .toList();
            default -> List.of();
        };

        String q = query == null ? "" : query.toLowerCase();
        return FXCollections.observableArrayList(
                resultats.stream()
                        .filter(p -> q.isBlank()
                                || contient(p.getNom(), q)
                                || contient(p.getPrenom(), q)
                                || contient(p.getMatricule(), q))
                        .toList()
        );
    }

    private boolean contient(String valeur, String recherche) {
        return valeur != null && valeur.toLowerCase().contains(recherche);
    }
}




//package org.erpklassup.erpklassup.service;
//
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import org.erpklassup.erpklassup.dao.*;
//import org.erpklassup.erpklassup.dto.*;
//import org.erpklassup.erpklassup.models.Utilisateur;
//
//import java.sql.SQLException;
//import java.util.List;
//import java.util.Set;
//import java.util.function.Consumer;
//
//public class UtilisateurService extends ServiceAsyncBase {
//
//    private final UtilisateurDAO utilisateurDAO;
//    private final RoleDAO roleDAO;
//    private final UtilisateurRoleDAO utilisateurRoleDAO;
//    private final RafraichisseurPermissions rafraichisseur;
//
//    private final PersonnelDAO personnelDAO;
//    private final EleveDAO eleveDAO;
//    private final ParentDAO parentDAO;
//
//    public UtilisateurService() {
//        this(new UtilisateurDAO(), new RoleDAO(), new UtilisateurRoleDAO(), new RafraichisseurPermissions());
//    }
//
//    public UtilisateurService(UtilisateurDAO utilisateurDAO, RoleDAO roleDAO, UtilisateurRoleDAO utilisateurRoleDAO, RafraichisseurPermissions rafraichisseur) {
//        this.utilisateurDAO = utilisateurDAO;
//        this.roleDAO = roleDAO;
//        this.utilisateurRoleDAO = utilisateurRoleDAO;
//        this.rafraichisseur = rafraichisseur;
//    }
//
//    // Lecture : pas de contrôle d'action, la visibilité de l'écran suffit
//    public void rechercherAsync(FiltreUtilisateur filtre,
//                                Consumer<ResultatPagine<UtilisateurLigne>> onSucces,
//                                Consumer<Throwable> onErreur) {
//        long jeton = nouvelleRequete();
//        executer(() -> utilisateurDAO.rechercherPagine(filtre),
//                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
//                erreur -> { if (estRequeteActuelle(jeton)) onErreur.accept(erreur); });
//    }
//
//    public void chargerRolesAsync(String idEcole, Consumer<List<RoleOption>> onSucces, Consumer<Throwable> onErreur) {
//        executer(() -> roleDAO.listerParEcole(idEcole), onSucces, onErreur);
//    }
//
//    public void creerUtilisateurAsync(String idEcole, String username, String motDePasse, String nom, String prenom,
//                                      String email, String idRole, Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//            Autorisation.exigerAction("user.creer");
//            Utilisateur u = new Utilisateur(username, nom);
//            u.setIdEcole(idEcole);
//            u.setPrenom(prenom);
//            u.setEmail(email);
//            u.setPasswordHash(PasswordService.hacher(motDePasse));
//            if (!utilisateurDAO.create(u)) throw new IllegalStateException("Identifiant déjà utilisé ou création impossible.");
//            if (idRole != null) {
//                utilisateurRoleDAO.assignerRole(u.getIdUtilisateur(), idRole, idEcole);
//                rafraichisseur.rafraichirPourUtilisateur(u.getIdUtilisateur());
//            }
//        }, onSucces, onErreur);
//    }
//
//    public void modifierUtilisateurAsync(String idUtilisateur, String nom, String prenom, String email,
//                                         Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//            Autorisation.exigerAction("user.modifier");
//            Utilisateur u = utilisateurDAO.findById(idUtilisateur)
//                    .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable."));
//            u.setNom(nom);
//            u.setPrenom(prenom);
//            u.setEmail(email);
//            utilisateurDAO.update(u);
//            rafraichisseur.rafraichirPourUtilisateur(idUtilisateur);
//        }, onSucces, onErreur);
//    }
//
//    public void reinitialiserMotDePasseAsync(String idUtilisateur, String nouveauMotDePasse,
//                                             Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//            Autorisation.exigerAction("user.reset_mdp");
//            utilisateurDAO.updatePassword(idUtilisateur, PasswordService.hacher(nouveauMotDePasse));
//        }, onSucces, onErreur);
//    }
//
//    public void verrouillerAsync(String idUtilisateur, int dureeMinutes, Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//            Autorisation.exigerAction("user.verrouiller");
//            utilisateurDAO.verrouillerCompte(idUtilisateur, dureeMinutes);
//            rafraichisseur.rafraichirPourUtilisateur(idUtilisateur);
//        }, onSucces, onErreur);
//    }
//
//    public void deverrouillerAsync(String idUtilisateur, Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//            Autorisation.exigerAction("user.deverouiller");
//            utilisateurDAO.deverrouillerCompte(idUtilisateur);
//            rafraichisseur.rafraichirPourUtilisateur(idUtilisateur);
//        }, onSucces, onErreur);
//    }
//
//    public void changerStatutActifAsync(String idUtilisateur, boolean actif, Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//            Autorisation.exigerAction("user.activer");
//            Utilisateur u = utilisateurDAO.findById(idUtilisateur)
//                    .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable."));
//            u.setEstActif(actif);
//            utilisateurDAO.update(u);
//            rafraichisseur.rafraichirPourUtilisateur(idUtilisateur);
//        }, onSucces, onErreur);
//    }
//
//    public void supprimerAsync(String idUtilisateur, Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//            Autorisation.exigerAction("user.supprimer");
//            utilisateurDAO.softDelete(idUtilisateur);
//            rafraichisseur.rafraichirPourUtilisateur(idUtilisateur);
//        }, onSucces, onErreur);
//    }
//
//    // ---------- Actions par lot ----------
//
//    public void changerStatutActifEnMasseAsync(Set<String> ids, boolean actif, Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//            Autorisation.exigerAction("user.activer");
//            for (String id : ids) {
//                utilisateurDAO.findById(id).ifPresent(u -> {
//                    u.setEstActif(actif);
//                    utilisateurDAO.update(u);
//                    rafraichisseur.rafraichirPourUtilisateur(id);
//                });
//            }
//        }, onSucces, onErreur);
//    }
//
//    public void supprimerEnMasseAsync(Set<String> ids, Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//            Autorisation.exigerAction("user.supprimer");
//            for (String id : ids) {
//                utilisateurDAO.softDelete(id);
//                rafraichisseur.rafraichirPourUtilisateur(id);
//            }
//        }, onSucces, onErreur);
//    }
//
//    public void mettreAJourRolesAsync(String idUtilisateur, String idEcole, List<String> idsRoles,
//                                      Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//            Autorisation.exigerAction("user.modifier");
//
//            // 1. Supprimer/Remplacer les rôles actuels via le DAO (à implémenter selon votre UtilisateurRoleDAO)
//            utilisateurRoleDAO.reinitialiserRolesUtilisateur(idUtilisateur, idEcole);
//
//            // 2. Assigner les nouveaux rôles sélectionnés
//            for (String idRole : idsRoles) {
//                utilisateurRoleDAO.assignerRole(idUtilisateur, idRole, idEcole);
//            }
//
//            // 3. Rafraîchir le cache des permissions
//            rafraichisseur.rafraichirPourUtilisateur(idUtilisateur);
//        }, onSucces, onErreur);
//    }
//
//    // À ajouter dans UtilisateurService
//    /**
//     * Recherche les personnes (Personnel, Élève ou Parent) sans compte utilisateur,
//     * pour l'étape 2 de la création de compte. Méthode synchrone volontairement :
//     * le contrôleur l'appelle déjà depuis son propre Task (voir
//     * UserCreateController.effectuerRechercheAsync), donc l'enrober une seconde
//     * fois dans un Task ici serait redondant.
//     *
//     * NB Élève : Eleve n'a pas de champ "classe" en base (la classe vit dans
//     * Inscription via la table de liaison), donc infoSup affiche le tuteur en
//     * attendant. Si tu veux la classe réelle, il faudra une méthode DAO qui
//     * joint Eleve + Inscription + Classe.
//     */
//    public ObservableList<PersonneDTO> rechercherPersonnesSansCompte(String idEcole, String type, String query) throws SQLException {
//        List<PersonneDTO> resultats = switch (type) {
//            case "PERSONNEL" -> personnelDAO.findSansCompte(idEcole).stream()
//                    .map(p -> new PersonneDTO(p.getMatriculeInterne(), p.getNom(), p.getPrenom(), p.getPosteOuFonction()))
//                    .toList();
//            case "ELEVE" -> eleveDAO.findSansCompte(idEcole).stream()
//                    .map(e -> new PersonneDTO(e.getMatricule(), e.getNom(), e.getPrenom(),
//                            e.getNomTuteur() != null ? "Tuteur : " + e.getNomTuteur() : ""))
//                    .toList();
//            case "PARENT" -> parentDAO.findSansCompte(idEcole).stream()
//                    .map(p -> new PersonneDTO(p.getIdParent(), p.getNom(), p.getPrenom(), p.getTelephone()))
//                    .toList();
//            default -> List.of();
//        };
//
//        String q = query == null ? "" : query.toLowerCase();
//        return FXCollections.observableArrayList(
//                resultats.stream()
//                        .filter(p -> q.isBlank()
//                                || contient(p.getNom(), q)
//                                || contient(p.getPrenom(), q)
//                                || contient(p.getMatricule(), q))
//                        .toList()
//        );
//    }
//
//    /** Comparaison null-safe : nom/prenom/telephone peuvent être null en base (ex: Parent.prenom). */
//    private boolean contient(String valeur, String recherche) {
//        return valeur != null && valeur.toLowerCase().contains(recherche);
//    }
//}
//}