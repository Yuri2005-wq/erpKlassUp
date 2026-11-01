package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.erpklassup.erpklassup.models.module2.AnneeScolaire;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.module2.AnneeScolaireService;
import org.erpklassup.erpklassup.util.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller de l'onglet « Années scolaires ».
 *
 * <p>Respecte les patterns établis :
 * <ul>
 *   <li>Permissions via {@link ControleAcces}</li>
 *   <li>Modale via {@link ModalUtil}</li>
 *   <li>Confirmation via {@link AlertUtil}</li>
 *   <li>Rafraîchissement auto via {@link AutoRefreshService}</li>
 *   <li>Audit via {@link AuditService}</li>
 *   <li>i18n via {@link I18nManager}</li>
 * </ul>
 * </p>
 */
public class AnneeController implements Initializable, VueDisposable {

    /* ============================================================
       FXML — TOOLBAR
       ============================================================ */
    @FXML private StackPane rootStack;
    @FXML private TextField searchAnneeField;
    @FXML private Button clearSearchAnnee;
    @FXML private ComboBox<AnneeScolaire.Statut> filterStatut;
    @FXML private Button btnNouvelleAnnee;

    /* ============================================================
       FXML — TABLE
       ============================================================ */
    @FXML private TableView<AnneeScolaire> tableAnnees;
    @FXML private TableColumn<AnneeScolaire, String>        colAnneeLibelle;
    @FXML private TableColumn<AnneeScolaire, String>        colAnneeDebut;
    @FXML private TableColumn<AnneeScolaire, String>        colAnneeFin;
    @FXML private TableColumn<AnneeScolaire, AnneeScolaire> colAnneeStatut;
    @FXML private TableColumn<AnneeScolaire, AnneeScolaire> colAnneeActions;
    @FXML private Label footerInfoAnnee;
    @FXML private HBox  paginationAnnee;

    /* ============================================================
       FXML — TOAST
       ============================================================ */
    @FXML private HBox      toastBox;
    @FXML private FontIcon  toastIcon;
    @FXML private Label     toastMessage;

    /* ============================================================
       CONSTANTES
       ============================================================ */
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Duration PERIODE_REFRESH = Duration.seconds(15);

    /* ============================================================
       SERVICES
       ============================================================ */
    private final AnneeScolaireService anneeService = new AnneeScolaireService();
    private final AuditService auditService = new AuditService();

    /* ============================================================
       ÉTAT
       ============================================================ */
    private String idEcoleCourante;
    private AutoRefreshService<List<AnneeScolaire>> autoRefresh;
    private final PauseTransition debounceRecherche = new PauseTransition(Duration.millis(350));
    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;
    private final Runnable ecouteurI18n = this::rafraichirTextes;

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
        tableAnnees.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        configurerFiltres();
        configurerTable();
        configurerRecherche();
        configurerBoutons();

        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
        appliquerControlesAcces();
        i18n().ecouterChangement(ecouteurI18n);

        if (idEcoleCourante == null) {
            tableAnnees.setPlaceholder(new Label(i18n().t("annee.value.no_school")));
            return;
        }

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
       FILTRES
       ============================================================ */
    private void configurerFiltres() {
        // ComboBox statuts : [null (Tous)] + valeurs de l'enum
        var statuts = FXCollections.<AnneeScolaire.Statut>observableArrayList();
        statuts.add(null);   // option "Tous les statuts"
        statuts.addAll(AnneeScolaire.Statut.values());

        filterStatut.setItems(statuts);

        filterStatut.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AnneeScolaire.Statut item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Tous les statuts");
                } else {
                    setText(item.getLibelle());
                }
            }
        });
        filterStatut.setButtonCell(filterStatut.getCellFactory().call(null));
        filterStatut.getSelectionModel().selectFirst();

        // Au changement du filtre, on relance le refresh
        filterStatut.getSelectionModel().selectedItemProperty()
                .addListener((o, a, n) -> {
                    if (autoRefresh != null) autoRefresh.restartNow();
                });
    }

    /* ============================================================
       AUTO-REFRESH
       ============================================================ */
    private void demarrerRafraichissementAuto() {
        autoRefresh = new AutoRefreshService<List<AnneeScolaire>>(PERIODE_REFRESH)
                .loader(this::chargerAnneesPourRefresh)
                .onBeforeLoad(() -> {
                    if (tableAnnees.getItems().isEmpty()) {
                        tableAnnees.setPlaceholder(new Label(i18n().t("annee.value.loading")));
                    }
                })
                .onSuccess(this::appliquerResultat)
                .onFailure(err -> {
                    System.err.println("❌ Auto-refresh années : " + err.getMessage());
                    if (tableAnnees.getItems().isEmpty()) {
                        tableAnnees.setPlaceholder(new Label(i18n().t("annee.value.error_loading")));
                    }
                })
                .start();
    }

    /**
     * Charge la liste des années selon les filtres en cours (thread background).
     */
    private List<AnneeScolaire> chargerAnneesPourRefresh() {
        if (idEcoleCourante == null) return List.of();

        String terme = searchAnneeField.getText();
        AnneeScolaire.Statut statut = filterStatut.getSelectionModel().getSelectedItem();

        // Récupère toutes les années (avec terme de recherche)
        List<AnneeScolaire> annees = (terme != null && !terme.isBlank())
                ? anneeService.rechercher(idEcoleCourante, terme)
                : anneeService.listerAnnees(idEcoleCourante);

        // Filtrage par statut (en mémoire)
        if (statut != null) {
            annees = annees.stream()
                    .filter(a -> a.getStatut() == statut)
                    .toList();
        }
        return annees;
    }

    private void appliquerResultat(List<AnneeScolaire> annees) {
        if (annees == null) return;

        tableAnnees.setItems(FXCollections.observableArrayList(annees));
        tableAnnees.setPlaceholder(new Label(i18n().t("annee.value.empty")));

        long nb = annees.size();
        footerInfoAnnee.setText(nb + (nb > 1 ? " années" : " année"));
    }

    /* ============================================================
       i18n
       ============================================================ */
    private void rafraichirTextes() {
        if (tableAnnees != null) tableAnnees.refresh();

        if (tableAnnees != null && tableAnnees.getItems().isEmpty()) {
            tableAnnees.setPlaceholder(new Label(i18n().t("annee.value.empty")));
        }

        // Rafraîchir le ComboBox (le "Tous les statuts" est en dur, à adapter si i18n)
        if (filterStatut != null) filterStatut.setButtonCell(filterStatut.getCellFactory().call(null));
    }

    /* ============================================================
       CONTRÔLES D'ACCÈS
       ============================================================ */
    private void appliquerControlesAcces() {
        ControleAcces.appliquerAction(btnNouvelleAnnee, "user.creer");
        if (tableAnnees != null) tableAnnees.refresh();
    }

    /* ============================================================
       CONFIGURATION DE LA TABLE
       ============================================================ */
    private void configurerTable() {

        // ---------- Colonne Libellé ----------
        colAnneeLibelle.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getLibelle()));
        colAnneeLibelle.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String libelle, boolean vide) {
                super.updateItem(libelle, vide);
                if (vide || libelle == null) { setText(null); setGraphic(null); return; }

                HBox box = new HBox(8);
                box.setAlignment(Pos.CENTER_LEFT);

                FontIcon icone = new FontIcon("fth-calendar");
                icone.setIconSize(14);
                icone.getStyleClass().add("icon-orange");
                Label label = new Label(libelle);
                label.setStyle("-fx-font-weight: 700; -fx-text-fill: #16213A;");

                box.getChildren().addAll(icone, label);
                setGraphic(box);
                setText(null);
            }
        });

        // ---------- Colonne Date début ----------
        colAnneeDebut.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDateDebut() != null
                        ? data.getValue().getDateDebut().format(FORMAT_DATE) : "—"));

        // ---------- Colonne Date fin ----------
        colAnneeFin.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDateFin() != null
                        ? data.getValue().getDateFin().format(FORMAT_DATE) : "—"));

        // ---------- Colonne Statut ----------
        colAnneeStatut.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colAnneeStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(AnneeScolaire annee, boolean vide) {
                super.updateItem(annee, vide);
                if (vide || annee == null) { setGraphic(null); return; }

                HBox conteneur = new HBox(6);
                conteneur.setAlignment(Pos.CENTER_LEFT);

                // Badge "Courante"
                if (annee.isEstCourante()) {
                    Label badgeCourante = new Label("● Courante");
                    badgeCourante.getStyleClass().addAll("event-tag", "tag-green");
                    conteneur.getChildren().add(badgeCourante);
                }

                // Badge de statut
                if (annee.getStatut() != null) {
                    Label badgeStatut = new Label(annee.getStatut().getLibelle());
                    String tag = switch (annee.getStatut()) {
                        case EN_COURS    -> "tag-blue";
                        case CLOTUREE    -> "tag-orange";
                        case ARCHIVEE    -> "tag-neutral";
                        case PREPARATION -> "tag-neutral";
                    };
                    badgeStatut.getStyleClass().addAll("event-tag", tag);
                    conteneur.getChildren().add(badgeStatut);
                }
                setGraphic(conteneur);
                setText(null);
            }
        });

        // ---------- Colonne Actions : 2 boutons (comme UtilisateursView) ----------
        colAnneeActions.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colAnneeActions.setCellFactory(col -> new TableCell<>() {

            private final Button btnEditer = new Button();
            private final Button btnSupprimer = new Button();
            private final HBox conteneur = new HBox(6, btnEditer, btnSupprimer);

            {
                conteneur.setAlignment(Pos.CENTER);

                // Bouton Éditer
                FontIcon iconEdit = new FontIcon("fth-edit-2");
                iconEdit.setIconSize(14);
                btnEditer.setGraphic(iconEdit);
                btnEditer.getStyleClass().add("btn-icon-light");
                btnEditer.setTooltip(new Tooltip("Modifier l'année"));

                // Bouton Supprimer
                FontIcon iconDelete = new FontIcon("fth-trash-2");
                iconDelete.setIconSize(14);
                btnSupprimer.setGraphic(iconDelete);
                btnSupprimer.getStyleClass().addAll("btn-icon-light", "btn-danger-icon");
                btnSupprimer.setTooltip(new Tooltip("Supprimer l'année"));

                // Actions
                btnEditer.setOnAction(e -> {
                    AnneeScolaire annee = getItem();
                    if (annee != null) ouvrirModalEdition(annee);
                });

                btnSupprimer.setOnAction(e -> {
                    AnneeScolaire annee = getItem();
                    if (annee != null) demanderSuppression(annee);
                });
            }

            @Override
            protected void updateItem(AnneeScolaire annee, boolean vide) {
                super.updateItem(annee, vide);
                if (vide || annee == null) {
                    setGraphic(null);
                    return;
                }

                // Vérification des permissions
                btnEditer.setVisible(ControleAcces.autoriseAction("user.creer"));
                btnEditer.setManaged(btnEditer.isVisible());

                btnSupprimer.setVisible(ControleAcces.autoriseAction("user.creer"));
                btnSupprimer.setManaged(btnSupprimer.isVisible());

                setGraphic(conteneur);
                setText(null);
            }
        });
    }

    private void configurerRecherche() {
        RechercheUtil.configurer(
                searchAnneeField,
                clearSearchAnnee,
                () -> {
                    if (autoRefresh != null) autoRefresh.restartNow();
                }
        );
    }


    /* ============================================================
       BOUTONS
       ============================================================ */
    private void configurerBoutons() {
        btnNouvelleAnnee.setOnAction(e -> ouvrirModalCreation());
    }

    /* ============================================================
       MODALES
       ============================================================ */
    private void ouvrirModalCreation() {
        if (!ControleAcces.autoriseAction("user.creer")) {
            ToastNotification.avertissement(getStage(),
                    "Vous n'avez pas la permission de créer une année.");
            return;
        }

        String fxmlPath = "/org/erpklassup/erpklassup/view/module2/annee-modal-view.fxml";

        AnneeModalController ctrl = ModalUtil.ouvrirModal(
                getClass(), fxmlPath, "Créez une nouvelle Année scolaire",
                getStage(), 540, 510
        );

        if (ctrl != null) {
            ctrl.initCreation(idEcoleCourante, () -> {
                if (autoRefresh != null) autoRefresh.restartNow();
                ToastNotification.succes(getStage(), "Année Scolaire créée avec succès");
            });
        }
    }

    private void ouvrirModalEdition(AnneeScolaire annee) {
        if (!ControleAcces.autoriseAction("user.creer")) {
            ToastNotification.avertissement(getStage(),
                    "Vous n'avez pas la permission de modifier une année.");
            return;
        }

        String fxmlPath = "/org/erpklassup/erpklassup/view/module2/annee-modal-view.fxml";

        AnneeModalController ctrl = ModalUtil.ouvrirModal(
                getClass(), fxmlPath, "Modifiez une Année scolaire",
                getStage(), 540, 510
        );

        if (ctrl != null) {
            ctrl.initEdition(annee, () -> {
                if (autoRefresh != null) autoRefresh.restartNow();
                ToastNotification.succes(getStage(), "Année Scolaire modifiée avec succès");
            });
        }
    }

    /* ============================================================
       SUPPRESSION
       ============================================================ */
    private void demanderSuppression(AnneeScolaire annee) {
        if (!ControleAcces.autoriseAction("user.creer")) {
            ToastNotification.avertissement(getStage(),
                    "Vous n'avez pas la permission de supprimer une année.");
            return;
        }

        // Vérifier d'abord s'il y a des classes rattachées
        anneeService.compterClassesAsync(annee.getId(),
                nbClasses -> Platform.runLater(() -> {
                    if (nbClasses > 0) {
                        AlertUtil.afficherAvertissement(
                                "Suppression impossible",
                                "Cette année contient " + nbClasses
                                        + " classe(s) rattachée(s).\n"
                                        + "Supprimez-les d'abord.");
                        return;
                    }

                    Boolean confirme = AlertUtil.afficherConfirmation(
                            "Supprimer l'année ?",
                            "Voulez-vous vraiment supprimer l'année « "
                                    + annee.getLibelle() + " » ?\n"
                                    + "Cette action est irréversible.",
                            getStage()
                    );

                    if (Boolean.TRUE.equals(confirme)) {
                        executerSuppression(annee);
                    }
                }),
                err -> Platform.runLater(() ->
                        ToastNotification.erreur(getStage(),
                                "Erreur : " + err.getMessage())));
    }

    private void executerSuppression(AnneeScolaire annee) {
        anneeService.supprimerAsync(annee.getId(),
                () -> Platform.runLater(() -> {
                    auditService.tracerActionAsync("STRUCTURE", "SUPPRESSION_ANNEE",
                            "Année supprimée : " + annee.getLibelle());
                    ToastNotification.succes(getStage(),
                            "Année « " + annee.getLibelle() + " » supprimée");
                    if (autoRefresh != null) autoRefresh.restartNow();
                }),
                err -> Platform.runLater(() -> {
                    System.err.println("❌ Erreur suppression : " + err.getMessage());
                    ToastNotification.erreur(getStage(), "Erreur : " + err.getMessage());
                }));
    }

    /* ============================================================
       HELPERS
       ============================================================ */
    private Stage getStage() {
        if (rootStack == null || rootStack.getScene() == null) return null;
        return (Stage) rootStack.getScene().getWindow();
    }
}