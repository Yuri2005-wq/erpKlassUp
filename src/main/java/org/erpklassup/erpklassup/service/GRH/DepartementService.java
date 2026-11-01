package org.erpklassup.erpklassup.service.GRH;

import org.erpklassup.erpklassup.dao.GRH.DepartementDAO;
import org.erpklassup.erpklassup.dao.GRH.IDepartementDAO;
import org.erpklassup.erpklassup.dao.PersonnelDAO;
import org.erpklassup.erpklassup.dto.DepartementLigne;
import org.erpklassup.erpklassup.dto.FiltreDepartement;
import org.erpklassup.erpklassup.dto.ResultatPagine;
import org.erpklassup.erpklassup.enums.StatutDepartement;

import org.erpklassup.erpklassup.models.GRH.Departement;
import org.erpklassup.erpklassup.service.Autorisation;
import org.erpklassup.erpklassup.service.RafraichisseurPermissions;
import org.erpklassup.erpklassup.service.ServiceAsyncBase;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class DepartementService extends ServiceAsyncBase {

    private final IDepartementDAO departementDAO;
    private final PersonnelDAO personnelDAO;
    private final RafraichisseurPermissions rafraichisseur;

    // ==========================================
    // CONSTRUCTEURS
    // ==========================================
    public DepartementService() {
        this(new DepartementDAO(), new PersonnelDAO(), new RafraichisseurPermissions());
    }

    public DepartementService(IDepartementDAO departementDAO,
                              PersonnelDAO personnelDAO,
                              RafraichisseurPermissions rafraichisseur) {
        this.departementDAO = departementDAO;
        this.personnelDAO = personnelDAO;
        this.rafraichisseur = rafraichisseur;
    }

    // ==========================================
    // LECTURE / RECHERCHE PAGINÉE & ASYNCHRONE
    // ==========================================
    public ResultatPagine<DepartementLigne> rechercher(FiltreDepartement filtre) {
        return departementDAO.rechercherPagine(filtre);
    }

    public void rechercherAsync(FiltreDepartement filtre,
                                Consumer<ResultatPagine<DepartementLigne>> onSucces,
                                Consumer<Throwable> onErreur) {
        long jeton = nouvelleRequete();
        executer(() -> departementDAO.rechercherPagine(filtre),
                resultat -> { if (estRequeteActuelle(jeton)) onSucces.accept(resultat); },
                erreur -> { if (estRequeteActuelle(jeton)) onErreur.accept(erreur); });
    }

    public void chargerTousParEcoleAsync(String idEcole,
                                         Consumer<List<Departement>> onSucces,
                                         Consumer<Throwable> onErreur) {
        executer(() -> departementDAO.findByEcole(idEcole), onSucces, onErreur);
    }

    public void chargerRacinesAsync(String idEcole,
                                    Consumer<List<Departement>> onSucces,
                                    Consumer<Throwable> onErreur) {
        executer(() -> departementDAO.findRacines(idEcole), onSucces, onErreur);
    }

    public void chargerSousDepartementsAsync(String idEcole, String idParent,
                                             Consumer<List<Departement>> onSucces,
                                             Consumer<Throwable> onErreur) {
        executer(() -> departementDAO.findSousDepartements(idEcole, idParent), onSucces, onErreur);
    }

    public void trouverParIdAsync(String idDepartement,
                                  Consumer<Optional<Departement>> onSucces,
                                  Consumer<Throwable> onErreur) {
        executer(() -> departementDAO.findById(idDepartement), onSucces, onErreur);
    }

    public void compterActifsAsync(String idEcole,
                                   Consumer<Long> onSucces,
                                   Consumer<Throwable> onErreur) {
        executer(() -> departementDAO.compterActifsParEcole(idEcole), onSucces, onErreur);
    }

    // ==========================================
    // CRÉATION / MODIFICATION / SUPPRESSION
    // ==========================================
    public void creerDepartementAsync(Departement departement,
                                      Runnable onSucces,
                                      Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("grh.departement.creer");

            // Valider unicité du code et du libellé
            if (departementDAO.existeCode(departement.getIdEcole(), departement.getCode(), null)) {
                throw new IllegalStateException("Le code département '" + departement.getCode() + "' est déjà utilisé.");
            }
            if (departementDAO.existeLibelle(departement.getIdEcole(), departement.getLibelle(), null)) {
                throw new IllegalStateException("Un département avec le libellé '" + departement.getLibelle() + "' existe déjà.");
            }

            if (!departementDAO.create(departement)) {
                throw new IllegalStateException("Erreur lors de la création du département.");
            }
        }, onSucces, onErreur);
    }

    public void modifierDepartementAsync(Departement departement,
                                         Runnable onSucces,
                                         Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("grh.departement.modifier");

            // Valider que le département existe
            Departement existant = departementDAO.findById(departement.getIdDepartement())
                    .orElseThrow(() -> new IllegalStateException("Département introuvable."));

            // Valider unicité du code et du libellé en excluant lui-même
            if (departementDAO.existeCode(departement.getIdEcole(), departement.getCode(), departement.getIdDepartement())) {
                throw new IllegalStateException("Le code '" + departement.getCode() + "' est déjà utilisé par un autre département.");
            }
            if (departementDAO.existeLibelle(departement.getIdEcole(), departement.getLibelle(), departement.getIdDepartement())) {
                throw new IllegalStateException("Le libellé '" + departement.getLibelle() + "' est déjà attribué.");
            }

            // Éviter qu'un département soit son propre parent
            if (departement.getIdDepartementParent() != null &&
                    departement.getIdDepartementParent().equals(departement.getIdDepartement())) {
                throw new IllegalStateException("Un département ne peut pas être son propre parent.");
            }

            if (!departementDAO.update(departement)) {
                throw new IllegalStateException("Échec de la mise à jour du département.");
            }
        }, onSucces, onErreur);
    }

    public void changerStatutAsync(String idDepartement, StatutDepartement nouveauStatut,
                                   String motifDesactivation,
                                   Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("grh.departement.modifier");

            Departement dept = departementDAO.findById(idDepartement)
                    .orElseThrow(() -> new IllegalStateException("Département introuvable."));

            dept.setStatut(nouveauStatut);
            dept.setMotifDesactivation(motifDesactivation);
            departementDAO.update(dept);
        }, onSucces, onErreur);
    }

    public void supprimerAsync(String idDepartement, Runnable onSucces, Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("grh.departement.supprimer");

            // Règle métier : Ne pas supprimer si des sous-départements sont actifs
            if (departementDAO.aDesEnfantsActifs(idDepartement)) {
                throw new IllegalStateException("Impossible de supprimer ce département car il contient des sous-départements rattachés.");
            }

            // Règle métier : Ne pas supprimer si du personnel y est rattaché
            int effectifRattache = departementDAO.compterPersonnelRattache(idDepartement);
            if (effectifRattache > 0) {
                throw new IllegalStateException("Impossible de supprimer ce département car " + effectifRattache + " employé(s) y sont rattaché(s).");
            }

            if (!departementDAO.softDelete(idDepartement)) {
                throw new IllegalStateException("Échec de la suppression du département.");
            }
        }, onSucces, onErreur);
    }

    // ==========================================
    // ACTIONS PAR LOT
    // ==========================================
    public void supprimerEnMasseAsync(Set<String> idsDepartements,
                                      Runnable onSucces,
                                      Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("grh.departement.supprimer");

            for (String id : idsDepartements) {
                if (departementDAO.aDesEnfantsActifs(id) || departementDAO.compterPersonnelRattache(id) > 0) {
                    continue; // Saute les départements avec contraintes
                }
                departementDAO.softDelete(id);
            }
        }, onSucces, onErreur);
    }

    // ==========================================
    // RÉAFFECTATION & RESPONSABLES
    // ==========================================
    public void reassignerResponsablesAsync(String idDepartement,
                                            String idResponsable,
                                            String idResponsableAdjoint,
                                            Runnable onSucces,
                                            Consumer<Throwable> onErreur) {
        executerSansRetour(() -> {
//            Autorisation.exigerAction("grh.departement.reassigner_responsables");

            boolean ok = departementDAO.reassignerResponsable(idDepartement, idResponsable, idResponsableAdjoint);
            if (!ok) {
                throw new IllegalStateException("Impossible de réaffecter les responsables du département.");
            }
        }, onSucces, onErreur);
    }
}