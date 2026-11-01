package org.erpklassup.erpklassup.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Classe utilitaire générique pour gérer la pagination côté client dans JavaFX.
 * <p>
 * Contrairement à la version précédente, {@link #mettreAJourDonnees(List)} ne
 * remplace plus la liste du TableView et ne revient plus systématiquement à
 * la page 1 : elle fusionne les nouvelles données (ajouts/modifications/
 * suppressions ciblés) via {@link FusionListeUtil}, ce qui évite tout reset
 * visuel (scroll, page courante, sélection, tri, édition en cours) lors d'un
 * rafraîchissement automatique (ex : {@code AutoRefreshService}) quand rien
 * — ou peu — n'a réellement changé côté base.
 * <p>
 * Si aucun {@code extracteurId} n'est fourni (constructeur à 3 arguments),
 * le comportement retombe sur l'ancien mode "remplacement complet", pour
 * rester compatible avec d'éventuels autres usages de cette classe où les
 * éléments n'ont pas d'identifiant stable exploitable.
 *
 * @param <T> Le type d'élément affiché dans la TableView
 */
public class GestionnairePagination<T> {

    private final TableView<T> tableView;
    private final Pagination pagination;
    private final int elementsParPage;
    private final Function<T, String> extracteurId;

    /** Master-list complète (toutes pages confondues), jamais remplacée après construction. */
    private final ObservableList<T> toutesLesDonnees = FXCollections.observableArrayList();

    /** Liste réellement bindée au TableView : seule sa CONTENU change, jamais sa référence. */
    private final ObservableList<T> pageAffichee = FXCollections.observableArrayList();

    /** Évite que notre propre correction de page ne redéclenche l'écouteur de navigation. */
    private boolean navigationInterne = false;

    public GestionnairePagination(TableView<T> tableView, Pagination pagination, int elementsParPage) {
        this(tableView, pagination, elementsParPage, null);
    }

    public GestionnairePagination(TableView<T> tableView, Pagination pagination,
                                  int elementsParPage, Function<T, String> extracteurId) {
        this.tableView = tableView;
        this.pagination = pagination;
        this.elementsParPage = elementsParPage > 0 ? elementsParPage : 20;
        this.extracteurId = extracteurId;

        // La liste affichée n'est créée QU'UNE SEULE FOIS : on ne fera plus
        // jamais tableView.setItems(nouvelleListe) par la suite.
        this.tableView.setItems(pageAffichee);

        configurerEcouteur();
    }

    private void configurerEcouteur() {
        this.pagination.currentPageIndexProperty().addListener((obs, ancienne, nouvelle) -> {
            if (!navigationInterne) {
                afficherPage(nouvelle.intValue());
            }
        });
    }

    /**
     * Reçoit la liste complète des éléments et met à jour l'affichage.
     * <ul>
     *   <li>Fusionne la master-list (pas de remplacement complet)</li>
     *   <li>Recalcule le nombre de pages</li>
     *   <li>Conserve la page actuellement affichée, sauf si elle n'existe
     *       plus (ex : dernière ligne de la dernière page supprimée), auquel
     *       cas on recule d'une page au lieu de revenir en page 1</li>
     * </ul>
     */
    public void mettreAJourDonnees(List<T> nouvelleListe) {
        List<T> source = (nouvelleListe != null) ? nouvelleListe : Collections.emptyList();

        if (extracteurId != null) {
            FusionListeUtil.fusionner(toutesLesDonnees, source, extracteurId);
        } else {
            // Pas d'id exploitable : on garde l'ancien comportement (remplacement).
            toutesLesDonnees.setAll(source);
        }

        int totalPages = toutesLesDonnees.isEmpty()
                ? 1
                : (int) Math.ceil((double) toutesLesDonnees.size() / elementsParPage);
        totalPages = Math.max(1, totalPages);

        int pageActuelle = pagination.getCurrentPageIndex();
        int pageCorrigee = Math.max(0, Math.min(pageActuelle, totalPages - 1));

        if (pagination.getPageCount() != totalPages) {
            pagination.setPageCount(totalPages);
        }
        if (pagination.getCurrentPageIndex() != pageCorrigee) {
            navigationInterne = true;
            pagination.setCurrentPageIndex(pageCorrigee);
            navigationInterne = false;
        }

        afficherPage(pageCorrigee);
    }

    /**
     * Met à jour le contenu de la page demandée par fusion différentielle
     * (jamais de {@code setItems} complet) : préserve scroll, sélection et tri.
     */
    public void afficherPage(int pageIndex) {
        List<T> sousListe = extraireSousListe(pageIndex);

        if (extracteurId != null) {
            FusionListeUtil.fusionner(pageAffichee, sousListe, extracteurId);
        } else {
            pageAffichee.setAll(sousListe);
        }
    }

    private List<T> extraireSousListe(int pageIndex) {
        if (toutesLesDonnees.isEmpty()) {
            return Collections.emptyList();
        }
        int indexDebut = pageIndex * elementsParPage;
        if (indexDebut >= toutesLesDonnees.size()) {
            return Collections.emptyList();
        }
        int indexFin = Math.min(indexDebut + elementsParPage, toutesLesDonnees.size());
        // Copie défensive : subList() est une vue vivante sur toutesLesDonnees,
        // on ne veut pas que FusionListeUtil mute directement la master-list.
        return new ArrayList<>(toutesLesDonnees.subList(indexDebut, indexFin));
    }

    public List<T> getToutesLesDonnees() {
        return Collections.unmodifiableList(toutesLesDonnees);
    }

    public int getElementsParPage() {
        return elementsParPage;
    }
}