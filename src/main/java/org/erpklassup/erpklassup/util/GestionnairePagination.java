package org.erpklassup.erpklassup.util;

import javafx.collections.FXCollections;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableView;

import java.util.Collections;
import java.util.List;

/**
 * Classe utilitaire générique pour gérer la pagination côté client dans JavaFX.
 *
 * @param <T> Le type d'élément affiché dans la TableView
 */
public class GestionnairePagination<T> {

    private final TableView<T> tableView;
    private final Pagination pagination;
    private final int elementsParPage;
    private List<T> toutesLesDonnees = Collections.emptyList();

    public GestionnairePagination(TableView<T> tableView, Pagination pagination, int elementsParPage) {
        this.tableView = tableView;
        this.pagination = pagination;
        this.elementsParPage = elementsParPage > 0 ? elementsParPage : 20;

        configurerEcouteur();
    }

    private void configurerEcouteur() {
        this.pagination.currentPageIndexProperty().addListener((obs, anciennePage, nouvellePage) ->
                afficherPage(nouvellePage.intValue())
        );
    }

    /**
     * Reçoit la liste complète des éléments, calcule le nombre de pages
     * et réinitialise l'affichage sur la première page.
     */
    public void mettreAJourDonnees(List<T> nouvelleListe) {
        this.toutesLesDonnees = (nouvelleListe != null) ? nouvelleListe : Collections.emptyList();

        if (this.toutesLesDonnees.isEmpty()) {
            this.pagination.setPageCount(1);
            this.pagination.setCurrentPageIndex(0);
            this.tableView.setItems(FXCollections.observableArrayList());
            return;
        }

        int totalPages = (int) Math.ceil((double) this.toutesLesDonnees.size() / elementsParPage);
        this.pagination.setPageCount(Math.max(1, totalPages));
        this.pagination.setCurrentPageIndex(0);

        afficherPage(0);
    }

    /**
     * Extrait la sous-liste correspondant à l'index de page demandé
     * et met à jour les éléments de la TableView.
     */
    public void afficherPage(int pageIndex) {
        if (toutesLesDonnees.isEmpty()) {
            tableView.setItems(FXCollections.observableArrayList());
            return;
        }

        int indexDebut = pageIndex * elementsParPage;
        int indexFin = Math.min(indexDebut + elementsParPage, toutesLesDonnees.size());

        if (indexDebut < toutesLesDonnees.size()) {
            List<T> sousListe = toutesLesDonnees.subList(indexDebut, indexFin);
            tableView.setItems(FXCollections.observableArrayList(sousListe));
        } else {
            tableView.setItems(FXCollections.observableArrayList());
        }
    }

    public List<T> getToutesLesDonnees() {
        return toutesLesDonnees;
    }

    public int getElementsParPage() {
        return elementsParPage;
    }
}