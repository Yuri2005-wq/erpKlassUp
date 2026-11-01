package org.erpklassup.erpklassup.controllers.actionPage.GRH;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.models.GRH.Departement;
import org.erpklassup.erpklassup.service.GRH.DepartementService;
import org.erpklassup.erpklassup.util.AlertUtil;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.Map;

import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DepartementHierarchieModalController implements Initializable {

    @FXML private TreeView<Departement> treeDepartements;
    @FXML private Button btnDeplierTout;
    @FXML private Button btnReplierTout;
    @FXML private Button btnFermer;

    private final DepartementService service = new DepartementService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerBoutons();
        configurerCellulesTree();
    }

    public void initData(String idEcole) {
        service.chargerTousParEcoleAsync(idEcole,
                departements -> Platform.runLater(() -> construireArbre(departements)),
                err -> Platform.runLater(() -> AlertUtil.afficherErreur("Erreur", err.getMessage()))
        );
    }

    private void construireArbre(List<Departement> liste) {
        if (liste == null || liste.isEmpty()) {
            treeDepartements.setRoot(null);
            return;
        }

        // Regroupe les départements par ID parent
        Map<String, List<Departement>> parParent = liste.stream()
                .collect(Collectors.groupingBy(d -> d.getIdDepartementParent() == null ? "RACINE" : d.getIdDepartementParent()));

        // Racine virtuelle pour l'organisation
        Departement racineVirtuelle = new Departement();
        racineVirtuelle.setLibelle("Établissement (Racine)");
        TreeItem<Departement> rootItem = new TreeItem<>(racineVirtuelle);
        rootItem.setExpanded(true);

        List<Departement> racines = parParent.get("RACINE");
        if (racines != null) {
            for (Departement dept : racines) {
                TreeItem<Departement> item = new TreeItem<>(dept);
                item.setExpanded(true);
                ajouterEnfants(item, dept.getIdDepartement(), parParent);
                rootItem.getChildren().add(item);
            }
        }

        treeDepartements.setRoot(rootItem);
        treeDepartements.setShowRoot(false); // Masque la racine virtuelle
    }

    private void ajouterEnfants(TreeItem<Departement> parentItem, String idParent, Map<String, List<Departement>> map) {
        List<Departement> enfants = map.get(idParent);
        if (enfants != null) {
            for (Departement enfant : enfants) {
                TreeItem<Departement> childItem = new TreeItem<>(enfant);
                childItem.setExpanded(true);
                ajouterEnfants(childItem, enfant.getIdDepartement(), map);
                parentItem.getChildren().add(childItem);
            }
        }
    }

    private void configurerCellulesTree() {
        treeDepartements.setCellFactory(tv -> new TreeCell<>() {
            private final FontIcon iconFolder = new FontIcon("fas-folder");
            private final Label lblNom = new Label();
            private final Label lblBadgeType = new Label();
            private final HBox conteneur = new HBox(8, iconFolder, lblNom, lblBadgeType);

            {
                conteneur.setAlignment(Pos.CENTER_LEFT);
                lblNom.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B;");
                lblBadgeType.getStyleClass().addAll("event-tag", "tag-blue");
                iconFolder.getStyleClass().addAll("icon-orange");
            }

            @Override
            protected void updateItem(Departement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    lblNom.setText(item.getLibelle());
                    if (item.getType() != null) {
                        lblBadgeType.setText(item.getType().getLibelle());
                        lblBadgeType.setVisible(true);
                    } else {
                        lblBadgeType.setVisible(false);
                    }
                    setGraphic(conteneur);
                }
            }
        });
    }

    private void configurerBoutons() {
        btnDeplierTout.setOnAction(e -> modifierExpansion(treeDepartements.getRoot(), true));
        btnReplierTout.setOnAction(e -> modifierExpansion(treeDepartements.getRoot(), false));
    }

    private void modifierExpansion(TreeItem<Departement> item, boolean expanded) {
        if (item != null) {
            item.setExpanded(expanded);
            for (TreeItem<Departement> child : item.getChildren()) {
                modifierExpansion(child, expanded);
            }
        }
    }

    @FXML
    private void handleFermer() {
        Stage stage = (Stage) btnFermer.getScene().getWindow();
        if (stage != null) stage.close();
    }
}