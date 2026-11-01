package org.erpklassup.erpklassup.service.GRH;

import org.erpklassup.erpklassup.dao.GRH.PosteDAO;
import org.erpklassup.erpklassup.dao.PersonnelDAO;
import org.erpklassup.erpklassup.dto.FiltrePoste;
import org.erpklassup.erpklassup.dto.PosteLigne;
import org.erpklassup.erpklassup.dto.ResultatPagine;
import org.erpklassup.erpklassup.models.GRH.Poste;
import org.erpklassup.erpklassup.service.RafraichisseurPermissions;
import org.erpklassup.erpklassup.service.ServiceAsyncBase;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class PosteService extends ServiceAsyncBase {

    private final PosteDAO posteDAO;
    private final PersonnelDAO personnelDAO;
    private final RafraichisseurPermissions rafraichisseur;

    // ==========================================
    // CONSTRUCTEURS
    // ==========================================
    public PosteService() {
        this(new PosteDAO(), new PersonnelDAO(), new RafraichisseurPermissions());
    }

    public PosteService(PosteDAO posteDAO,
                        PersonnelDAO personnelDAO,
                        RafraichisseurPermissions rafraichisseur) {
        this.posteDAO = posteDAO;
        this.personnelDAO = personnelDAO;
        this.rafraichisseur = rafraichisseur;
    }

    // ==========================================
    // LECTURE / RECHERCHE PAGINÉE & ASYNCHRONE
    // ==========================================
    public ResultatPagine<PosteLigne> rechercher(FiltrePoste filtre) {
        // Hypothèse : méthode synchrone sur la DAO si implémentée
        return posteDAO.rechercherPagine(filtre);
    }

    public void rechercherAsync(FiltrePoste filtre,
                                Consumer<ResultatPagine<PosteLigne>> onSucces,
                                Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(() -> posteDAO.rechercherPagine(filtre),
                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
                erreur -> { if (estRequeteActuelle(jeton)) onErreur.accept(erreur); });
    }

    public void chargerTousParEcoleAsync(String idEcole,
                                         Consumer<List<Poste>> onSucces,
                                         Consumer<Throwable> onErreur) {
        executer(() -> posteDAO.findByEcole(idEcole), onSucces, onErreur);
    }

    public void chargerParCategorieAsync(String idEcole, String categorie,
                                         Consumer<List<Poste>> onSucces,
                                         Consumer<Throwable> onErreur) {
        executer(() -> posteDAO.findByCategorie(idEcole, categorie), onSucces, onErreur);
    }

    public void trouverParIdAsync(String idPoste,
                                  Consumer<Optional<Poste>> onSucces,
                                  Consumer<Throwable> onErreur) {
        executer(() -> posteDAO.findById(idPoste), onSucces, onErreur);
    }

    public void compterActifsAsync(String idEcole,
                                   Consumer<Long> onSucces,
                                   Consumer<Throwable> onErreur) {
        executer(() -> posteDAO.compterActifsParEcole(idEcole), onSucces, onErreur);
    }

    // ==========================================
    // CRÉATION / MODIFICATION / SUPPRESSION
    // ==========================================
    public void creerPosteAsync(Poste poste,
                                Runnable onSucces,
                                Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("grh.poste.creer");

            // Valider unicité de l'intitulé du poste pour cette école
            if (posteDAO.existeIntitule(poste.getIdEcole(), poste.getIntitule(), null)) {
                throw new IllegalStateException("Un poste avec l'intitulé '" + poste.getIntitule() + "' existe déjà.");
            }

            if (!posteDAO.create(poste)) {
                throw new IllegalStateException("Erreur lors de la création du poste.");
            }
        }, onSucces, onErreur);
    }

    public void modifierPosteAsync(Poste poste,
                                   Runnable onSucces,
                                   Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("grh.poste.modifier");

            // Valider que le poste existe
            Poste existant = posteDAO.findById(poste.getIdPoste())
                    .orElseThrow(() -> new IllegalStateException("Poste introuvable."));

            // Valider unicité de l'intitulé en excluant lui-même
            if (posteDAO.existeIntitule(poste.getIdEcole(), poste.getIntitule(), poste.getIdPoste())) {
                throw new IllegalStateException("L'intitulé '" + poste.getIntitule() + "' est déjà attribué à un autre poste.");
            }

            if (!posteDAO.update(poste)) {
                throw new IllegalStateException("Échec de la mise à jour du poste.");
            }
        }, onSucces, onErreur);
    }

    public void changerStatutAsync(String idPoste, boolean estActif,
                                   Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("grh.poste.modifier");

            Poste poste = posteDAO.findById(idPoste)
                    .orElseThrow(() -> new IllegalStateException("Poste introuvable."));

            poste.setEstActif(estActif);
            if (!posteDAO.update(poste)) {
                throw new IllegalStateException("Échec du changement de statut du poste.");
            }
        }, onSucces, onErreur);
    }

    public void supprimerAsync(String idPoste, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("grh.poste.supprimer");

            // Règle métier : Vérifier si du personnel occupe actuellement ce poste
            int effectifRattache = posteDAO.compterPersonnelRattache(idPoste);
            if (effectifRattache > 0) {
                throw new IllegalStateException("Impossible de supprimer ce poste car " + effectifRattache + " employé(s) y sont rattaché(s).");
            }

            if (!posteDAO.softDelete(idPoste)) {
                throw new IllegalStateException("Échec de la suppression du poste.");
            }
        }, onSucces, onErreur);
    }

    // ==========================================
    // ACTIONS PAR LOT
    // ==========================================
    public void supprimerEnMasseAsync(Set<String> idsPostes,
                                      Runnable onSucces,
                                      Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("grh.poste.supprimer");

            for (String id : idsPostes) {
                if (posteDAO.compterPersonnelRattache(id) > 0) {
                    continue; // Saute les postes occupés
                }
                posteDAO.softDelete(id);
            }
        }, onSucces, onErreur);
    }
}