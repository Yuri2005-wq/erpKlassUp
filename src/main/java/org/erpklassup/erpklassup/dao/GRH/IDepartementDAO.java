package org.erpklassup.erpklassup.dao.GRH;

import org.erpklassup.erpklassup.dto.DepartementLigne;
import org.erpklassup.erpklassup.dto.FiltreDepartement;
import org.erpklassup.erpklassup.dto.ResultatPagine;
import org.erpklassup.erpklassup.models.GRH.Departement;

import java.util.List;
import java.util.Optional;

public interface IDepartementDAO {

    // CRUD Standard
    boolean create(Departement d);
    Optional<Departement> findById(String idDepartement);
    List<Departement> findByEcole(String idEcole);
    boolean update(Departement d);
    boolean softDelete(String idDepartement);

    // Recherche Paginée (Pour TableView)
    ResultatPagine<DepartementLigne> rechercherPagine(FiltreDepartement filtre);

    // Arborescence & Hiérarchie
    List<Departement> findRacines(String idEcole);
    List<Departement> findSousDepartements(String idEcole, String idParent);
    boolean aDesEnfantsActifs(String idDepartement);

    // Validation d'unicité
    boolean existeCode(String idEcole, String code, String idExclu);
    boolean existeLibelle(String idEcole, String libelle, String idExclu);

    // ============================================================
    // 💡 FONCTIONNALITÉS AVANCÉES
    // ============================================================

    /** Compte le nombre total de départements actifs pour le bandeau d'en-tête */
    long compterActifsParEcole(String idEcole);

    /** Réaffecte en bloc les responsables de départements (Pour la vue "Responsables") */
    boolean reassignerResponsable(String idDepartement, String idResponsable, String idResponsableAdjoint);

    /** Vérifie la présence de personnel rattaché avant désactivation/suppression */
    int compterPersonnelRattache(String idDepartement);
}