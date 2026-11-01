package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.erpklassup.erpklassup.controllers.actionPage.UserDetailController;
import org.erpklassup.erpklassup.dto.UtilisateurLigne;
import org.erpklassup.erpklassup.models.module2.Salle;
import org.erpklassup.erpklassup.models.module2.Salle.TypeSalle;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.module2.SalleService;
import org.erpklassup.erpklassup.util.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Controller de l'onglet « Salles ».
 *
 * <p>Supporte deux vues :
 * <ul>
 *   <li><b>Cartes</b> : FlowPane avec cartes visuelles par salle</li>
 *   <li><b>Tableau</b> : TableView classique</li>
 * </ul>
 * </p>
 */
public class SalleController implements Initializable, VueDisposable {

    /* ============================================================
       FXML — TOOLBAR
       ============================================================ */
    @FXML private StackPane rootStack;
    @FXML private TextField searchSalleField;
    @FXML private Button clearSearchSalle;
    @FXML private ComboBox<TypeSalle> filtreTypeCombo;
    @FXML private ComboBox<String> filtreBatimentCombo;
    @FXML private ComboBox<String> filtreOccupationCombo;
    @FXML private ToggleButton btnVueCartes;
    @FXML private ToggleButton btnVueTable;
    @FXML private Button btnNouvelleSalle;

    /* ============================================================
       FXML — STATISTIQUES
       ============================================================ */
    @FXML private Label statTotalSalles;
    @FXML private Label statSallesLibres;
    @FXML private Label statSallesOccupees;
    @FXML private Label statCapaciteTotale;

    /* ============================================================
       FXML — VUE CARTES
       ============================================================ */
    @FXML private ScrollPane scrollCartes;
    @FXML private FlowPane cartesPane;

    /* ============================================================
       FXML — VUE TABLEAU
       ============================================================ */
    @FXML private VBox containerTable;
    @FXML private TableView<Salle> tableSalles;
    @FXML private TableColumn<Salle, String> colSalleCode;
    @FXML private TableColumn<Salle, String> colSalleNom;
    @FXML private TableColumn<Salle, String> colSalleBatiment;
    @FXML private TableColumn<Salle, Salle> colSalleType;
    @FXML private TableColumn<Salle, Integer> colSalleCapacite;
    @FXML private TableColumn<Salle, Salle> colSalleStatut;
    @FXML private TableColumn<Salle, Salle> colSalleActions;
    @FXML private Label footerInfoSalle;

    /* ============================================================
       FXML — DRAWER
       ============================================================ */
    @FXML private StackPane drawerOverlaySalle;
    @FXML private FontIcon drawerSalleIcon;
    @FXML private Label drawerSalleTitle;
    @FXML private Label drawerSalleSubtitle;
    @FXML private Button btnCloseSalle;
    @FXML private TextField fieldSalleCode;
    @FXML private TextField fieldSalleNom;
    @FXML private TextField fieldSalleBatiment;
    @FXML private TextField fieldSalleCapacite;
    @FXML private ComboBox<TypeSalle> fieldSalleType;
    @FXML private ToggleButton toggleSalleOccupee;
    @FXML private Label errSalleCode;
    @FXML private Label errSalleNom;
    @FXML private Label errSalleType;
    @FXML private Label errSalleCapacite;
    @FXML private Label errSalleGlobal;
    @FXML private Button btnCancelSalle;
    @FXML private Button btnSaveSalle;

    /* ============================================================
       FXML — CONFIRMATION
       ============================================================ */
    @FXML private StackPane confirmOverlay;
    @FXML private Label confirmTitle;
    @FXML private Label confirmMessage;
    @FXML private Button btnCancelDelete;
    @FXML private Button btnConfirmDelete;

    /* ============================================================
       FXML — TOAST
       ============================================================ */
    @FXML private HBox toastBox;
    @FXML private FontIcon toastIcon;
    @FXML private Label toastMessage;

    /* ============================================================
       CONSTANTES
       ============================================================ */
    private static final Duration PERIODE_REFRESH = Duration.seconds(15);
    private static final String OPTION_TOUTES = "Toutes";
    private static final String OPTION_TOUS_BATIMENTS = "Tous les bâtiments";

    /* ============================================================
       SERVICES
       ============================================================ */
    private final SalleService salleService = new SalleService();
    private final AuditService auditService = new AuditService();

    /* ============================================================
       ÉTAT
       ============================================================ */
    private String idEcoleCourante;
    private AutoRefreshService<List<Salle>> autoRefresh;
    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;
    private final Runnable ecouteurI18n = this::rafraichirTextes;

    private Salle salleEnEdition;
    private Runnable pendingDelete;

    /* ============================================================
       i18n
       ============================================================ */
    private I18nManager i18n() {
        return I18nManager.getInstance();
    }

    /* ============================================================
       INITIALISATION
       ============================================================ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();

        configurerFiltres();
        configurerTable();
        configurerRecherche();
        configurerToggleVue();
        configBtn();

        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
        appliquerControlesAcces();
        i18n().ecouterChangement(ecouteurI18n);

        if (idEcoleCourante == null) {
            afficherPlaceholder("Aucune école sélectionnée");
            return;
        }

        chargerBatiments();
        demarrerRafraichissementAuto();
    }

    /* ============================================================
       DISPOSE
       ============================================================ */
    @Override
    public void disposer() {
        if (autoRefresh != null) {
            autoRefresh.dispose();
            autoRefresh = null;
        }
        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
        i18n().arreterEcoute(ecouteurI18n);
    }

    /* ============================================================
       CONFIGURATION DES FILTRES
       ============================================================ */
    private void configurerFiltres() {
        // Type
        var types = FXCollections.<TypeSalle>observableArrayList();
        types.add(null);   // "Tous les types"
        types.addAll(TypeSalle.values());
        filtreTypeCombo.setItems(types);

        filtreTypeCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TypeSalle item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Tous les types");
                    setGraphic(null);
                } else {
                    FontIcon ic = new FontIcon(item.getIcone());
                    ic.setIconSize(12);
                    ic.setIconColor(Color.web("#64748B"));
                    setGraphic(ic);
                    setText(item.getLibelle());
                }
            }
        });
        filtreTypeCombo.setButtonCell(filtreTypeCombo.getCellFactory().call(null));
        filtreTypeCombo.getSelectionModel().selectFirst();

        // Bâtiment
        filtreBatimentCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? OPTION_TOUS_BATIMENTS : item);
            }
        });
        filtreBatimentCombo.setButtonCell(filtreBatimentCombo.getCellFactory().call(null));

        // Occupation
        filtreOccupationCombo.setItems(FXCollections.observableArrayList(
                OPTION_TOUTES, "Libres", "Occupées"
        ));
        filtreOccupationCombo.getSelectionModel().selectFirst();

        // Listener : tout changement relance le refresh
        filtreTypeCombo.getSelectionModel().selectedItemProperty()
                .addListener((o, a, n) -> relancerRefresh());
        filtreBatimentCombo.getSelectionModel().selectedItemProperty()
                .addListener((o, a, n) -> relancerRefresh());
        filtreOccupationCombo.getSelectionModel().selectedItemProperty()
                .addListener((o, a, n) -> relancerRefresh());
    }

    private void chargerBatiments() {
        salleService.listerBatimentsAsync(idEcoleCourante,
                batiments -> Platform.runLater(() -> {
                    var items = FXCollections.<String>observableArrayList();
                    items.add(null);   // Option "Tous les bâtiments"
                    items.addAll(batiments);
                    filtreBatimentCombo.setItems(items);
                    filtreBatimentCombo.getSelectionModel().selectFirst();
                }),
                err -> System.err.println("❌ Erreur chargement bâtiments : " + err.getMessage()));
    }

    /* ============================================================
       CONFIGURATION DE LA RECHERCHE
       ============================================================ */
    private void configurerRecherche() {
        RechercheUtil.configurer(
                searchSalleField,
                clearSearchSalle,
                this::relancerRefresh
        );
    }

    /* ============================================================
       CONFIGURATION DU TOGGLE VUE
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
       CONFIGURATION DE LA TABLE
       ============================================================ */
    private void configurerTable() {
        tableSalles.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Code
        colSalleCode.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getCodeSalle()));

        // Nom
        colSalleNom.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getNomSalle()));

        // Bâtiment
        colSalleBatiment.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getBatiment() != null ? data.getValue().getBatiment() : "—"));

        // Type
        colSalleType.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        colSalleType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Salle salle, boolean vide) {
                super.updateItem(salle, vide);
                if (vide || salle == null) { setGraphic(null); return; }

                HBox box = new HBox(8);
                box.setAlignment(Pos.CENTER_LEFT);

                FontIcon ic = new FontIcon(salle.getTypeSalle().getIcone());
                ic.setIconSize(14);
                ic.setIconColor(Color.web("#F5A524"));

                Label lbl = new Label(salle.getTypeSalle().getLibelle());
                lbl.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");

                box.getChildren().addAll(ic, lbl);
                setGraphic(box);
                setText(null);
            }
        });

        // Capacité
        colSalleCapacite.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getCapaciteMax()));

        // Statut
        colSalleStatut.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        colSalleStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Salle salle, boolean vide) {
                super.updateItem(salle, vide);
                if (vide || salle == null) { setGraphic(null); return; }

                Label badge = new Label(salle.isOccupee() ? "● Occupée" : "● Libre");
                badge.getStyleClass().addAll("event-tag",
                        salle.isOccupee() ? "tag-red" : "tag-green");
                setGraphic(badge);
                setText(null);
            }
        });

        // Actions
        colSalleActions.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        colSalleActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnToggle = new Button();
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox box = new HBox(6, btnToggle, btnEdit, btnDelete);

            {
                box.setAlignment(Pos.CENTER);

                FontIcon iconToggle = new FontIcon("fth-lock");
                iconToggle.setIconSize(13);
                btnToggle.setGraphic(iconToggle);
                btnToggle.getStyleClass().add("btn-icon-light");
                btnToggle.setTooltip(new Tooltip("Basculer occupation"));

                FontIcon iconEdit = new FontIcon("fth-edit-2");
                iconEdit.setIconSize(13);
                btnEdit.setGraphic(iconEdit);
                btnEdit.getStyleClass().add("btn-icon-light");
                btnEdit.setTooltip(new Tooltip("Modifier"));

                FontIcon iconDelete = new FontIcon("fth-trash-2");
                iconDelete.setIconSize(13);
                btnDelete.setGraphic(iconDelete);
                btnDelete.getStyleClass().addAll("btn-icon-light", "btn-danger-icon");
                btnDelete.setTooltip(new Tooltip("Supprimer"));

                btnToggle.setOnAction(e -> {
                    Salle s = getItem();
                    if (s != null) basculerOccupation(s);
                });
                btnEdit.setOnAction(e -> {
                    Salle s = getItem();
                    if (s != null) ouvrirDrawerEdition(s);
                });
                btnDelete.setOnAction(e -> {
                    Salle s = getItem();
                    if (s != null) supprimerSalle(s);
                });
            }

            @Override
            protected void updateItem(Salle salle, boolean vide) {
                super.updateItem(salle, vide);
                if (vide || salle == null) { setGraphic(null); return; }

                // Icône toggle dynamique
                FontIcon ic = (FontIcon) btnToggle.getGraphic();
                ic.setIconLiteral(salle.isOccupee() ? "fth-unlock" : "fth-lock");

                // Permissions
                btnToggle.setVisible(ControleAcces.autoriseAction("user.creer"));
                btnToggle.setManaged(btnToggle.isVisible());
                btnEdit.setVisible(ControleAcces.autoriseAction("user.creer"));
                btnEdit.setManaged(btnEdit.isVisible());
                btnDelete.setVisible(ControleAcces.autoriseAction("user.creer"));
                btnDelete.setManaged(btnDelete.isVisible());

                setGraphic(box);
                setText(null);
            }
        });
    }

    /* ============================================================
       CONTRÔLES D'ACCÈS
       ============================================================ */
    private void appliquerControlesAcces() {
//        ControleAcces.appliquerAction(btnNouvelleSalle, "salle.create");
        if (tableSalles != null) tableSalles.refresh();
    }

    private void configBtn(){
//        ControleAcces.appliquerAction(btnNouvelleSalle, "salle.create");
        btnNouvelleSalle.setOnAction(e -> ouvrirDrawerCreation());
    }

    /* ============================================================
       i18n
       ============================================================ */
    private void rafraichirTextes() {
        if (tableSalles != null) tableSalles.refresh();
    }


    private void supprimerSalle(Salle salle) {
        Boolean confirmation = AlertUtil.afficherConfirmation(

                "Supprimer Salle",
                "Est-vous sûr de vouloir supprimer la Salle  << " + salle.getCodeSalle()+" >>",
                btnNouvelleSalle.getScene().getWindow()
        );

        if (confirmation) {
            salleService.supprimerAsync(salle.getIdSalle(),
                    () -> Platform.runLater(() -> {
                        auditService.tracerActionAsync("STRUCTURE", "SUPPRESSION_SALLE",
                                "Salle Supprimée : " + salle.getCodeSalle());

                        ToastNotification.succes((Stage) btnNouvelleSalle.getScene().getWindow(),
                                "salle " + salle.getCodeSalle() + "supprimée");
                        relancerRefresh();
                    }),
                    err -> Platform.runLater(() ->
                            ToastNotification.erreur(getStage(), "Erreur : " + err.getMessage())));
        }
    }

    /* ============================================================
       AUTO-REFRESH
       ============================================================ */
    private void demarrerRafraichissementAuto() {
        autoRefresh = new AutoRefreshService<List<Salle>>(PERIODE_REFRESH)
                .loader(this::chargerSallesPourRefresh)
                .onBeforeLoad(() -> {
                    if (tableSalles.getItems().isEmpty() && cartesPane.getChildren().isEmpty()) {
                        afficherPlaceholder("Chargement des salles...");
                    }
                })
                .onSuccess(this::appliquerResultat)
                .onFailure(err -> {
                    System.err.println("❌ Auto-refresh salles : " + err.getMessage());
                    afficherPlaceholder("Erreur de chargement");
                })
                .start();
    }

    private List<Salle> chargerSallesPourRefresh() {
        if (idEcoleCourante == null) return List.of();

        String terme = searchSalleField.getText();
        TypeSalle type = filtreTypeCombo.getSelectionModel().getSelectedItem();
        String batiment = filtreBatimentCombo.getSelectionModel().getSelectedItem();

        // Occupation
        Boolean occupee = null;
        String occStr = filtreOccupationCombo.getSelectionModel().getSelectedItem();
        if (occStr != null) {
            if ("Libres".equals(occStr)) occupee = false;
            else if ("Occupées".equals(occStr)) occupee = true;
        }

        return salleService.rechercher(idEcoleCourante, terme, type, batiment, occupee);
    }

    private void relancerRefresh() {
        if (autoRefresh != null) autoRefresh.restartNow();
    }

    private void appliquerResultat(List<Salle> salles) {
        if (salles == null) return;

        // Remplir la table
        tableSalles.setItems(FXCollections.observableArrayList(salles));
        tableSalles.setPlaceholder(new Label("Aucune salle"));

        // Remplir les cartes
        afficherCartesSalles(salles);

        // Stats
        mettreAJourStatistiques(salles);

        // Footer
        long nb = salles.size();
        footerInfoSalle.setText(nb + (nb > 1 ? " salles" : " salle"));
    }

    /* ============================================================
       STATISTIQUES
       ============================================================ */
    private void mettreAJourStatistiques(List<Salle> salles) {
        if (salles == null) return;

        long total = salles.size();
        long libres = salles.stream().filter(Salle::estLibre).count();
        long occupees = total - libres;
        int capacite = salles.stream().mapToInt(Salle::getCapaciteMax).sum();

        statTotalSalles.setText(String.valueOf(total));
        statSallesLibres.setText(String.valueOf(libres));
        statSallesOccupees.setText(String.valueOf(occupees));
        statCapaciteTotale.setText(String.valueOf(capacite));
    }

    /* ============================================================
       AFFICHAGE DES CARTES
       ============================================================ */
    private void afficherCartesSalles(List<Salle> salles) {
        if (cartesPane == null) return;

        cartesPane.getChildren().clear();

        if (salles == null || salles.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPrefWidth(800);
            empty.setPrefHeight(300);

            FontIcon icon = new FontIcon("fth-home");
            icon.setIconSize(48);
            icon.setIconColor(Color.web("#CBD5E1"));

            Label msg = new Label("Aucune salle enregistrée");
            msg.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");

            empty.getChildren().addAll(icon, msg);
            cartesPane.getChildren().add(empty);
            return;
        }

        for (Salle salle : salles) {
            cartesPane.getChildren().add(creerCarteSalle(salle));
        }
    }

    /**
     * Génère une carte visuelle pour une salle.
     */
    /**
     * Génère une carte visuelle pour une salle.
     * Design : bande supérieure colorée + contenu structuré + footer avec actions.
     */
    private Node creerCarteSalle(Salle salle) {

        // ============================================================
        // CONTENEUR PRINCIPAL — VBox vertical
        // ============================================================
        VBox carte = new VBox();
        carte.getStyleClass().add("salle-card");
        carte.setPrefWidth(265);
        carte.setMinWidth(265);
        carte.setMaxWidth(265);
        carte.setPrefHeight(285);
        carte.setMinHeight(285);
        carte.setMaxHeight(285);

        // ============================================================
        // BANDE SUPÉRIEURE COLORÉE (selon type de salle)
        // ============================================================
        Region stripeTop = new Region();
        stripeTop.getStyleClass().add("salle-card-stripe-top");
        stripeTop.setPrefHeight(6);
        stripeTop.setMinHeight(6);
        stripeTop.setMaxHeight(6);

        // Couleur selon type
        String stripeColor = switch (salle.getTypeSalle()) {
            case SALLE -> "#3B82F6";           // Bleu
            case LABO -> "#8B5CF6";            // Violet
            case AMPHI -> "#F5A524";           // Orange
            case ATELIER -> "#EA580C";         // Orange foncé
            case SALLE_INFO -> "#06B6D4";      // Cyan
            case BIBLIOTHEQUE -> "#EC4899";    // Rose
            case COUR -> "#CA8A04";            // Jaune foncé
        };
        stripeTop.setStyle("-fx-background-color: " + stripeColor + "; " +
                "-fx-background-radius: 12 12 0 0;");

        // ============================================================
        // HEADER — Icône + Code + Nom
        // ============================================================
        HBox header = new HBox(14);
        header.getStyleClass().add("salle-card-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // Icône type (grande, colorée)
        StackPane iconContainer = new StackPane();
        iconContainer.getStyleClass().add("salle-card-icon-container");
        iconContainer.setStyle("-fx-background-color: " + stripeColor + "1A; " +
                "-fx-background-radius: 12; " +
                "-fx-min-width: 52; -fx-min-height: 52; " +
                "-fx-max-width: 52; -fx-max-height: 52;");

        FontIcon icone = new FontIcon(salle.getTypeSalle().getIcone());
        icone.setIconSize(26);
        icone.setIconColor(Color.web(stripeColor));
        iconContainer.getChildren().add(icone);

        // Code + Nom
        VBox headerText = new VBox(4);
        headerText.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headerText, Priority.ALWAYS);

        Label lblCode = new Label(salle.getCodeSalle());
        lblCode.getStyleClass().add("salle-card-code");

        Label lblNom = new Label(salle.getNomSalle());
        lblNom.getStyleClass().add("salle-card-nom");
        lblNom.setWrapText(true);
        lblNom.setMaxWidth(180);

        headerText.getChildren().addAll(lblCode, lblNom);
        header.getChildren().addAll(iconContainer, headerText);

        // ============================================================
        // SÉPARATEUR
        // ============================================================
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #F1F5F9;");

        // ============================================================
        // BODY — Bâtiment + Type + Capacité
        // ============================================================
        VBox body = new VBox(12);
        body.getStyleClass().add("salle-card-body-v2");
        VBox.setVgrow(body, Priority.ALWAYS);

        // Ligne bâtiment
        HBox ligneBatiment = new HBox(8);
        ligneBatiment.setAlignment(Pos.CENTER_LEFT);
        FontIcon iconBat = new FontIcon("fth-map-pin");
        iconBat.setIconSize(13);
        iconBat.setIconColor(Color.web("#94A3B8"));

        Label lblBat = new Label(
                (salle.getBatiment() != null && !salle.getBatiment().isBlank())
                        ? salle.getBatiment() : "Aucun bâtiment");
        lblBat.getStyleClass().add("salle-card-batiment-v2");

        ligneBatiment.getChildren().addAll(iconBat, lblBat);

        // Ligne type
        HBox ligneType = new HBox(8);
        ligneType.setAlignment(Pos.CENTER_LEFT);
        FontIcon iconTag = new FontIcon("fth-tag");
        iconTag.setIconSize(13);
        iconTag.setIconColor(Color.web("#94A3B8"));

        Label lblType = new Label(salle.getTypeSalle().getLibelle());
        lblType.getStyleClass().add("salle-card-info-v2");

        ligneType.getChildren().addAll(iconTag, lblType);

        // Ligne capacité
        HBox ligneCapacite = new HBox(8);
        ligneCapacite.setAlignment(Pos.CENTER_LEFT);
        FontIcon iconUsers = new FontIcon("fth-users");
        iconUsers.setIconSize(13);
        iconUsers.setIconColor(Color.web("#94A3B8"));

        Label lblCapacite = new Label(salle.getCapaciteMax() + " places");
        lblCapacite.getStyleClass().add("salle-card-info-v2");

        ligneCapacite.getChildren().addAll(iconUsers, lblCapacite);

        body.getChildren().addAll(ligneBatiment, ligneType, ligneCapacite);

        // ============================================================
        // SÉPARATEUR
        // ============================================================
        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color: #F1F5F9;");

        // ============================================================
        // FOOTER — Badge statut + Actions
        // ============================================================
        HBox footer = new HBox(8);
        footer.getStyleClass().add("salle-card-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        // Badge statut
        Label badge = new Label(salle.isOccupee() ? "● Occupée" : "● Libre");
        badge.getStyleClass().addAll("salle-card-badge",
                salle.isOccupee() ? "salle-card-badge-occupee" : "salle-card-badge-libre");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton toggle occupation
        Button btnToggle = new Button();
        FontIcon iconToggle = new FontIcon(salle.isOccupee() ? "fth-unlock" : "fth-lock");
        iconToggle.setIconSize(13);
        iconToggle.setIconColor(Color.web(salle.isOccupee() ? "#E5484D" : "#16A34A"));
        btnToggle.setGraphic(iconToggle);
        btnToggle.getStyleClass().add("salle-card-icon-btn");
        btnToggle.setTooltip(new Tooltip(salle.isOccupee() ? "Marquer comme libre" : "Marquer comme occupée"));
        btnToggle.setOnAction(e -> basculerOccupation(salle));

        // Bouton éditer
        Button btnEdit = new Button();
        FontIcon iconEdit = new FontIcon("fth-edit-2");
        iconEdit.setIconSize(13);
        iconEdit.setIconColor(Color.web("#3B82F6"));
        btnEdit.setGraphic(iconEdit);
        btnEdit.getStyleClass().add("salle-card-icon-btn");
        btnEdit.setTooltip(new Tooltip("Modifier"));
        btnEdit.setOnAction(e -> ouvrirDrawerEdition(salle));

        // Bouton supprimer
        Button btnDelete = new Button();
        FontIcon iconDelete = new FontIcon("fth-trash-2");
        iconDelete.setIconSize(13);
        iconDelete.setIconColor(Color.web("#E5484D"));
        btnDelete.setGraphic(iconDelete);
        btnDelete.getStyleClass().add("salle-card-icon-btn");
        btnDelete.setTooltip(new Tooltip("Supprimer"));
        btnDelete.setOnAction(e -> supprimerSalle(salle));

        footer.getChildren().addAll(badge, spacer, btnToggle, btnEdit, btnDelete);

        // ============================================================
        // PERMISSIONS
        // ============================================================

//        if (!ControleAcces.autoriseAction("salle.update")) {
//            btnToggle.setVisible(false); btnToggle.setManaged(false);
//            btnEdit.setVisible(false);   btnEdit.setManaged(false);
//        }
//        if (!ControleAcces.autoriseAction("salle.delete")) {
//            btnDelete.setVisible(false); btnDelete.setManaged(false);
//        }

        // ============================================================
        // ASSEMBLAGE
        // ============================================================
        carte.getChildren().addAll(stripeTop, header, sep1, body, sep2, footer);
        return carte;
    }

    /* ============================================================
       DRAWER — CRÉATION / ÉDITION
       ============================================================ */

    private void ouvrirDrawerCreation() {
        String fxmlPath = "/org/erpklassup/erpklassup/view/module2/salle-modal-view.fxml";
        SalleModalController ctrl = ModalUtil.ouvrirModal(
                getClass(),
                fxmlPath,
                "Créer nouvelle salle",
                btnNouvelleSalle.getScene().getWindow(),
                560,
                530
        );

        if (ctrl != null) {
            ctrl.initCreation(this.idEcoleCourante, this::relancerRefresh);
        }

    }

    private void ouvrirDrawerEdition(Salle salle) {
        String fxmlPath = "/org/erpklassup/erpklassup/view/module2/salle-modal-view.fxml";
        SalleModalController ctrl = ModalUtil.ouvrirModal(
                getClass(),
                fxmlPath,
                "Modifier  une salle",
                btnNouvelleSalle.getScene().getWindow(),
                560,
                530
        );

        if (ctrl != null) {
            ctrl.initEdition(salle, this::relancerRefresh);
        }
    }

    /* ============================================================
       BASCULER OCCUPATION
       ============================================================ */
//    private void basculerOccupation(Salle salle) {
////        if (!ControleAcces.autoriseAction("salle.update")) {
////            ToastNotification.avertissement(getStage(), "Permission refusée");
////            return;
////        }
//
//        salleService.basculerOccupationAsync(salle.getId(),
//                () -> Platform.runLater(() -> {
//                    relancerRefresh();
//
//                    String etat = salle.isOccupee() ? "marquée comme libre" : "marquée comme occupée";
//                    ToastNotification.succes(getStage(),
//                            "Salle « " + salle.getCodeSalle() + " » " + etat);
//                    auditService.tracerActionAsync("STRUCTURE", "OCCUPATION_SALLE",
//                            "Salle " + salle.getCodeSalle() + " : " + etat);
//                }),
//                err -> Platform.runLater(() ->
//                        ToastNotification.erreur(getStage(), "Erreur : " + err.getMessage())));
//    }

/* ============================================================
   BASCULER OCCUPATION
   ============================================================ */
private void basculerOccupation(Salle salle) {
    if (salle == null) return;

    // Si on veut LIBÉRER la salle (elle est actuellement occupée)
    if (salle.isOccupee()) {
        salleService.listerNomsClassesDansSalleAsync(salle.getId(),
                noms -> Platform.runLater(() -> {
                    if (noms.isEmpty()) {
                        // Aucune classe : on libère directement
                        effectuerBascule(salle);
                    } else {
                        // ✅ ALERTE INFORMATIVE BLOQUANTE
                        String message = "Impossible de libérer cette salle.\n\n"
                                + "Elle est actuellement occupée par les classes suivantes :\n\n"
                                + noms.stream().map(n -> "• " + n)
                                .collect(java.util.stream.Collectors.joining("\n"))
                                + "\n\nRéaffectez ces classes à une autre salle avant de pouvoir libérer celle-ci.";

                        AlertUtil.afficherInformation(
                                "Salle occupée",
                                message,
                                getStage());
                        // ❌ Pas de bascule — l'action est bloquée
                    }
                }),
                err -> Platform.runLater(() ->
                        ToastNotification.erreur(getStage(),
                                "Erreur : " + err.getMessage())));
        return;
    }

    // Sinon (on veut OCCUPER la salle) → bascule directe
    effectuerBascule(salle);
}

    /**
     * Effectue réellement la bascule d'occupation.
     */
    private void effectuerBascule(Salle salle) {
        salleService.basculerOccupationAsync(salle.getId(),
                () -> Platform.runLater(() -> {
                    relancerRefresh();

                    String etat = salle.isOccupee() ? "marquée comme libre" : "marquée comme occupée";
                    ToastNotification.succes(getStage(),
                            "Salle « " + salle.getCodeSalle() + " » " + etat);
                    auditService.tracerActionAsync("STRUCTURE", "OCCUPATION_SALLE",
                            "Salle " + salle.getCodeSalle() + " : " + etat);
                }),
                err -> Platform.runLater(() ->
                        ToastNotification.erreur(getStage(), "Erreur : " + err.getMessage())));
    }



    /* ============================================================
       SUPPRESSION
       ============================================================ */

    /* ============================================================
       HELPERS UI
       ============================================================ */
    private void afficherPlaceholder(String message) {
        if (cartesPane != null) {
            cartesPane.getChildren().clear();
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPrefWidth(800);
            empty.setPrefHeight(300);

            FontIcon icon = new FontIcon("fth-home");
            icon.setIconSize(48);
            icon.setIconColor(Color.web("#CBD5E1"));

            Label msg = new Label(message);
            msg.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");

            empty.getChildren().addAll(icon, msg);
            cartesPane.getChildren().add(empty);
        }
    }

    private void showDrawer() {
        drawerOverlaySalle.setVisible(true);
        drawerOverlaySalle.setManaged(true);
    }

    private void fermerDrawer() {
        drawerOverlaySalle.setVisible(false);
        drawerOverlaySalle.setManaged(false);
        salleEnEdition = null;
    }

    private void showConfirm() {
        confirmOverlay.setVisible(true);
        confirmOverlay.setManaged(true);
    }

    private void hideConfirm() {
        confirmOverlay.setVisible(false);
        confirmOverlay.setManaged(false);
    }

    private void showError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideAllErrors() {
        for (Label l : new Label[]{errSalleCode, errSalleNom, errSalleType,
                errSalleCapacite, errSalleGlobal}) {
            if (l != null) {
                l.setVisible(false);
                l.setManaged(false);
            }
        }
    }

    private Stage getStage() {
        if (rootStack == null || rootStack.getScene() == null) return null;
        return (Stage) rootStack.getScene().getWindow();
    }
}