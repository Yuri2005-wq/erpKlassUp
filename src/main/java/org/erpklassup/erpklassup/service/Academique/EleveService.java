package org.erpklassup.erpklassup.service.Academique;

import org.erpklassup.erpklassup.dao.Academique.EleveDAO;
import org.erpklassup.erpklassup.models.Academique.Eleve;
import org.erpklassup.erpklassup.service.ServiceAsyncBase;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class EleveService extends ServiceAsyncBase {

    private final EleveDAO eleveDAO;

    public EleveService() {
        this(new EleveDAO());
    }

    public EleveService(EleveDAO eleveDAO) {
        this.eleveDAO = eleveDAO;
    }

    public Optional<Eleve> charger(String idEcole, String idEleve) {
        validerIdentifiant(idEcole, "L'école");
        validerIdentifiant(idEleve, "L'élève");
        return eleveDAO.findById(idEcole, idEleve);
    }

    public void chargerAsync(String idEcole, String idEleve,
                             Consumer<Optional<Eleve>> onSucces,
                             Consumer<Throwable> onErreur) {
        executer(() -> charger(idEcole, idEleve), onSucces, onErreur);
    }

    public Optional<Eleve> chargerParUtilisateur(String idUtilisateur) {
        validerIdentifiant(idUtilisateur, "L'utilisateur");
        return eleveDAO.findByUtilisateurId(idUtilisateur);
    }

    public void chargerParUtilisateurAsync(String idUtilisateur,
                                           Consumer<Optional<Eleve>> onSucces,
                                           Consumer<Throwable> onErreur) {
        executer(() -> chargerParUtilisateur(idUtilisateur), onSucces, onErreur);
    }

    public Optional<Eleve> chargerParMatricule(String idEcole, String matricule) {
        validerIdentifiant(idEcole, "L'école");
        validerIdentifiant(matricule, "Le matricule");
        return eleveDAO.findByMatricule(idEcole, matricule.trim());
    }

    public void chargerParMatriculeAsync(String idEcole, String matricule,
                                         Consumer<Optional<Eleve>> onSucces,
                                         Consumer<Throwable> onErreur) {
        executer(() -> chargerParMatricule(idEcole, matricule), onSucces, onErreur);
    }

    public List<Eleve> listerParEcole(String idEcole) {
        validerIdentifiant(idEcole, "L'école");
        return eleveDAO.findByEcole(idEcole);
    }

    public void listerParEcoleAsync(String idEcole, Consumer<List<Eleve>> onSucces,
                                    Consumer<Throwable> onErreur) {
        executer(() -> listerParEcole(idEcole), onSucces, onErreur);
    }

    public List<Eleve> listerSansCompte(String idEcole) {
        validerIdentifiant(idEcole, "L'école");
        return eleveDAO.findSansCompte(idEcole);
    }

    public void listerSansCompteAsync(String idEcole, Consumer<List<Eleve>> onSucces,
                                      Consumer<Throwable> onErreur) {
        executer(() -> listerSansCompte(idEcole), onSucces, onErreur);
    }

    public List<Eleve> rechercher(String idEcole, String terme) {
        validerIdentifiant(idEcole, "L'école");
        return eleveDAO.rechercher(idEcole, terme);
    }

    public void rechercherAsync(String idEcole, String terme,
                                Consumer<List<Eleve>> onSucces,
                                Consumer<Throwable> onErreur) {
        executer(() -> rechercher(idEcole, terme), onSucces, onErreur);
    }

    public void creer(Eleve eleve) {
        valider(eleve);
        if (eleveDAO.existeMatricule(eleve.getIdEcole(), eleve.getMatricule(), null)) {
            throw new IllegalArgumentException("Ce matricule est déjà utilisé dans cette école.");
        }
        if (!eleveDAO.insert(eleve)) {
            throw new IllegalStateException("Impossible de créer l'élève (matricule déjà utilisé ou erreur de base de données).");
        }
    }

    public void creerAsync(Eleve eleve, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> creer(eleve), onSucces, onErreur);
    }

    public void modifier(Eleve eleve) {
        valider(eleve);
        validerIdentifiant(eleve.getIdEleve(), "L'identifiant de l'élève");
        if (eleveDAO.existeMatricule(eleve.getIdEcole(), eleve.getMatricule(), eleve.getIdEleve())) {
            throw new IllegalArgumentException("Ce matricule est déjà utilisé dans cette école.");
        }
        if (!eleveDAO.update(eleve)) {
            throw new IllegalStateException(
                    "Impossible de modifier l'élève : il a peut-être été supprimé ou modifié par un autre utilisateur.");
        }
    }

    public void modifierAsync(Eleve eleve, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> modifier(eleve), onSucces, onErreur);
    }

    public void supprimer(String idEcole, String idEleve) {
        validerIdentifiant(idEcole, "L'école");
        validerIdentifiant(idEleve, "L'élève");
        Eleve eleve = eleveDAO.findById(idEcole, idEleve)
                .orElseThrow(() -> new IllegalStateException("Élève introuvable dans cette école."));
        long version = eleve.getVersion() != null ? eleve.getVersion() : 1L;
        if (!eleveDAO.softDelete(idEcole, idEleve, version)) {
            throw new IllegalStateException(
                    "Impossible de supprimer l'élève : il a peut-être été supprimé ou modifié par un autre utilisateur.");
        }
    }

    public void supprimerAsync(String idEcole, String idEleve,
                               Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> supprimer(idEcole, idEleve), onSucces, onErreur);
    }

    public void lierUtilisateur(String idEcole, String idEleve, String idUtilisateur) {
        validerIdentifiant(idEcole, "L'école");
        validerIdentifiant(idEleve, "L'élève");
        String utilisateurLie = idUtilisateur == null || idUtilisateur.isBlank()
                ? null
                : idUtilisateur.trim();
        validerLongueur(utilisateurLie, 50, "L'identifiant de l'utilisateur");
        Eleve eleve = eleveDAO.findById(idEcole, idEleve)
                .orElseThrow(() -> new IllegalStateException("Élève introuvable dans cette école."));
        long version = eleve.getVersion() != null ? eleve.getVersion() : 1L;
        if (!eleveDAO.lierUtilisateur(idEcole, idEleve, utilisateurLie, version)) {
            throw new IllegalStateException(
                    "Impossible de lier le compte : l'élève a peut-être été modifié par un autre utilisateur.");
        }
        eleve.setVersion(version + 1);
    }

    public void delierUtilisateur(String idEcole, String idEleve) {
        lierUtilisateur(idEcole, idEleve, null);
    }

    public void lierUtilisateurAsync(String idEcole, String idEleve, String idUtilisateur,
                                     Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> lierUtilisateur(idEcole, idEleve, idUtilisateur), onSucces, onErreur);
    }

    public void delierUtilisateurAsync(String idEcole, String idEleve,
                                       Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> delierUtilisateur(idEcole, idEleve), onSucces, onErreur);
    }

    private void valider(Eleve eleve) {
        if (eleve == null) throw new IllegalArgumentException("L'élève est obligatoire.");
        validerIdentifiant(eleve.getIdEcole(), "L'école");
        validerLongueur(eleve.getIdEcole(), 50, "L'identifiant de l'école");
        validerLongueur(eleve.getIdUtilisateur(), 50, "L'identifiant de l'utilisateur");
        validerChamp(eleve.getMatricule(), 50, "Le matricule");
        validerChamp(eleve.getNom(), 50, "Le nom");
        validerChamp(eleve.getPrenom(), 50, "Le prénom");
        validerChamp(eleve.getSexe(), 10, "Le sexe");
        validerLongueur(eleve.getLieuNaissance(), 100, "Le lieu de naissance");
        validerLongueur(eleve.getNationalite(), 50, "La nationalité");
        validerLongueur(eleve.getNomTuteur(), 100, "Le nom du tuteur");
        validerLongueur(eleve.getTelephoneTuteur(), 30, "Le téléphone du tuteur");
        validerLongueur(eleve.getEmailTuteur(), 100, "L'adresse e-mail du tuteur");
        validerLongueur(eleve.getPhotoPath(), 255, "Le chemin de la photo");
        if (eleve.getDateNaissance() != null && eleve.getDateNaissance().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de naissance ne peut pas être dans le futur.");
        }

        eleve.setMatricule(eleve.getMatricule().trim());
        eleve.setNom(eleve.getNom().trim());
        eleve.setPrenom(eleve.getPrenom().trim());
        eleve.setSexe(eleve.getSexe().trim());
        if (eleve.getNationalite() == null || eleve.getNationalite().isBlank()) {
            eleve.setNationalite("Camerounaise");
        } else {
            eleve.setNationalite(eleve.getNationalite().trim());
        }
    }

    private void validerChamp(String valeur, int longueurMax, String libelle) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(libelle + " est obligatoire.");
        }
        validerLongueur(valeur, longueurMax, libelle);
    }

    private void validerLongueur(String valeur, int longueurMax, String libelle) {
        if (valeur != null && valeur.length() > longueurMax) {
            throw new IllegalArgumentException(libelle + " ne peut pas dépasser " + longueurMax + " caractères.");
        }
    }

    private void validerIdentifiant(String valeur, String libelle) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(libelle + " est obligatoire.");
        }
    }
}
