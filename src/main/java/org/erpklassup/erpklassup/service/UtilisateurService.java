package org.erpklassup.erpklassup.service;

import org.erpklassup.erpklassup.dao.RoleDAO;
import org.erpklassup.erpklassup.dao.UtilisateurDAO;
import org.erpklassup.erpklassup.dao.UtilisateurRoleDAO;
import org.erpklassup.erpklassup.dto.*;
import org.erpklassup.erpklassup.models.Utilisateur;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class UtilisateurService extends ServiceAsyncBase {

    private final UtilisateurDAO utilisateurDAO;
    private final RoleDAO roleDAO;
    private final UtilisateurRoleDAO utilisateurRoleDAO;

    public UtilisateurService() {
        this(new UtilisateurDAO(), new RoleDAO(), new UtilisateurRoleDAO());
    }

    public UtilisateurService(UtilisateurDAO utilisateurDAO, RoleDAO roleDAO, UtilisateurRoleDAO utilisateurRoleDAO) {
        this.utilisateurDAO = utilisateurDAO;
        this.roleDAO = roleDAO;
        this.utilisateurRoleDAO = utilisateurRoleDAO;
    }

    // Lecture : pas de contrôle d'action, la visibilité de l'écran suffit (déjà gérée en amont)
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
            Autorisation.exigerAction("UTILISATEUR_CREER");
            Utilisateur u = new Utilisateur(username, nom);
            u.setIdEcole(idEcole);
            u.setPrenom(prenom);
            u.setEmail(email);
            u.setPasswordHash(PasswordService.hacher(motDePasse));
            if (!utilisateurDAO.create(u)) throw new IllegalStateException("Identifiant déjà utilisé ou création impossible.");
            if (idRole != null) utilisateurRoleDAO.assignerRole(u.getIdUtilisateur(), idRole, idEcole);
        }, onSucces, onErreur);
    }

    public void modifierUtilisateurAsync(String idUtilisateur, String nom, String prenom, String email,
                                         Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("UTILISATEUR_MODIFIER");
            Utilisateur u = utilisateurDAO.findById(idUtilisateur)
                    .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable."));
            u.setNom(nom);
            u.setPrenom(prenom);
            u.setEmail(email);
            utilisateurDAO.update(u);
        }, onSucces, onErreur);
    }

    public void reinitialiserMotDePasseAsync(String idUtilisateur, String nouveauMotDePasse,
                                             Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("UTILISATEUR_REINITIALISER_MDP");
            utilisateurDAO.updatePassword(idUtilisateur, PasswordService.hacher(nouveauMotDePasse));
        }, onSucces, onErreur);
    }

    public void verrouillerAsync(String idUtilisateur, int dureeMinutes, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("UTILISATEUR_VERROUILLER");
            utilisateurDAO.verrouillerCompte(idUtilisateur, dureeMinutes);
        }, onSucces, onErreur);
    }

    public void deverrouillerAsync(String idUtilisateur, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("UTILISATEUR_VERROUILLER");
            utilisateurDAO.deverrouillerCompte(idUtilisateur);
        }, onSucces, onErreur);
    }

    public void changerStatutActifAsync(String idUtilisateur, boolean actif, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("UTILISATEUR_ACTIVER_DESACTIVER");
            Utilisateur u = utilisateurDAO.findById(idUtilisateur)
                    .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable."));
            u.setEstActif(actif);
            utilisateurDAO.update(u);
        }, onSucces, onErreur);
    }

    public void supprimerAsync(String idUtilisateur, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("UTILISATEUR_SUPPRIMER");
            utilisateurDAO.softDelete(idUtilisateur);
        }, onSucces, onErreur);
    }

    public void changerStatutActifEnMasseAsync(Set<String> ids, boolean actif, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("UTILISATEUR_ACTIVER_DESACTIVER");
            for (String id : ids) utilisateurDAO.findById(id).ifPresent(u -> { u.setEstActif(actif); utilisateurDAO.update(u); });
        }, onSucces, onErreur);
    }

    public void supprimerEnMasseAsync(Set<String> ids, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
            Autorisation.exigerAction("UTILISATEUR_SUPPRIMER");
            for (String id : ids) utilisateurDAO.softDelete(id);
        }, onSucces, onErreur);
    }
}

//package org.erpklassup.erpklassup.service;
//
//import org.erpklassup.erpklassup.dao.RoleDAO;
//import org.erpklassup.erpklassup.dao.UtilisateurDAO;
//import org.erpklassup.erpklassup.dao.UtilisateurRoleDAO;
//import org.erpklassup.erpklassup.dto.*;
//import org.erpklassup.erpklassup.models.Utilisateur;
//
//import java.util.List;
//import java.util.Set;
//import java.util.function.Consumer;
//
//public class UtilisateurService extends ServiceAsyncBase {
//
//    private final UtilisateurDAO utilisateurDAO;
//    private final RoleDAO roleDAO;
//    private final UtilisateurRoleDAO utilisateurRoleDAO;
//
//    public UtilisateurService() {
//        this(new UtilisateurDAO(), new RoleDAO(), new UtilisateurRoleDAO());
//    }
//
//    public UtilisateurService(UtilisateurDAO utilisateurDAO, RoleDAO roleDAO, UtilisateurRoleDAO utilisateurRoleDAO) {
//        this.utilisateurDAO = utilisateurDAO;
//        this.roleDAO = roleDAO;
//        this.utilisateurRoleDAO = utilisateurRoleDAO;
//    }
//
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
//            Utilisateur u = new Utilisateur(username, nom);
//            u.setIdEcole(idEcole);
//            u.setPrenom(prenom);
//            u.setEmail(email);
//            u.setPasswordHash(PasswordService.hacher(motDePasse));
//            if (!utilisateurDAO.create(u)) throw new IllegalStateException("Identifiant déjà utilisé ou création impossible.");
//            if (idRole != null) utilisateurRoleDAO.assignerRole(u.getIdUtilisateur(), idRole, idEcole);
//        }, onSucces, onErreur);
//    }
//
//    public void modifierUtilisateurAsync(String idUtilisateur, String nom, String prenom, String email,
//                                         Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//            Utilisateur u = utilisateurDAO.findById(idUtilisateur)
//                    .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable."));
//            u.setNom(nom);
//            u.setPrenom(prenom);
//            u.setEmail(email);
//            utilisateurDAO.update(u);
//        }, onSucces, onErreur);
//    }
//
//    public void reinitialiserMotDePasseAsync(String idUtilisateur, String nouveauMotDePasse,
//                                             Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> utilisateurDAO.updatePassword(idUtilisateur, PasswordService.hacher(nouveauMotDePasse)),
//                onSucces, onErreur);
//    }
//
//    public void verrouillerAsync(String idUtilisateur, int dureeMinutes, Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> utilisateurDAO.verrouillerCompte(idUtilisateur, dureeMinutes), onSucces, onErreur);
//    }
//
//    public void deverrouillerAsync(String idUtilisateur, Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> utilisateurDAO.deverrouillerCompte(idUtilisateur), onSucces, onErreur);
//    }
//
//    public void changerStatutActifAsync(String idUtilisateur, boolean actif, Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//            Utilisateur u = utilisateurDAO.findById(idUtilisateur)
//                    .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable."));
//            u.setEstActif(actif);
//            utilisateurDAO.update(u);
//        }, onSucces, onErreur);
//    }
//
//    public void supprimerAsync(String idUtilisateur, Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> utilisateurDAO.softDelete(idUtilisateur), onSucces, onErreur);
//    }
//
//    // ---------- Actions par lot ----------
//
//    public void changerStatutActifEnMasseAsync(Set<String> ids, boolean actif, Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> {
//            for (String id : ids) {
//                utilisateurDAO.findById(id).ifPresent(u -> { u.setEstActif(actif); utilisateurDAO.update(u); });
//            }
//        }, onSucces, onErreur);
//    }
//
//    public void supprimerEnMasseAsync(Set<String> ids, Runnable onSucces, Consumer<Throwable> onErreur) {
//        executerSansRetour(() -> { for (String id : ids) utilisateurDAO.softDelete(id); }, onSucces, onErreur);
//    }
//}