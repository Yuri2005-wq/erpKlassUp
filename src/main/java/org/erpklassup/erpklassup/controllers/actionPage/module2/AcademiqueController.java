package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.erpklassup.erpklassup.models.module2.Filiere;
import org.erpklassup.erpklassup.models.module2.GroupeMatiere;
import org.erpklassup.erpklassup.models.module2.Niveau;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.module2.AcademiqueService;
import org.erpklassup.erpklassup.util.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur de la vue "Structure académique" :
 * gère les onglets Niveaux, Filières et Groupes de matières.
 */
public class AcademiqueController implements Initializable, VueDisposable {

    /* ============================================================
       FXML — CONTENU PRINCIPAL
       ============================================================ */
    @FXML private StackPane rootStack;
    @FXML private TextField fieldRecherche;
    @FXML private Button btnNouveau;
    @FXML private TabPane tabsStructure;

    // --- Onglet Niveaux ---
    @FXML private TableView<Niveau> tableNiveaux;
    @FXML private TableColumn<Niveau, String>  colNiveauNom;
    @FXML private TableColumn<Niveau, String>  colNiveauSysteme;
    @FXML private TableColumn<Niveau, String>  colNiveauCycle;
    @FXML private TableColumn<Niveau, Integer> colNiveauOrdre;
    @FXML private TableColumn<Niveau, Niveau>  colNiveauActions;
    @FXML private Label lblFooterNiveaux;
    @FXML private Pagination paginationNiveaux;

    // --- Onglet Filières ---
    @FXML private TableView<Filiere> tableFilieres;
    @FXML private TableColumn<Filiere, String> colFiliereCode;
    @FXML private TableColumn<Filiere, String> colFiliereNom;
    @FXML private TableColumn<Filiere, Filiere> colFiliereActions;
    @FXML private Label lblFooterFilieres;
    @FXML private Pagination paginationFilieres;

    // --- Onglet Groupes de Matières ---
    @FXML private TableView<GroupeMatiere> tableGroupes;
    @FXML private TableColumn<GroupeMatiere, String>  colGroupeCode;
    @FXML private TableColumn<GroupeMatiere, String>  colGroupeNom;
    @FXML private TableColumn<GroupeMatiere, Integer> colGroupeOrdre;
    @FXML private TableColumn<GroupeMatiere, GroupeMatiere> colGroupeActions;
    @FXML private Label lblFooterGroupes;
    @FXML private Pagination paginationGroupes;

    /* ============================================================
       CONSTANTES & SERVICES
       ============================================================ */
    private static final int TAILLE_PAGE = 15;
    private static final Duration PERIODE_REFRESH = Duration.seconds(10);

    private final AcademiqueService service = new AcademiqueService();

    private final PauseTransition debounceRecherche = new PauseTransition(Duration.millis(350));
    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;

    private String idEcoleCourante;

    /* ============================================================
       PAGINATION
       ============================================================ */
    private GestionnairePagination<Niveau>         paginationNiveauxGeree;
    private GestionnairePagination<Filiere>        paginationFilieresGeree;
    private GestionnairePagination<GroupeMatiere>  paginationGroupesGeree;

    private AutoRefreshService<Void> autoRefresh;

    /* ============================================================
       INITIALISATION
       ============================================================ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();

        paginationNiveauxGeree  = new GestionnairePagination<>(tableNiveaux,  paginationNiveaux,  TAILLE_PAGE);
        paginationFilieresGeree = new GestionnairePagination<>(tableFilieres, paginationFilieres, TAILLE_PAGE);
        paginationGroupesGeree  = new GestionnairePagination<>(tableGroupes,  paginationGroupes,  TAILLE_PAGE);

        configurerTables();
        configurerRecherche();
        configurerChangementsOnglet();
        configurerBoutonNouveau();

        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
        appliquerControlesAcces();

        if (idEcoleCourante == null) {
            tableNiveaux.setPlaceholder(new Label("Aucune école sélectionnée"));
            return;
        }

        tableNiveaux.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableFilieres.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableGroupes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        chargerOngletActif();
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
       AUTO-REFRESH
       ============================================================ */
    private void demarrerRafraichissementAuto() {
        autoRefresh = new AutoRefreshService<Void>(PERIODE_REFRESH)
                .loader(() -> {
                    Platform.runLater(this::chargerOngletActif);
                    return null;
                })
                .start();
    }

    /* ============================================================
       CONTRÔLES D'ACCÈS
       ============================================================ */
    private void appliquerControlesAcces() {
//        ControleAcces.appliquerAction(btnNouveau, "structure.creer");
        tableNiveaux.refresh();
        tableFilieres.refresh();
        tableGroupes.refresh();
    }

    /* ============================================================
       CONFIGURATION DES TABLES
       ============================================================ */
    private void configurerTables() {

        // ----- Onglet Niveaux -----
        colNiveauNom    .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNomNiveau()));
        colNiveauSysteme.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSysteme()));
        colNiveauCycle  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCycle()));
        colNiveauOrdre  .setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getOrdre()).asObject());
        colNiveauActions.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colNiveauActions.setCellFactory(c -> creerCelluleActions(this::ouvrirEditionNiveau, this::supprimerNiveau));

        // ----- Onglet Filières -----
        colFiliereCode   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCodeFiliere()));
        colFiliereNom    .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNomFiliere()));
        colFiliereActions.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colFiliereActions.setCellFactory(c -> creerCelluleActions(this::ouvrirEditionFiliere, this::supprimerFiliere));

        // ----- Onglet Groupes -----
        colGroupeCode   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCodeGroupe()));
        colGroupeNom    .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNomGroupe()));
        colGroupeOrdre  .setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getOrdreAffichage()).asObject());
        colGroupeActions.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colGroupeActions.setCellFactory(c -> creerCelluleActions(this::ouvrirEditionGroupe, this::supprimerGroupe));
    }

    /**
     * Cellule générique avec 2 boutons : Voir (œil) + Supprimer (poubelle).
     */
    private <T> TableCell<T, T> creerCelluleActions(
            java.util.function.Consumer<T> onView,
            java.util.function.Consumer<T> onDelete) {

        return new TableCell<>() {
            private final Button btnView   = new Button();
            private final Button btnDelete = new Button();
            private final HBox   conteneur = new HBox(6, btnView, btnDelete);

            {
                conteneur.setAlignment(Pos.CENTER);

                FontIcon iconView = new FontIcon("fth-eye");
                iconView.setIconSize(14);
                btnView.setGraphic(iconView);
                btnView.getStyleClass().add("btn-icon-light");
                btnView.setTooltip(new Tooltip("Modifier"));

                FontIcon iconDelete = new FontIcon("fth-trash-2");
                iconDelete.setIconSize(14);
                btnDelete.setGraphic(iconDelete);
                btnDelete.getStyleClass().addAll("btn-icon-light", "btn-danger-icon");
                btnDelete.setTooltip(new Tooltip("Supprimer"));

                btnView.setOnAction(e -> {
                    T item = getItem();
                    if (item != null) onView.accept(item);
                });
                btnDelete.setOnAction(e -> {
                    T item = getItem();
                    if (item != null) onDelete.accept(item);
                });
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    boolean peutVoir    = ControleAcces.autoriseAction("user.creer")
                            || ControleAcces.autoriseAction("user.creer");
                    boolean peutSupprimer = ControleAcces.autoriseAction("user.creer");

                    btnView.setVisible(peutVoir);
                    btnView.setManaged(peutVoir);
                    btnDelete.setVisible(peutSupprimer);
                    btnDelete.setManaged(peutSupprimer);

                    setGraphic(conteneur);
                }
            }
        };
    }

    /* ============================================================
       RECHERCHE & ONGLETS
       ============================================================ */
    private void configurerRecherche() {
        debounceRecherche.setOnFinished(e -> chargerOngletActif());
        fieldRecherche.textProperty().addListener((obs, o, n) -> debounceRecherche.playFromStart());
    }

    private void configurerChangementsOnglet() {
        tabsStructure.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
            fieldRecherche.clear();
            chargerOngletActif();
        });
    }

    private void configurerBoutonNouveau() {
        btnNouveau.setOnAction(this::onNouveau);
    }

    private void chargerOngletActif() {
        int index = tabsStructure.getSelectionModel().getSelectedIndex();
        switch (index) {
            case 0 -> chargerNiveaux();
            case 1 -> chargerFilieres();
            case 2 -> chargerGroupes();
        }
    }

    /* ============================================================
       CHARGEMENTS ASYNC
       ============================================================ */
    private void chargerNiveaux() {
        String terme = fieldRecherche.getText();
        service.rechercherNiveauAsync(idEcoleCourante, terme,
                this::appliquerNiveaux,
                this::afficherErreur);
    }

    private void appliquerNiveaux(List<Niveau> niveaux) {
        Platform.runLater(() -> {
            paginationNiveauxGeree.mettreAJourDonnees(niveaux);
            lblFooterNiveaux.setText(niveaux.size() + " niveau(x) au total");
        });
    }

    private void chargerFilieres() {
        String terme = fieldRecherche.getText();
        service.rechercherFiliereAsync(idEcoleCourante, terme,
                this::appliquerFilieres,
                this::afficherErreur);
    }

    private void appliquerFilieres(List<Filiere> filieres) {
        Platform.runLater(() -> {
            paginationFilieresGeree.mettreAJourDonnees(filieres);
            lblFooterFilieres.setText(filieres.size() + " filière(s) au total");
        });
    }

    private void chargerGroupes() {
        String terme = fieldRecherche.getText();
        service.rechercherGroupeAsync(idEcoleCourante, terme,
                this::appliquerGroupes,
                this::afficherErreur);
    }

    private void appliquerGroupes(List<GroupeMatiere> groupes) {
        Platform.runLater(() -> {
            paginationGroupesGeree.mettreAJourDonnees(groupes);
            lblFooterGroupes.setText(groupes.size() + " groupe(s) au total");
        });
    }

    /* ============================================================
       ACTIONS — NOUVEAU
       ============================================================ */
    @FXML
    private void onNouveau(ActionEvent event) {
        int index = tabsStructure.getSelectionModel().getSelectedIndex();
        switch (index) {
            case 0 -> ouvrirEditionNiveau(null);
            case 1 -> ouvrirEditionFiliere(null);
            case 2 -> ouvrirEditionGroupe(null);
        }
    }

    /* ============================================================
       OUVERTURE DES MODALES
       ============================================================ */
    private void ouvrirEditionNiveau(Niveau niveau) {
//        if (!ControleAcces.autoriseAction(
//                niveau == null ? "academique.niveau.creer" : "academique.niveau.modifier")) {
//            ToastNotification.avertissement(getStage(), "Permission refusée");
//            return;
//        }

        String fxml = "/org/erpklassup/erpklassup/view/module2/ModalNiveau.fxml";
        NiveauFormController ctrl = ModalUtil.ouvrirModal(
                getClass(), fxml, "Niveau", getStage(), 480, 520);

        if (ctrl != null) {
            ctrl.setNiveau(niveau);
            ctrl.setOnSuccess(this::chargerOngletActif);
        }
    }

    private void ouvrirEditionFiliere(Filiere filiere) {
//        if (!ControleAcces.autoriseAction(
//                filiere == null ? "academique.filiere.creer" : "academique.filiere.modifier")) {
//            ToastNotification.avertissement(getStage(), "Permission refusée");
//            return;
//        }

        String fxml = "/org/erpklassup/erpklassup/view/Module2/modalFiliere.fxml";
        FiliereFormController ctrl = ModalUtil.ouvrirModal(
                getClass(), fxml, "Filière", getStage(), 480, 420);

        if (ctrl != null) {
            ctrl.setFiliere(filiere);
            ctrl.setOnSuccess(this::chargerOngletActif);
        }
    }

    private void ouvrirEditionGroupe(GroupeMatiere groupe) {
//        if (!ControleAcces.autoriseAction(
//                groupe == null ? "academique.groupematiere.creer" : "academique.groupematiere.modifier")) {
//            ToastNotification.avertissement(getStage(), "Permission refusée");
//            return;
//        }

        String fxml = "/org/erpklassup/erpklassup/view/module2/ModalGroupe.fxml";
        GroupeFormController ctrl = ModalUtil.ouvrirModal(
                getClass(), fxml, "Groupe", getStage(), 480, 460);

        if (ctrl != null) {
            ctrl.setGroupe(groupe);
            ctrl.setOnSuccess(this::chargerOngletActif);
        }
    }

    /* ============================================================
       SUPPRESSIONS
       ============================================================ */
    private void supprimerNiveau(Niveau niveau) {
//        if (!ControleAcces.autoriseAction("academique.niveau.supprimer")) {
//            ToastNotification.avertissement(getStage(), "Permission refusée");
//            return;
//        }

        boolean ok = AlertUtil.afficherConfirmation(
                "Confirmation de suppression",
                "Voulez-vous vraiment supprimer le niveau « " + niveau.getNomNiveau() + " » ?",
                getStage());

        if (ok) {
            service.supprimerNiveauAsync(niveau.getId(),
                    () -> Platform.runLater(this::chargerOngletActif),
                    err -> Platform.runLater(() -> afficherErreur(err)));
        }
    }

    private void supprimerFiliere(Filiere filiere) {
//        if (!ControleAcces.autoriseAction("academique.filiere.supprimer")) {
//            ToastNotification.avertissement(getStage(), "Permission refusée");
//            return;
//        }

        boolean ok = AlertUtil.afficherConfirmation(
                "Confirmation de suppression",
                "Voulez-vous vraiment supprimer la filière « " + filiere.getNomFiliere() + " » ?",
                getStage());

        if (ok) {
            service.supprimerFiliereAsync(filiere.getId(),
                    () -> Platform.runLater(this::chargerOngletActif),
                    err -> Platform.runLater(() -> afficherErreur(err)));
        }
    }

    private void supprimerGroupe(GroupeMatiere groupe) {
//        if (!ControleAcces.autoriseAction("academique.groupematiere.supprimer")) {
//            ToastNotification.avertissement(getStage(), "Permission refusée");
//            return;
//        }

        boolean ok = AlertUtil.afficherConfirmation(
                "Confirmation de suppression",
                "Voulez-vous vraiment supprimer le groupe « " + groupe.getNomGroupe() + " » ?",
                getStage());

        if (ok) {
            service.supprimerGroupeMatiereAsync(groupe.getId(),
                    () -> Platform.runLater(this::chargerOngletActif),
                    err -> Platform.runLater(() -> afficherErreur(err)));
        }
    }

    /* ============================================================
       HELPERS
       ============================================================ */
    private void afficherErreur(Throwable err) {
        String msg = (err != null && err.getMessage() != null)
                ? err.getMessage()
                : "Une erreur est survenue.";
        ToastNotification.erreur(getStage(), msg);
    }

    private Stage getStage() {
        if (rootStack == null || rootStack.getScene() == null) return null;
        return (Stage) rootStack.getScene().getWindow();
    }
}