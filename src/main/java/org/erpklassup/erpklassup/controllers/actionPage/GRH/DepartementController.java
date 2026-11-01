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
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.erpklassup.erpklassup.dto.DepartementLigne;
import org.erpklassup.erpklassup.dto.FiltreDepartement;
import org.erpklassup.erpklassup.dto.ResultatPagine;
import org.erpklassup.erpklassup.enums.StatutDepartement;
import org.erpklassup.erpklassup.enums.TypeDepartement;
import org.erpklassup.erpklassup.models.GRH.Departement;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.GRH.DepartementService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DepartementController implements Initializable, VueDisposable {

    /* ============================================================
       CHAMPS FXML
       ============================================================ */
    @FXML
    private StackPane rootStack;
    @FXML
    private Label lblContexteDepartements;
    @FXML
    private TextField fieldRecherche;
    @FXML
    private ComboBox<TypeDepartement> filtreType;
    @FXML
    private ComboBox<String> filtreResponsable;
    @FXML
    private ComboBox<StatutDepartement> filtreStatut;

    @FXML
    private Button btnVueHierarchique;
    @FXML
    private Button btnReaffecterResponsables;
    @FXML
    private Button btnNouveauDepartement;
    @FXML
    private Button btnClearSearch;

    @FXML
    private TableView<DepartementLigne> tableDepartements;
    @FXML
    private TableColumn<DepartementLigne, String> colDeptLibelle;
    @FXML
    private TableColumn<DepartementLigne, String> colDeptParent;
    @FXML
    private TableColumn<DepartementLigne, TypeDepartement> colDeptType;
    @FXML
    private TableColumn<DepartementLigne, String> colDeptResponsable;
    @FXML
    private TableColumn<DepartementLigne, String> colDeptEffectif;
    @FXML
    private TableColumn<DepartementLigne, DepartementLigne> colDeptStatut;
    @FXML
    private TableColumn<DepartementLigne, DepartementLigne> colDeptActions;

    @FXML
    private Label lblFooterDepartements;
    @FXML
    private Pagination paginationDepartements;

    /* ============================================================
       CONSTANTES & SERVICES
       ============================================================ */
    private static final int TAILLE_PAGE = 15;
    private static final Duration PERIODE_REFRESH = Duration.seconds(5);

    private final DepartementService service = new DepartementService();
    private final AuditService auditService = new AuditService();

    private final PauseTransition debounceRecherche = new PauseTransition(Duration.millis(350));
    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;

    private String idEcoleCourante;
    private GestionnairePagination<DepartementLigne> gestionnairePagination;
    private AutoRefreshService<List<DepartementLigne>> autoRefresh;

    /* ============================================================
       INITIALISATION
       ============================================================ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();

        gestionnairePagination = new GestionnairePagination<>(
                tableDepartements, paginationDepartements, TAILLE_PAGE,
                DepartementLigne::idDepartement
        );
        tableDepartements.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        configurerTable();
        configurerFiltres();
        configurerRecherche();
        configurerActionsBarre();

        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
        appliquerControlesAcces();

        if (idEcoleCourante == null) {
            tableDepartements.setPlaceholder(new Label("Aucune école sélectionnée."));
            return;
        }

        lancerRecherche();
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
    }

    /* ============================================================
       AUTO-REFRESH TEMPS RÉEL
       ============================================================ */
    private void demarrerRafraichissementAuto() {
        autoRefresh = new AutoRefreshService<List<DepartementLigne>>(PERIODE_REFRESH)
                .loader(this::chargerDepartementsPourRefresh)
                .onSuccess(this::appliquerResultatRefresh)
                .onFailure(err -> System.err.println("❌ Auto-refresh départements : " + err.getMessage()))
                .start();
    }

    private List<DepartementLigne> chargerDepartementsPourRefresh() {
        if (idEcoleCourante == null) return List.of();

        FiltreDepartement filtre = new FiltreDepartement(
                idEcoleCourante,
                fieldRecherche.getText(),
                filtreType.getSelectionModel().getSelectedItem(),
                null,
                filtreStatut.getSelectionModel().getSelectedItem(),
                1,
                Integer.MAX_VALUE
        );

        ResultatPagine<DepartementLigne> res = service.rechercher(filtre);
        return res != null ? res.elements() : List.of();
    }

    private void appliquerResultatRefresh(List<DepartementLigne> resultat) {
        if (resultat == null) return;

        ObservableList<DepartementLigne> listeCourante = tableDepartements.getItems();
        FusionListeUtil.fusionner(listeCourante, resultat, DepartementLigne::idDepartement);
        gestionnairePagination.mettreAJourDonnees(listeCourante);

        compterDepartementsActifs();
    }

    /* ============================================================
       CONTRÔLES D'ACCÈS
       ============================================================ */
    private void appliquerControlesAcces() {
//        ControleAcces.appliquerAction(btnNouveauDepartement, "grh.departement.creer");
//        ControleAcces.appliquerAction(btnReaffecterResponsables, "grh.departement.reassigner_responsables");
        tableDepartements.refresh();
    }

    /* ============================================================
       CONFIGURATION DE LA TABLE
       ============================================================ */
    private void configurerTable() {
        colDeptLibelle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().libelle()));
        colDeptLibelle.setCellFactory(col -> new TableCell<>() {
            private final FontIcon icon = new FontIcon("fas-sitemap");
            private final HBox box = new HBox(8, icon, new Label());

            {
                box.setAlignment(Pos.CENTER_LEFT);
                icon.setIconSize(13);
                icon.getStyleClass().add("icon-orange");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    ((Label) box.getChildren().get(1)).setText(item);
                    setGraphic(box);
                }
            }
        });


        colDeptParent.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().parentLibelle() != null ? d.getValue().parentLibelle() : "-"));

        colDeptType.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().type()));
        colDeptResponsable.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().responsableNomComplet().isBlank() ? "Non assigné" : d.getValue().responsableNomComplet()));

        colDeptEffectif.setCellValueFactory(d -> {
            DepartementLigne l = d.getValue();
            String txt = l.effectifActuel() + (l.effectifMax() != null ? " / " + l.effectifMax() : "");
            return new SimpleStringProperty(txt);
        });

        // Colonne STATUT
        colDeptStatut.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colDeptStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(DepartementLigne item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.statut() == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(item.statut().getLibelle());
                badge.getStyleClass().addAll("event-tag", switch (item.statut()) {
                    case ACTIF -> "tag-green";
                    case PLANIFIE -> "tag-blue";
                    case INACTIF -> "tag-orange";
                    case ARCHIVE -> "tag-red";
                });
                setGraphic(badge);
            }
        });

        // Colonne ACTIONS
        colDeptActions.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colDeptActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditer = new Button();
            private final Button btnSupprimer = new Button();
            private final Button btnVoir = new Button();

            private final HBox conteneur = new HBox(6, btnVoir, btnEditer, btnSupprimer);

            {
                conteneur.setAlignment(Pos.CENTER);

                FontIcon iconEditer = new FontIcon("fth-edit-2");
                iconEditer.setIconSize(14);
                btnEditer.setGraphic(iconEditer);
                btnEditer.getStyleClass().add("btn-icon-light");
                btnEditer.setTooltip(new Tooltip("Modifier le département"));

                FontIcon iconDelete = new FontIcon("fth-trash-2");
                iconDelete.setIconSize(14);
                btnSupprimer.setGraphic(iconDelete);
                btnSupprimer.getStyleClass().addAll("btn-icon-light", "btn-danger-icon", "icon-red");
                btnSupprimer.setTooltip(new Tooltip("Supprimer le département"));

                FontIcon iconVoir = new FontIcon("fth-eye");
                iconDelete.setIconSize(14);
                btnVoir.setGraphic(iconVoir);
                btnVoir.getStyleClass().addAll("btn-icon-light");
                btnVoir.setTooltip(new Tooltip("plus de détail"));

                btnVoir.setOnAction(e -> {
                    DepartementLigne ligne = getItem();
                    if (ligne != null) ouvrirModalVoir(ligne.idDepartement());
                });

                btnEditer.setOnAction(e -> {
                    DepartementLigne ligne = getItem();
                    if (ligne != null) ouvrirModalEdition(ligne.idDepartement());
                });

                btnSupprimer.setOnAction(e -> {
                    DepartementLigne ligne = getItem();
                    if (ligne != null) supprimerDepartement(ligne);
                });
            }

            @Override
            protected void updateItem(DepartementLigne item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
//                    btnEditer.setVisible(ControleAcces.autoriseAction("grh.departement.modifier"));
//                    btnEditer.setManaged(btnEditer.isVisible());

//                    btnSupprimer.setVisible(ControleAcces.autoriseAction("grh.departement.supprimer"));
//                    btnSupprimer.setManaged(btnSupprimer.isVisible());

                    setGraphic(conteneur);
                }
            }
        });
    }

    /* ============================================================
       CONFIGURATION DES FILTRES & RECHERCHE
       ============================================================ */
    private void configurerFiltres() {
        ObservableList<TypeDepartement> types = FXCollections.observableArrayList();
        types.add(null);
        types.addAll(TypeDepartement.values());
        filtreType.setItems(types);
        filtreType.getSelectionModel().selectFirst();
        filtreType.valueProperty().addListener((o, a, n) -> lancerRecherche());

        ObservableList<StatutDepartement> statuts = FXCollections.observableArrayList();
        statuts.add(null);
        statuts.addAll(StatutDepartement.values());
        filtreStatut.setItems(statuts);
        filtreStatut.getSelectionModel().selectFirst();
        filtreStatut.valueProperty().addListener((o, a, n) -> lancerRecherche());
    }

    private void configurerRecherche() {
        debounceRecherche.setOnFinished(e -> lancerRecherche());

        fieldRecherche.textProperty().addListener((obs, ancien, nouveau) -> {
            boolean aDuTexte = nouveau != null && !nouveau.trim().isEmpty();

            if (btnClearSearch != null) {
                btnClearSearch.setVisible(aDuTexte);
            }

            debounceRecherche.playFromStart();
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnAction(e -> {
                fieldRecherche.clear();
                fieldRecherche.requestFocus();
            });
        }


    }

    /* ============================================================
       BOUTONS ET ACTIONS BARRE
       ============================================================ */
    private void configurerActionsBarre() {
        btnVueHierarchique.setOnAction(e -> afficherVueArborescente());
        btnReaffecterResponsables.setOnAction(e -> ouvrirDialogueReaffectation());
    }

    @FXML
    public void onNouveauDepartement() {
        ouvrirModalCreation();
    }

    /* ============================================================
       RECHERCHE & CHARGEMENT
       ============================================================ */
    private void lancerRecherche() {
        if (idEcoleCourante == null) return;

        FiltreDepartement filtre = new FiltreDepartement(
                idEcoleCourante,
                fieldRecherche.getText(),
                filtreType.getValue(),
                null,
                filtreStatut.getValue(),
                1,
                Integer.MAX_VALUE
        );

        tableDepartements.setPlaceholder(new Label("Chargement des départements..."));

        service.rechercherAsync(filtre,
                resultat -> Platform.runLater(() -> {
                    gestionnairePagination.mettreAJourDonnees(resultat.elements());
                    tableDepartements.setPlaceholder(new Label("Aucun département trouvé."));
                    lblFooterDepartements.setText(resultat.total() + " département(s)");
                    compterDepartementsActifs();
                }),
                erreur -> Platform.runLater(() -> {
                    tableDepartements.setPlaceholder(new Label("Erreur de chargement."));
                    AlertUtil.afficherErreur("Erreur", erreur.getMessage());
                })
        );
    }

    private void compterDepartementsActifs() {
//        service.compterActifsAsync(idEcoleCourante,
//                nb -> Platform.runLater(() ->
//                        lblContexteDepartements.setText("Structure organisationnelle - " + nb + " département(s) actif(s)")
//                ),
//                err -> {
//                }
//        );
    }

    /* ============================================================
       GESTION DES MODALES
       ============================================================ */
    private void ouvrirModalCreation() {
        String fxmlPath = "/org/erpklassup/erpklassup/view/GRH/DepartementModal.fxml";
        DepartementModalController ctrl = ModalUtil.ouvrirModal(
                getClass(), fxmlPath, "Nouveau département",
                tableDepartements.getScene().getWindow(), 560, 705
        );

        if (ctrl != null) {
            ctrl.initDataForCreate(this.idEcoleCourante, this::lancerRecherche);
        }
    }

    private void ouvrirModalEdition(String idDepartement) {
        service.trouverParIdAsync(idDepartement,
                opt -> Platform.runLater(() -> {
                    if (opt.isPresent()) {
                        String fxmlPath = "/org/erpklassup/erpklassup/view/GRH/DepartementModal.fxml";
                        DepartementModalController ctrl = ModalUtil.ouvrirModal(
                                getClass(), fxmlPath, "Modifier le département",
                                tableDepartements.getScene().getWindow(), 560, 705
                        );
                        if (ctrl != null) {
                            ctrl.initDataForUpdate(opt.get(), this::lancerRecherche);
                        }
                    } else {
                        AlertUtil.afficherErreur("Erreur", "Département introuvable.");
                    }
                }),
                err -> Platform.runLater(() -> AlertUtil.afficherErreur("Erreur", err.getMessage()))
        );
    }


    private void ouvrirModalVoir(String idDepartement) {
        service.trouverParIdAsync(idDepartement,
                opt -> Platform.runLater(() -> {
                    if (opt.isPresent()) {
                        String fxmlPath = "/org/erpklassup/erpklassup/view/GRH/DepartementModal.fxml";
                        DepartementModalController ctrl = ModalUtil.ouvrirModal(
                                getClass(), fxmlPath, "Consulter le département",
                                tableDepartements.getScene().getWindow(), 560, 690
                        );
                        if (ctrl != null) {
                            ctrl.initDataVoir(opt.get(), this::lancerRecherche);
                        }
                    } else {
                        AlertUtil.afficherErreur("Erreur", "Département introuvable.");
                    }
                }),
                err -> Platform.runLater(() -> AlertUtil.afficherErreur("Erreur", err.getMessage()))
        );
    }


    private void supprimerDepartement(DepartementLigne ligne) {
        boolean ok = AlertUtil.afficherConfirmation(
                "Suppression",
                "Êtes-vous sûr de vouloir supprimer le département : " + ligne.libelle() + " ?",
                tableDepartements.getScene().getWindow()
        );

        if (ok) {
            service.supprimerAsync(ligne.idDepartement(),
                    () -> Platform.runLater(() -> {
                        auditService.tracerActionAsync("GRH", "SUPPRESSION_DEPARTEMENT", "Département supprimé : " + ligne.libelle());
                        ToastNotification.succes((Stage) tableDepartements.getScene().getWindow(), "Département supprimé avec succès.");
                        lancerRecherche();
                    }),
                    err -> Platform.runLater(() -> ToastNotification.erreur((Stage) tableDepartements.getScene().getWindow(), err.getMessage()))
            );
        }
    }

    private void afficherVueArborescente() {
        String fxmlPath = "/org/erpklassup/erpklassup/view/GRH/DepartementHierarchieModal.fxml";
        DepartementHierarchieModalController ctrl = ModalUtil.ouvrirModal(
                getClass(), fxmlPath, "Organigramme des départements",
                tableDepartements.getScene().getWindow(), 597, 600
        );

        if (ctrl != null) {
            ctrl.initData(this.idEcoleCourante);
        }
    }

    private void ouvrirDialogueReaffectation() {
        String fxmlPath = "/org/erpklassup/erpklassup/view/GRH/reafectations.fxml";

        // Ouvre la modale via l'utilitaire standard du projet
        ReaffectationResponsablesModalController ctrl = ModalUtil.ouvrirModal(
                getClass(), fxmlPath, "Réaffectation & Supervision des Responsables",
                tableDepartements.getScene().getWindow(), 720, 600
        );

        // Initialise les données et passe le callback pour rafraîchir la liste à la fermeture
        if (ctrl != null) {
            ctrl.initData(this.idEcoleCourante, this::lancerRecherche);
        }
    }
}
