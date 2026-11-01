package org.erpklassup.erpklassup.controllers.actionPage.GRH;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.erpklassup.erpklassup.dto.FiltrePersonnel;
import org.erpklassup.erpklassup.dto.PersonnelLigne;
import org.erpklassup.erpklassup.enums.StatutEmploi;
import org.erpklassup.erpklassup.models.GRH.Departement;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.GRH.DepartementService;
import org.erpklassup.erpklassup.service.GRH.PersonnelService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class PersonnelController implements Initializable, VueDisposable {

    /* ============================================================
       CHAMPS FXML — BARRE D'OUTILS & FILTRES
       ============================================================ */
    @FXML private StackPane rootStack;
    @FXML private TextField searchPersonnelField;
    @FXML private Button clearSearchPersonnel;
    @FXML private ComboBox<Departement> filtreDepartementCombo;
    @FXML private ComboBox<StatutEmploi> filtreStatutCombo;
    @FXML private ToggleButton btnVueCartes;
    @FXML private ToggleButton btnVueTable;
    @FXML private Button btnNouveauPersonnel;

    /* ============================================================
       CHAMPS FXML — STATISTIQUES
       ============================================================ */
    @FXML private Label statTotalPersonnel;
    @FXML private Label statActifs;
    @FXML private Label statEnConge;
    @FXML private Label statInactifs;

    /* ============================================================
       CHAMPS FXML — VUES
       ============================================================ */
    @FXML private ScrollPane scrollCartes;
    @FXML private FlowPane cartesPane;

    @FXML private VBox containerTable;
    @FXML private TableView<PersonnelLigne> tablePersonnel;
    @FXML private TableColumn<PersonnelLigne, PersonnelLigne> colEmploye;
    @FXML private TableColumn<PersonnelLigne, String> colMatricule;
    @FXML private TableColumn<PersonnelLigne, String> colPoste;
    @FXML private TableColumn<PersonnelLigne, String> colDepartement;
    @FXML private TableColumn<PersonnelLigne, PersonnelLigne> colContact;
    @FXML private TableColumn<PersonnelLigne, PersonnelLigne> colStatut;
    @FXML private TableColumn<PersonnelLigne, PersonnelLigne> colActions;
    @FXML private Label footerInfoPersonnel;
    @FXML private Pagination pagination;

    /* ============================================================
       SERVICES & ÉTAT
       ============================================================ */
    private static final int TAILLE_PAGE = FiltrePersonnel.TAILLE_PAGE_DEFAUT;
    private static final Duration PERIODE_REFRESH = Duration.seconds(15);

    private final PersonnelService personnelService = new PersonnelService();
    private final DepartementService departementService = new DepartementService();
    private final AuditService auditService = new AuditService();

    private final PauseTransition debounceRecherche = new PauseTransition(Duration.millis(350));
    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;

    private String idEcoleCourante;
    private GestionnairePagination<PersonnelLigne> gestionnairePagination;
    private AutoRefreshService<List<PersonnelLigne>> autoRefresh;

    /* ============================================================
       INITIALISATION
       ============================================================ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tablePersonnel.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();

        gestionnairePagination = new GestionnairePagination<>(
                tablePersonnel, pagination, TAILLE_PAGE,
                PersonnelLigne::idPersonnel
        );

        configurerFiltres();
        configurerTable();
        configurerRecherche();
        configurerToggleVue();

        btnNouveauPersonnel.setOnAction(e -> ouvrirModalCreation());

        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
        appliquerControlesAcces();

        if (idEcoleCourante == null) {
            afficherPlaceholder("Aucune école sélectionnée");
            return;
        }
        RechercheUtil.configurer(searchPersonnelField, clearSearchPersonnel, this::lancerRecherche);

        chargerDepartements();
        lancerRecherche();
        demarrerRafraichissementAuto();
    }

    @Override
    public void disposer() {
        if (autoRefresh != null) {
            autoRefresh.dispose();
            autoRefresh = null;
        }
        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
    }

    /* ============================================================
       TOGGLE ENTRE VUE KANBAN ET VUE TABLEAU
       ============================================================ */
    private void configurerToggleVue() {
        btnVueCartes.selectedProperty().addListener((o, a, isCartes) -> {
            if (Boolean.TRUE.equals(isCartes)) {
                scrollCartes.setVisible(true);
                scrollCartes.setManaged(true);
                containerTable.setVisible(false);
                containerTable.setManaged(false);
            }
        });

        btnVueTable.selectedProperty().addListener((o, a, isTable) -> {
            if (Boolean.TRUE.equals(isTable)) {
                scrollCartes.setVisible(false);
                scrollCartes.setManaged(false);
                containerTable.setVisible(true);
                containerTable.setManaged(true);
            }
        });
    }

    /* ============================================================
       CONFIGURATION DES FILTRES & RECHERCHE
       ============================================================ */
    private void configurerFiltres() {
        ObservableList<StatutEmploi> statuts = FXCollections.observableArrayList();
        statuts.add(null);
        statuts.addAll(StatutEmploi.values());
        filtreStatutCombo.setItems(statuts);

        filtreStatutCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(StatutEmploi item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Tous les statuts" : item.name());
            }
        });
        filtreStatutCombo.setButtonCell(filtreStatutCombo.getCellFactory().call(null));
        filtreStatutCombo.getSelectionModel().selectFirst();

        filtreDepartementCombo.valueProperty().addListener((o, a, n) -> lancerRecherche());
        filtreStatutCombo.valueProperty().addListener((o, a, n) -> lancerRecherche());
    }

    private void chargerDepartements() {
        departementService.chargerTousParEcoleAsync(idEcoleCourante,
                depts -> Platform.runLater(() -> {
                    ObservableList<Departement> items = FXCollections.observableArrayList();
                    items.add(null);
                    if (depts != null) items.addAll(depts);

                    filtreDepartementCombo.setCellFactory(lv -> new ListCell<>() {
                        @Override
                        protected void updateItem(Departement item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? "Tous les départements" : item.getLibelle());
                        }
                    });
                    filtreDepartementCombo.setButtonCell(filtreDepartementCombo.getCellFactory().call(null));
                    filtreDepartementCombo.setItems(items);
                    filtreDepartementCombo.getSelectionModel().selectFirst();
                }),
                err -> System.err.println("❌ Erreur chargement départements : " + err.getMessage())
        );
    }

    private void configurerRecherche() {
        debounceRecherche.setOnFinished(e -> lancerRecherche());
        searchPersonnelField.textProperty().addListener((o, a, n) -> {
            if (clearSearchPersonnel != null) {
                clearSearchPersonnel.setVisible(n != null && !n.isBlank());
            }
            debounceRecherche.playFromStart();
        });

        if (clearSearchPersonnel != null) {
            clearSearchPersonnel.setOnAction(e -> searchPersonnelField.clear());
        }
    }

    /* ============================================================
       CONFIGURATION DE LA TABLE (AVEC ACTIONS DIRECTES : ŒIL, EDIT, SUPPRESSION)
       ============================================================ */
    private void configurerTable() {
        tablePersonnel.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colEmploye.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colEmploye.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(PersonnelLigne p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                    return;
                }

                HBox box = new HBox(10);
                box.setAlignment(Pos.CENTER_LEFT);
                Node avatar = AvatarUtil.creer(p.photoPath(), p.nom(), p.prenom(), p.idPersonnel(), p.statutEmploi());

                VBox texte = new VBox(2);
                Label lblNom = new Label(p.nomComplet());
                lblNom.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B;");
                Label lblEmail = new Label(p.email() != null ? p.email() : "—");
                lblEmail.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

                texte.getChildren().addAll(lblNom, lblEmail);
                box.getChildren().addAll(avatar, texte);
                setGraphic(box);
            }
        });

        colMatricule.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().matricule() != null ? d.getValue().matricule() : "—"));
        colPoste.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().poste() != null ? d.getValue().poste() : "—"));
        colDepartement.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().nomDepartement() != null ? d.getValue().nomDepartement() : "—"));

        colContact.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colContact.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(PersonnelLigne p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setGraphic(null); return; }
                setGraphic(new Label(p.telephone() != null ? p.telephone() : "—"));
            }
        });

        colStatut.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(PersonnelLigne p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null || p.statutEmploi() == null) {
                    setGraphic(null);
                    return;
                }

                String st = p.statutEmploi();
                Label badge = new Label(st);
                badge.getStyleClass().addAll("event-tag", switch (st) {
                    case "actif" -> "tag-green";
                    case "suspendu" -> "tag-orange";
                    default -> "tag-red";
                });
                setGraphic(badge);
            }
        });

        // Colonne Actions avec 3 boutons directs : Œil, Édition, Suppression
        colActions.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnVoir = new Button();
            private final Button btnEdit = new Button();
            private final Button btnDel = new Button();
            private final HBox box = new HBox(8, btnVoir, btnEdit, btnDel);

            {
                box.setAlignment(Pos.CENTER);

                // Icône Œil (Détails)
                FontIcon iconView = new FontIcon("fth-eye");
                iconView.setIconSize(14);
                iconView.setIconColor(Color.web("#0284C7"));
                btnVoir.setGraphic(iconView);
                btnVoir.getStyleClass().add("btn-icon-light");
                btnVoir.setTooltip(new Tooltip("Voir les détails"));
                btnVoir.setOnAction(e -> {
                    PersonnelLigne p = getItem();
                    if (p != null) ouvrirModalDetails(p);
                });

                // Icône Édition
                FontIcon iconEdit = new FontIcon("fth-edit-2");
                iconEdit.setIconSize(14);
                iconEdit.setIconColor(Color.web("#D97706"));
                btnEdit.setGraphic(iconEdit);
                btnEdit.getStyleClass().add("btn-icon-light");
                btnEdit.setTooltip(new Tooltip("Modifier"));
                btnEdit.setOnAction(e -> {
                    PersonnelLigne p = getItem();
                    if (p != null) ouvrirModalEdition(p);
                });

                // Icône Suppression
                FontIcon iconDel = new FontIcon("fth-trash-2");
                iconDel.setIconSize(14);
                iconDel.getStyleClass().add("icon-red");
                btnDel.setGraphic(iconDel);
                btnDel.getStyleClass().add("btn-icon-light");
                btnDel.setTooltip(new Tooltip("Supprimer"));
                btnDel.setOnAction(e -> {
                    PersonnelLigne p = getItem();
                    if (p != null) supprimerPersonnel(p);
                });
            }

            @Override
            protected void updateItem(PersonnelLigne p, boolean empty) {
                super.updateItem(p, empty);
                setGraphic((empty || p == null) ? null : box);
            }
        });
    }

    /* ============================================================
       RECHERCHE & AUTO-REFRESH
       ============================================================ */
    private void demarrerRafraichissementAuto() {
        autoRefresh = new AutoRefreshService<List<PersonnelLigne>>(PERIODE_REFRESH)
                .loader(this::chargerPersonnelPourRefresh)
                .onSuccess(this::appliquerResultatRefresh)
                .onFailure(err -> System.err.println("❌ Erreur auto-refresh personnel : " + err.getMessage()))
                .start();
    }

    private List<PersonnelLigne> chargerPersonnelPourRefresh() {
        if (idEcoleCourante == null) return List.of();

        Departement dept = filtreDepartementCombo.getSelectionModel().getSelectedItem();
        StatutEmploi statut = filtreStatutCombo.getSelectionModel().getSelectedItem();
        String idDept = (dept != null) ? dept.getIdDepartement() : null;

        FiltrePersonnel filtre = new FiltrePersonnel(
                idEcoleCourante,
                idDept,
                null,
                statut,
                searchPersonnelField.getText(),
                1,
                Integer.MAX_VALUE
        );

        var res = personnelService.rechercher(filtre);
        return res != null ? res.elements() : List.of();
    }

    private void lancerRecherche() {
        if (idEcoleCourante == null) return;

        Departement dept = filtreDepartementCombo.getSelectionModel().getSelectedItem();
        StatutEmploi statut = filtreStatutCombo.getSelectionModel().getSelectedItem();
        String idDept = (dept != null) ? dept.getIdDepartement() : null;

        FiltrePersonnel filtre = new FiltrePersonnel(
                idEcoleCourante,
                idDept,
                null,
                statut,
                searchPersonnelField.getText(),
                1,
                TAILLE_PAGE
        );

        tablePersonnel.setPlaceholder(new Label("Chargement du personnel..."));

        personnelService.rechercherAsync(filtre,
                resultat -> Platform.runLater(() -> {
                    gestionnairePagination.mettreAJourDonnees(resultat.elements());
                    tablePersonnel.setPlaceholder(new Label("Aucun employé trouvé."));
                    footerInfoPersonnel.setText(resultat.total() + " employé(s)");

                    afficherCartesPersonnel(resultat.elements());
                    mettreAJourStatistiques(resultat.elements(), resultat.total());

                    if (autoRefresh != null && autoRefresh.isRunning()) {
                        autoRefresh.restartNow();
                    }
                }),
                err -> Platform.runLater(() -> {
                    tablePersonnel.setPlaceholder(new Label("Erreur de chargement."));
                    AlertUtil.afficherErreur("Erreur", err.getMessage());
                })
        );
    }

    private void appliquerResultatRefresh(List<PersonnelLigne> resultat) {
        if (resultat == null) return;
        ObservableList<PersonnelLigne> listeCourante = tablePersonnel.getItems();
        FusionListeUtil.fusionner(listeCourante, resultat, PersonnelLigne::idPersonnel);
        gestionnairePagination.mettreAJourDonnees(listeCourante);
        afficherCartesPersonnel(resultat);
    }

    /* ============================================================
       GÉNÉRATION DES CARTES KANBAN (FIDÈLE À LA MAQUETTE)
       ============================================================ */
    private void afficherCartesPersonnel(List<PersonnelLigne> personnels) {
        if (cartesPane == null) return;
        cartesPane.getChildren().clear();

        if (personnels == null || personnels.isEmpty()) {
            afficherPlaceholder("Aucun employé enregistré");
            return;
        }

        for (PersonnelLigne p : personnels) {
            cartesPane.getChildren().add(creerCartePersonnelMaquette(p));
        }
    }

    private Node creerCartePersonnelMaquette(PersonnelLigne p) {
        VBox carte = new VBox(12);
        carte.setPrefWidth(275);
        carte.setMinWidth(275);
        carte.setMaxWidth(275);
        carte.setStyle("""
            -fx-background-color: white; 
            -fx-background-radius: 14; 
            -fx-padding: 16; 
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 10, 0, 0, 4);
        """);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Node photoAvatar = AvatarUtil.creer(p.photoPath(), p.nom(), p.prenom(), p.idPersonnel(), 60, p.statutEmploi());

        VBox identityBox = new VBox(2);
        HBox.setHgrow(identityBox, Priority.ALWAYS);

        Label lblNom = new Label(p.nomComplet());
        lblNom.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1E293B;");

        Label lblPoste = new Label(p.poste() != null ? p.poste() : "Employé");
        lblPoste.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11.5px;");

        identityBox.getChildren().addAll(lblNom, lblPoste);

        Button btnMore = new Button();
        btnMore.setGraphic(new FontIcon("fth-more-horizontal"));
        btnMore.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #64748B;");
        btnMore.setOnAction(e -> ouvrirMenuActionsLigne(btnMore, p));

        header.getChildren().addAll(photoAvatar, identityBox, btnMore);

        VBox detailsContainer = new VBox(10);
        detailsContainer.setStyle("""
            -fx-background-color: #F8FAFC; 
            -fx-background-radius: 10; 
            -fx-padding: 12;
        """);

        GridPane gridInfo = new GridPane();
        gridInfo.setHgap(30);
        gridInfo.setVgap(2);

        VBox deptBox = new VBox(2);
        Label lblDeptTitle = new Label("Department");
        lblDeptTitle.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px;");
        Label lblDeptVal = new Label(p.nomDepartement() != null ? p.nomDepartement() : "—");
        lblDeptVal.setStyle("-fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 12px;");
        deptBox.getChildren().addAll(lblDeptTitle, lblDeptVal);

        VBox dateBox = new VBox(2);
        Label lblDateTitle = new Label("Hired Date");
        lblDateTitle.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px;");
        Label lblDateVal = new Label("—");
        lblDateVal.setStyle("-fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 12px;");
        dateBox.getChildren().addAll(lblDateTitle, lblDateVal);

        gridInfo.add(deptBox, 0, 0);
        gridInfo.add(dateBox, 1, 0);

        HBox matriculeRow = new HBox(6, createIcon("fth-hash"), new Label("Matricule : " + (p.matricule() != null ? p.matricule() : "—")));
        matriculeRow.setAlignment(Pos.CENTER_LEFT);
        ((Label) matriculeRow.getChildren().get(1)).setStyle("-fx-text-fill: #475569; -fx-font-size: 11.5px; -fx-font-weight: bold;");

        HBox mailRow = new HBox(6, createIcon("fth-mail"), new Label(p.email() != null ? p.email() : "—"));
        mailRow.setAlignment(Pos.CENTER_LEFT);
        ((Label) mailRow.getChildren().get(1)).setStyle("-fx-text-fill: #475569; -fx-font-size: 11.5px;");

        HBox phoneRow = new HBox(6, createIcon("fth-phone"), new Label(p.telephone() != null ? p.telephone() : "—"));
        phoneRow.setAlignment(Pos.CENTER_LEFT);
        ((Label) phoneRow.getChildren().get(1)).setStyle("-fx-text-fill: #475569; -fx-font-size: 11.5px;");

        detailsContainer.getChildren().addAll(gridInfo, new Separator(), matriculeRow, mailRow, phoneRow);

        carte.getChildren().addAll(header, detailsContainer);
        return carte;
    }

    private FontIcon createIcon(String literal) {
        FontIcon ic = new FontIcon(literal);
        ic.setIconSize(12);
        ic.setIconColor(Color.web("#64748B"));
        return ic;
    }

    /* ============================================================
       MENU CONTEXTUEL (POUR LES CARTES KANBAN)
       ============================================================ */
    private void ouvrirMenuActionsLigne(Button btnSource, PersonnelLigne p) {
        ContextMenu menu = new ContextMenu();

        MenuItem view = new MenuItem("Détails");
        view.setGraphic(new FontIcon("fth-eye"));
        view.setOnAction(e -> ouvrirModalDetails(p));
        menu.getItems().add(view);

        MenuItem edit = new MenuItem("Modifier");
        edit.setGraphic(new FontIcon("fth-edit-2"));
        edit.setOnAction(e -> ouvrirModalEdition(p));
        menu.getItems().add(edit);

        menu.getItems().add(new SeparatorMenuItem());

        MenuItem del = new MenuItem("Supprimer");
        FontIcon icDel = new FontIcon("fth-trash-2");
        icDel.getStyleClass().add("icon-red");
        del.setGraphic(icDel);
        del.setStyle("-fx-text-fill: #EF4444;");
        del.setOnAction(e -> supprimerPersonnel(p));
        menu.getItems().add(del);

        menu.show(btnSource, Side.BOTTOM, 0, 4);
    }

    /* ============================================================
       STATISTIQUES & HELPERS
       ============================================================ */
    private void mettreAJourStatistiques(List<PersonnelLigne> liste, long totalElements) {
        if (liste == null) return;
        long actifs = liste.stream().filter(PersonnelLigne::estActif).count();
        long inactifs = totalElements - actifs;

        statTotalPersonnel.setText(String.valueOf(totalElements));
        statActifs.setText(String.valueOf(actifs));
        statInactifs.setText(String.valueOf(inactifs));
        statEnConge.setText("0");
    }

    private void afficherPlaceholder(String msg) {
        cartesPane.getChildren().clear();
        VBox empty = new VBox(12);
        empty.setAlignment(Pos.CENTER);
        empty.getChildren().addAll(new FontIcon("fth-users"), new Label(msg));
        cartesPane.getChildren().add(empty);
    }

    private void appliquerControlesAcces() {
        // PERMISSIONS COMMENTÉES TEMPORAIREMENT POUR TESTS LIBRES
        // ControleAcces.appliquerAction(btnNouveauPersonnel, "personnel.creer");
    }

    private void ouvrirModalCreation() {
        ToastNotification.info((Stage) rootStack.getScene().getWindow(), "Ouverture du formulaire de création d'employé.");
    }

    private void ouvrirModalDetails(PersonnelLigne p) {
        ToastNotification.info((Stage) rootStack.getScene().getWindow(), "Consultation des détails de : " + p.nomComplet());
    }

    private void ouvrirModalEdition(PersonnelLigne p) {
        ToastNotification.info((Stage) rootStack.getScene().getWindow(), "Édition de l'employé : " + p.nomComplet());
    }

    private void supprimerPersonnel(PersonnelLigne p) {
        boolean ok = AlertUtil.afficherConfirmation("Suppression",
                "Êtes-vous sûr de vouloir supprimer l'employé " + p.nomComplet() + " ?",
                rootStack.getScene().getWindow());

        if (ok) {
            personnelService.supprimerAsync(p.idPersonnel(),
                    () -> Platform.runLater(() -> {
                        auditService.tracerActionAsync("GRH", "SUPPRESSION_PERSONNEL", "Personnel supprimé : " + p.nomComplet());
                        ToastNotification.succes((Stage) rootStack.getScene().getWindow(), "Employé supprimé.");
                        lancerRecherche();
                    }),
                    err -> Platform.runLater(() -> ToastNotification.erreur((Stage) rootStack.getScene().getWindow(), err.getMessage()))
            );
        }
    }
}
//package org.erpklassup.erpklassup.controllers.actionPage.GRH;
//
//import javafx.animation.PauseTransition;
//import javafx.application.Platform;
//import javafx.beans.property.SimpleObjectProperty;
//import javafx.beans.property.SimpleStringProperty;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.geometry.Pos;
//import javafx.geometry.Side;
//import javafx.scene.Node;
//import javafx.scene.control.*;
//import javafx.scene.layout.*;
//import javafx.scene.paint.Color;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//
//import org.erpklassup.erpklassup.dto.FiltrePersonnel;
//import org.erpklassup.erpklassup.dto.PersonnelLigne;
//import org.erpklassup.erpklassup.enums.StatutEmploi;
//import org.erpklassup.erpklassup.models.GRH.Departement;
//import org.erpklassup.erpklassup.service.AuditService;
//import org.erpklassup.erpklassup.service.GRH.DepartementService;
//import org.erpklassup.erpklassup.service.GRH.PersonnelService;
//import org.erpklassup.erpklassup.service.SessionManager;
//import org.erpklassup.erpklassup.util.*;
//import org.kordamp.ikonli.javafx.FontIcon;
//
//import java.net.URL;
//import java.util.List;
//import java.util.ResourceBundle;
//
//public class PersonnelController implements Initializable, VueDisposable {
//
//    /* ============================================================
//       CHAMPS FXML — BARRE D'OUTILS & FILTRES
//       ============================================================ */
//    @FXML private StackPane rootStack;
//    @FXML private TextField searchPersonnelField;
//    @FXML private Button clearSearchPersonnel;
//    @FXML private ComboBox<Departement> filtreDepartementCombo;
//    @FXML private ComboBox<StatutEmploi> filtreStatutCombo;
//    @FXML private ToggleButton btnVueCartes;
//    @FXML private ToggleButton btnVueTable;
//    @FXML private Button btnNouveauPersonnel;
//
//    /* ============================================================
//       CHAMPS FXML — STATISTIQUES
//       ============================================================ */
//    @FXML private Label statTotalPersonnel;
//    @FXML private Label statActifs;
//    @FXML private Label statEnConge;
//    @FXML private Label statInactifs;
//
//    /* ============================================================
//       CHAMPS FXML — VUES
//       ============================================================ */
//    @FXML private ScrollPane scrollCartes;
//    @FXML private FlowPane cartesPane;
//
//    @FXML private VBox containerTable;
//    @FXML private TableView<PersonnelLigne> tablePersonnel;
//    @FXML private TableColumn<PersonnelLigne, PersonnelLigne> colEmploye;
//    @FXML private TableColumn<PersonnelLigne, String> colMatricule;
//    @FXML private TableColumn<PersonnelLigne, String> colPoste;
//    @FXML private TableColumn<PersonnelLigne, String> colDepartement;
//    @FXML private TableColumn<PersonnelLigne, PersonnelLigne> colContact;
//    @FXML private TableColumn<PersonnelLigne, PersonnelLigne> colStatut;
//    @FXML private TableColumn<PersonnelLigne, PersonnelLigne> colActions;
//    @FXML private Label footerInfoPersonnel;
//    @FXML private Pagination pagination;
//
//    /* ============================================================
//       SERVICES & ÉTAT
//       ============================================================ */
//    private static final int TAILLE_PAGE = FiltrePersonnel.TAILLE_PAGE_DEFAUT;
//    private static final Duration PERIODE_REFRESH = Duration.seconds(15);
//
//    private final PersonnelService personnelService = new PersonnelService();
//    private final DepartementService departementService = new DepartementService();
//    private final AuditService auditService = new AuditService();
//
//    private final PauseTransition debounceRecherche = new PauseTransition(Duration.millis(350));
//    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;
//
//    private String idEcoleCourante;
//    private GestionnairePagination<PersonnelLigne> gestionnairePagination;
//    private AutoRefreshService<List<PersonnelLigne>> autoRefresh;
//
//    /* ============================================================
//       INITIALISATION
//       ============================================================ */
//    @Override
//    public void initialize(URL url, ResourceBundle rb) {
//        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();
//
//        gestionnairePagination = new GestionnairePagination<>(
//                tablePersonnel, pagination, TAILLE_PAGE,
//                PersonnelLigne::idPersonnel
//        );
//
//        configurerFiltres();
//        configurerTable();
//        configurerRecherche();
//        configurerToggleVue();
//
//        btnNouveauPersonnel.setOnAction(e -> ouvrirModalCreation());
//
//        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
//        appliquerControlesAcces();
//
//        if (idEcoleCourante == null) {
//            afficherPlaceholder("Aucune école sélectionnée");
//            return;
//        }
//
//        chargerDepartements();
//        lancerRecherche();
//        demarrerRafraichissementAuto();
//    }
//
//    @Override
//    public void disposer() {
//        if (autoRefresh != null) {
//            autoRefresh.dispose();
//            autoRefresh = null;
//        }
//        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
//    }
//
//    /* ============================================================
//       TOGGLE ENTRE VUE KANBAN ET VUE TABLEAU
//       ============================================================ */
//    private void configurerToggleVue() {
//        btnVueCartes.selectedProperty().addListener((o, a, isCartes) -> {
//            if (Boolean.TRUE.equals(isCartes)) {
//                scrollCartes.setVisible(true);
//                scrollCartes.setManaged(true);
//                containerTable.setVisible(false);
//                containerTable.setManaged(false);
//            }
//        });
//
//        btnVueTable.selectedProperty().addListener((o, a, isTable) -> {
//            if (Boolean.TRUE.equals(isTable)) {
//                scrollCartes.setVisible(false);
//                scrollCartes.setManaged(false);
//                containerTable.setVisible(true);
//                containerTable.setManaged(true);
//            }
//        });
//    }
//
//    /* ============================================================
//       CONFIGURATION DES FILTRES & RECHERCHE
//       ============================================================ */
//    private void configurerFiltres() {
//        ObservableList<StatutEmploi> statuts = FXCollections.observableArrayList();
//        statuts.add(null); // Tous
//        statuts.addAll(StatutEmploi.values());
//        filtreStatutCombo.setItems(statuts);
//
//        filtreStatutCombo.setCellFactory(lv -> new ListCell<>() {
//            @Override
//            protected void updateItem(StatutEmploi item, boolean empty) {
//                super.updateItem(item, empty);
//                setText(empty || item == null ? "Tous les statuts" : item.name());
//            }
//        });
//        filtreStatutCombo.setButtonCell(filtreStatutCombo.getCellFactory().call(null));
//        filtreStatutCombo.getSelectionModel().selectFirst();
//
//        filtreDepartementCombo.valueProperty().addListener((o, a, n) -> lancerRecherche());
//        filtreStatutCombo.valueProperty().addListener((o, a, n) -> lancerRecherche());
//    }
//
//    private void chargerDepartements() {
//        departementService.chargerTousParEcoleAsync(idEcoleCourante,
//                depts -> Platform.runLater(() -> {
//                    ObservableList<Departement> items = FXCollections.observableArrayList();
//                    items.add(null); // Option "Tous les départements"
//                    if (depts != null) items.addAll(depts);
//
//                    filtreDepartementCombo.setCellFactory(lv -> new ListCell<>() {
//                        @Override
//                        protected void updateItem(Departement item, boolean empty) {
//                            super.updateItem(item, empty);
//                            setText(empty || item == null ? "Tous les départements" : item.getLibelle());
//                        }
//                    });
//                    filtreDepartementCombo.setButtonCell(filtreDepartementCombo.getCellFactory().call(null));
//                    filtreDepartementCombo.setItems(items);
//                    filtreDepartementCombo.getSelectionModel().selectFirst();
//                }),
//                err -> System.err.println("❌ Erreur chargement départements : " + err.getMessage())
//        );
//    }
//
//    private void configurerRecherche() {
//        debounceRecherche.setOnFinished(e -> lancerRecherche());
//        searchPersonnelField.textProperty().addListener((o, a, n) -> {
//            if (clearSearchPersonnel != null) {
//                clearSearchPersonnel.setVisible(n != null && !n.isBlank());
//            }
//            debounceRecherche.playFromStart();
//        });
//
//        if (clearSearchPersonnel != null) {
//            clearSearchPersonnel.setOnAction(e -> searchPersonnelField.clear());
//        }
//    }
//

//    /* ============================================================
//       CONFIGURATION DE LA TABLE
//       ============================================================ */
//    private void configurerTable() {
//        tablePersonnel.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
//
//        // Employé (Avatar + Nom)
//        colEmploye.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
//        colEmploye.setCellFactory(col -> new TableCell<>() {
//            @Override
//            protected void updateItem(PersonnelLigne p, boolean empty) {
//                super.updateItem(p, empty);
//                if (empty || p == null) {
//                    setGraphic(null);
//                    return;
//                }
//
//                HBox box = new HBox(10);
//                box.setAlignment(Pos.CENTER_LEFT);
//                Node avatar = AvatarUtil.creer(p.photoPath(), p.nom(), p.prenom(), p.idPersonnel());
//
//                VBox texte = new VBox(2);
//                Label lblNom = new Label(p.nomComplet());
//                lblNom.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B;");
//                Label lblEmail = new Label(p.email() != null ? p.email() : "—");
//                lblEmail.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
//
//                texte.getChildren().addAll(lblNom, lblEmail);
//                box.getChildren().addAll(avatar, texte);
//                setGraphic(box);
//            }
//        });
//
//        colMatricule.setCellValueFactory(d -> new SimpleStringProperty(
//                d.getValue().matricule() != null ? d.getValue().matricule() : "—"));
//        colPoste.setCellValueFactory(d -> new SimpleStringProperty(
//                d.getValue().poste() != null ? d.getValue().poste() : "—"));
//        colDepartement.setCellValueFactory(d -> new SimpleStringProperty(
//                d.getValue().nomDepartement() != null ? d.getValue().nomDepartement() : "—"));
//
//        // Contact
//        colContact.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
//        colContact.setCellFactory(col -> new TableCell<>() {
//            @Override
//            protected void updateItem(PersonnelLigne p, boolean empty) {
//                super.updateItem(p, empty);
//                if (empty || p == null) { setGraphic(null); return; }
//                setGraphic(new Label(p.telephone() != null ? p.telephone() : "—"));
//            }
//        });
//
//        // Statut d'emploi
//        colStatut.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
//        colStatut.setCellFactory(col -> new TableCell<>() {
//            @Override
//            protected void updateItem(PersonnelLigne p, boolean empty) {
//                super.updateItem(p, empty);
//                if (empty || p == null || p.statutEmploi() == null) {
//                    setGraphic(null);
//                    return;
//                }
//
//                String st = p.statutEmploi();
//                Label badge = new Label("● " + st);
//                badge.getStyleClass().addAll("event-tag", switch (st) {
//                    case "ACTIF" -> "tag-green";
//                    case "SUSPENDU" -> "tag-orange";
//                    default -> "tag-red";
//                });
//                setGraphic(badge);
//            }
//        });
//
//        // Actions
//        colActions.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
//        colActions.setCellFactory(col -> new TableCell<>() {
//            private final Button btnMenu = new Button();
//            private final HBox box = new HBox(btnMenu);
//
//            {
//                box.setAlignment(Pos.CENTER);
//                FontIcon iconMore = new FontIcon("fth-more-vertical");
//                iconMore.setIconSize(16);
//                btnMenu.setGraphic(iconMore);
//                btnMenu.getStyleClass().add("btn-icon-light");
//
//                btnMenu.setOnAction(e -> {
//                    PersonnelLigne p = getItem();
//                    if (p != null) ouvrirMenuActionsLigne(btnMenu, p);
//                });
//            }
//
//            @Override
//            protected void updateItem(PersonnelLigne p, boolean empty) {
//                super.updateItem(p, empty);
//                setGraphic((empty || p == null) ? null : box);
//            }
//        });
//    }
//
//    /* ============================================================
//       RECHERCHE & AUTO-REFRESH
//       ============================================================ */
//    private void demarrerRafraichissementAuto() {
//        autoRefresh = new AutoRefreshService<List<PersonnelLigne>>(PERIODE_REFRESH)
//                .loader(this::chargerPersonnelPourRefresh)
//                .onSuccess(this::appliquerResultatRefresh)
//                .onFailure(err -> System.err.println("❌ Erreur auto-refresh personnel : " + err.getMessage()))
//                .start();
//    }
//
//    private List<PersonnelLigne> chargerPersonnelPourRefresh() {
//        if (idEcoleCourante == null) return List.of();
//
//        Departement dept = filtreDepartementCombo.getSelectionModel().getSelectedItem();
//        StatutEmploi statut = filtreStatutCombo.getSelectionModel().getSelectedItem();
//        String idDept = (dept != null) ? dept.getIdDepartement() : null;
//
//        FiltrePersonnel filtre = new FiltrePersonnel(
//                idEcoleCourante,
//                idDept,
//                null,
//                statut,
//                searchPersonnelField.getText(),
//                1,
//                Integer.MAX_VALUE
//        );
//
//        var res = personnelService.rechercher(filtre);
//        return res != null ? res.elements() : List.of();
//    }
//
//    private void lancerRecherche() {
//        if (idEcoleCourante == null) return;
//
//        Departement dept = filtreDepartementCombo.getSelectionModel().getSelectedItem();
//        StatutEmploi statut = filtreStatutCombo.getSelectionModel().getSelectedItem();
//        String idDept = (dept != null) ? dept.getIdDepartement() : null;
//
//        FiltrePersonnel filtre = new FiltrePersonnel(
//                idEcoleCourante,
//                idDept,
//                null,
//                statut,
//                searchPersonnelField.getText(),
//                1,
//                TAILLE_PAGE
//        );
//
//        tablePersonnel.setPlaceholder(new Label("Chargement du personnel..."));
//
//        personnelService.rechercherAsync(filtre,
//                resultat -> Platform.runLater(() -> {
//                    gestionnairePagination.mettreAJourDonnees(resultat.elements());
//                    tablePersonnel.setPlaceholder(new Label("Aucun employé trouvé."));
//                    footerInfoPersonnel.setText(resultat.total() + " employé(s)");
//
//                    afficherCartesPersonnel(resultat.elements());
//                    mettreAJourStatistiques(resultat.elements(), resultat.total());
//
//                    if (autoRefresh != null && autoRefresh.isRunning()) {
//                        autoRefresh.restartNow();
//                    }
//                }),
//                err -> Platform.runLater(() -> {
//                    tablePersonnel.setPlaceholder(new Label("Erreur de chargement."));
//                    AlertUtil.afficherErreur("Erreur", err.getMessage());
//                })
//        );
//    }
//
//    private void appliquerResultatRefresh(List<PersonnelLigne> resultat) {
//        if (resultat == null) return;
//        ObservableList<PersonnelLigne> listeCourante = tablePersonnel.getItems();
//        FusionListeUtil.fusionner(listeCourante, resultat, PersonnelLigne::idPersonnel);
//        gestionnairePagination.mettreAJourDonnees(listeCourante);
//        afficherCartesPersonnel(resultat);
//    }
//
//    /* ============================================================
//       GÉNÉRATION DES CARTES KANBAN (STYLE ODOO RH)
//       ============================================================ */
//    private void afficherCartesPersonnel(List<PersonnelLigne> personnels) {
//        if (cartesPane == null) return;
//        cartesPane.getChildren().clear();
//
//        if (personnels == null || personnels.isEmpty()) {
//            afficherPlaceholder("Aucun employé enregistré");
//            return;
//        }
//
//        for (PersonnelLigne p : personnels) {
//            cartesPane.getChildren().add(creerCartePersonnelOdoo(p));
//        }
//    }
//
//    private Node creerCartePersonnelOdoo(PersonnelLigne p) {
//        VBox carte = new VBox(10);
//        carte.getStyleClass().add("salle-card");
//        carte.setPrefWidth(285);
//        carte.setMinWidth(285);
//        carte.setMaxWidth(285);
//        carte.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 14; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 4);");
//
//        boolean actif = p.estActif();
//        Region stripeTop = new Region();
//        stripeTop.setPrefHeight(4);
//        stripeTop.setStyle("-fx-background-color: " + (actif ? "#10B981" : "#EF4444") + "; -fx-background-radius: 4 4 0 0;");
//
//        HBox header = new HBox(12);
//        header.setAlignment(Pos.CENTER_LEFT);
//
//        Node photoAvatar = AvatarUtil.creer(p.photoPath(), p.nom(), p.prenom(), p.idPersonnel());
//
//        VBox identityBox = new VBox(2);
//        Label lblNom = new Label(p.nomComplet());
//        lblNom.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1E293B;");
//
//        Label lblPoste = new Label(p.poste() != null ? p.poste() : "Employé");
//        lblPoste.setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold; -fx-font-size: 11px;");
//
//        Label lblMatricule = new Label("Matricule : " + (p.matricule() != null ? p.matricule() : "—"));
//        lblMatricule.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
//
//        identityBox.getChildren().addAll(lblNom, lblPoste, lblMatricule);
//        header.getChildren().addAll(photoAvatar, identityBox);
//
//        VBox details = new VBox(6);
//        details.setStyle("-fx-padding: 6 0 0 0;");
//
//        HBox deptRow = new HBox(6, createIcon("fth-briefcase"), new Label(p.nomDepartement() != null ? p.nomDepartement() : "Non assigné"));
//        deptRow.setAlignment(Pos.CENTER_LEFT);
//
//        HBox phoneRow = new HBox(6, createIcon("fth-phone"), new Label(p.telephone() != null ? p.telephone() : "—"));
//        phoneRow.setAlignment(Pos.CENTER_LEFT);
//
//        HBox mailRow = new HBox(6, createIcon("fth-mail"), new Label(p.email() != null ? p.email() : "—"));
//        mailRow.setAlignment(Pos.CENTER_LEFT);
//
//        details.getChildren().addAll(deptRow, phoneRow, mailRow);
//
//        HBox footer = new HBox(8);
//        footer.setAlignment(Pos.CENTER_RIGHT);
//
//        Button btnMore = new Button();
//        btnMore.setGraphic(new FontIcon("fth-more-vertical"));
//        btnMore.getStyleClass().add("salle-card-icon-btn");
//        btnMore.setOnAction(e -> ouvrirMenuActionsLigne(btnMore, p));
//
//        footer.getChildren().addAll(btnMore);
//
//        carte.getChildren().addAll(stripeTop, header, new Separator(), details, new Separator(), footer);
//        return carte;
//    }
//
//    private FontIcon createIcon(String literal) {
//        FontIcon ic = new FontIcon(literal);
//        ic.setIconSize(12);
//        ic.setIconColor(Color.web("#94A3B8"));
//        return ic;
//    }
//
//    /* ============================================================
//       MENU CONTEXTUEL PAR EMPLOYÉ
//       ============================================================ */
//    private void ouvrirMenuActionsLigne(Button btnSource, PersonnelLigne p) {
//        ContextMenu menu = new ContextMenu();
//
//        if (ControleAcces.autoriseAction("personnel.modifier")) {
//            MenuItem edit = new MenuItem("Modifier");
//            edit.setGraphic(new FontIcon("fth-edit-2"));
//            edit.setOnAction(e -> ouvrirModalEdition(p));
//            menu.getItems().add(edit);
//
//            boolean estActif = p.estActif();
//            MenuItem toggleStatut = new MenuItem(estActif ? "Suspendre" : "Réactiver");
//            toggleStatut.setGraphic(new FontIcon(estActif ? "fth-slash" : "fth-check-circle"));
//            toggleStatut.setOnAction(e -> {
//                if (estActif) {
//                    personnelService.changerStatutEmploiAsync(p.idPersonnel(), "SUSPENDU",
//                            () -> Platform.runLater(this::lancerRecherche),
//                            err -> Platform.runLater(() -> ToastNotification.erreur((Stage) rootStack.getScene().getWindow(), err.getMessage())));
//                } else {
//                    personnelService.reactiverAsync(p.idPersonnel(),
//                            () -> Platform.runLater(this::lancerRecherche),
//                            err -> Platform.runLater(() -> ToastNotification.erreur((Stage) rootStack.getScene().getWindow(), err.getMessage())));
//                }
//            });
//            menu.getItems().add(toggleStatut);
//        }
//
//        if (ControleAcces.autoriseAction("personnel.supprimer")) {
//            menu.getItems().add(new SeparatorMenuItem());
//            MenuItem del = new MenuItem("Supprimer");
//            FontIcon icDel = new FontIcon("fth-trash-2");
//            icDel.setStyle("-fx-icon-color: #EF4444;");
//            del.setGraphic(icDel);
//            del.setStyle("-fx-text-fill: #EF4444;");
//            del.setOnAction(e -> supprimerPersonnel(p));
//            menu.getItems().add(del);
//        }
//
//        if (!menu.getItems().isEmpty()) {
//            menu.show(btnSource, Side.BOTTOM, 0, 4);
//        }
//    }
//
//    /* ============================================================
//       STATISTIQUES & HELPERS
//       ============================================================ */
//    private void mettreAJourStatistiques(List<PersonnelLigne> liste, long totalElements) {
//        if (liste == null) return;
//        long actifs = liste.stream().filter(PersonnelLigne::estActif).count();
//        long inactifs = totalElements - actifs;
//
//        statTotalPersonnel.setText(String.valueOf(totalElements));
//        statActifs.setText(String.valueOf(actifs));
//        statInactifs.setText(String.valueOf(inactifs));
//        statEnConge.setText("0");
//    }
//
//    private void afficherPlaceholder(String msg) {
//        cartesPane.getChildren().clear();
//        VBox empty = new VBox(12);
//        empty.setAlignment(Pos.CENTER);
//        empty.getChildren().addAll(new FontIcon("fth-users"), new Label(msg));
//        cartesPane.getChildren().add(empty);
//    }
//
//    private void appliquerControlesAcces() {
//        ControleAcces.appliquerAction(btnNouveauPersonnel, "personnel.creer");
//    }
//
//    private void ouvrirModalCreation() {
//        ToastNotification.info((Stage) rootStack.getScene().getWindow(), "Ouverture du formulaire de création d'employé.");
//    }
//
//    private void ouvrirModalEdition(PersonnelLigne p) {
//        ToastNotification.info((Stage) rootStack.getScene().getWindow(), "Édition de l'employé : " + p.nomComplet());
//    }
//
//    private void supprimerPersonnel(PersonnelLigne p) {
//        boolean ok = AlertUtil.afficherConfirmation("Suppression",
//                "Êtes-vous sûr de vouloir supprimer l'employé " + p.nomComplet() + " ?",
//                rootStack.getScene().getWindow());
//
//        if (ok) {
//            personnelService.supprimerAsync(p.idPersonnel(),
//                    () -> Platform.runLater(() -> {
//                        auditService.tracerActionAsync("GRH", "SUPPRESSION_PERSONNEL", "Personnel supprimé : " + p.nomComplet());
//                        ToastNotification.succes((Stage) rootStack.getScene().getWindow(), "Employé supprimé.");
//                        lancerRecherche();
//                    }),
//                    err -> Platform.runLater(() -> ToastNotification.erreur((Stage) rootStack.getScene().getWindow(), err.getMessage()))
//            );
//        }
//    }
//}