package org.erpklassup.erpklassup.controllers.actionPage.GRH;

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
import org.erpklassup.erpklassup.dao.GRH.DepartementDAO;
import org.erpklassup.erpklassup.models.GRH.Departement;
import org.erpklassup.erpklassup.dto.FiltrePoste;
import org.erpklassup.erpklassup.dto.PosteLigne;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.GRH.PosteService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class PosteController implements Initializable, VueDisposable {

    @FXML private StackPane rootStack;
    @FXML private Label lblContextePostes;
    @FXML private TextField fieldRecherche;
    @FXML private Button btnClearSearch;
    @FXML private ComboBox<DepartementItem> filtreDepartement;
    @FXML private ComboBox<String> filtreCategorie;
    @FXML private ComboBox<String> filtreStatut;

    @FXML private Button btnNouveauPoste;

    @FXML private TableView<PosteLigne> tablePostes;
    @FXML private TableColumn<PosteLigne, String> colPosteIntitule;
    @FXML private TableColumn<PosteLigne, String> colPosteDepartement;
    @FXML private TableColumn<PosteLigne, String> colPosteCategorie;
    @FXML private TableColumn<PosteLigne, String> colPosteDescription;
    @FXML private TableColumn<PosteLigne, String> colPosteEffectif;
    @FXML private TableColumn<PosteLigne, PosteLigne> colPosteStatut;
    @FXML private TableColumn<PosteLigne, PosteLigne> colPosteActions;

    @FXML private Label lblFooterPostes;
    @FXML private Pagination paginationPostes;

    private static final int TAILLE_PAGE = 15;
    private final PosteService service = new PosteService();
    private final DepartementDAO departementDAO = new DepartementDAO();
    private final AuditService auditService = new AuditService();

    private String idEcoleCourante;
    private GestionnairePagination<PosteLigne> gestionnairePagination;
    private AutoRefreshService<List<PosteLigne>> autoRefresh;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();

        gestionnairePagination = new GestionnairePagination<>(
                tablePostes, paginationPostes, TAILLE_PAGE,
                PosteLigne::idPoste
        );
        tablePostes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        configurerTable();
        chargerFiltreDepartements();
        configurerFiltres();

        RechercheUtil.configurer(fieldRecherche, btnClearSearch, this::lancerRecherche);

        if (idEcoleCourante == null) {
            tablePostes.setPlaceholder(new Label("Aucune école sélectionnée."));
            return;
        }

        lancerRecherche();
        demarrerRafraichissementAuto();
    }

    @Override
    public void disposer() {
        if (autoRefresh != null) {
            autoRefresh.dispose();
            autoRefresh = null;
        }
    }

    private void chargerFiltreDepartements() {
        if (idEcoleCourante == null) return;
        List<Departement> depts = departementDAO.findByEcole(idEcoleCourante);
        ObservableList<DepartementItem> items = FXCollections.observableArrayList();
        items.add(new DepartementItem(null, "Tous les départements"));
        for (Departement d : depts) {
            items.add(new DepartementItem(d.getIdDepartement(), d.getLibelle()));
        }
        filtreDepartement.setItems(items);
        filtreDepartement.getSelectionModel().selectFirst();
        filtreDepartement.valueProperty().addListener((o, a, n) -> lancerRecherche());
    }

    private void demarrerRafraichissementAuto() {
        autoRefresh = new AutoRefreshService<List<PosteLigne>>(javafx.util.Duration.seconds(5))
                .loader(this::chargerPostesPourRefresh)
                .onSuccess(this::appliquerResultatRefresh)
                .onFailure(err -> System.err.println("❌ Auto-refresh postes : " + err.getMessage()))
                .start();
    }

    private List<PosteLigne> chargerPostesPourRefresh() {
        if (idEcoleCourante == null) return List.of();

        Boolean statutFiltre = null;
        String selStatut = filtreStatut.getSelectionModel().getSelectedItem();
        if ("Actif".equals(selStatut)) statutFiltre = true;
        else if ("Inactif".equals(selStatut)) statutFiltre = false;

        DepartementItem deptSel = filtreDepartement.getValue();
        String idDept = (deptSel != null) ? deptSel.id() : null;

        FiltrePoste filtre = new FiltrePoste(
                idEcoleCourante,
                fieldRecherche.getText(),
                idDept,
                filtreCategorie.getSelectionModel().getSelectedItem(),
                statutFiltre,
                1,
                Integer.MAX_VALUE
        );

        var res = service.rechercher(filtre);
        return res != null ? res.elements() : List.of();
    }

    private void appliquerResultatRefresh(List<PosteLigne> resultat) {
        if (resultat == null) return;
        ObservableList<PosteLigne> listeCourante = tablePostes.getItems();
        FusionListeUtil.fusionner(listeCourante, resultat, PosteLigne::idPoste);
        gestionnairePagination.mettreAJourDonnees(listeCourante);
        compterPostesActifs();
    }

    private void configurerTable() {
        colPosteIntitule.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().intitule()));
        colPosteIntitule.setCellFactory(col -> new TableCell<>() {
            private final FontIcon icon = new FontIcon("fth-briefcase");
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

        colPosteDepartement.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().departementLibelle() != null && !d.getValue().departementLibelle().isBlank()
                        ? d.getValue().departementLibelle() : "-"));

        colPosteCategorie.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().categorie() != null && !d.getValue().categorie().isBlank() ? d.getValue().categorie() : "-"));

        colPosteDescription.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().description() != null && !d.getValue().description().isBlank() ? d.getValue().description() : "-"));

        colPosteEffectif.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().effectifActuel())));

        colPosteStatut.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colPosteStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(PosteLigne item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                boolean actif = item.estActif();
                Label badge = new Label(actif ? "Actif" : "Inactif");
                badge.getStyleClass().addAll("event-tag", actif ? "tag-green" : "tag-red");
                setGraphic(badge);
            }
        });

        colPosteActions.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colPosteActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnVoir = new Button();
            private final Button btnEditer = new Button();
            private final Button btnSupprimer = new Button();
            private final HBox conteneur = new HBox(6, btnVoir, btnEditer, btnSupprimer);

            {
                conteneur.setAlignment(Pos.CENTER);

                FontIcon iconVoir = new FontIcon("fth-eye");
                iconVoir.setIconSize(14);
                btnVoir.setGraphic(iconVoir);
                btnVoir.getStyleClass().add("btn-icon-light");
                btnVoir.setTooltip(new Tooltip("Consulter"));

                FontIcon iconEditer = new FontIcon("fth-edit-2");
                iconEditer.setIconSize(14);
                btnEditer.setGraphic(iconEditer);
                btnEditer.getStyleClass().add("btn-icon-light");
                btnEditer.setTooltip(new Tooltip("Modifier"));

                FontIcon iconDelete = new FontIcon("fth-trash-2");
                iconDelete.setIconSize(14);
                btnSupprimer.setGraphic(iconDelete);
                btnSupprimer.getStyleClass().addAll("btn-icon-light", "btn-danger-icon", "icon-red");
                btnSupprimer.setTooltip(new Tooltip("Supprimer"));

                btnVoir.setOnAction(e -> {
                    PosteLigne ligne = getItem();
                    if (ligne != null) ouvrirModalVoir(ligne.idPoste());
                });
                btnEditer.setOnAction(e -> {
                    PosteLigne ligne = getItem();
                    if (ligne != null) ouvrirModalEdition(ligne.idPoste());
                });
                btnSupprimer.setOnAction(e -> {
                    PosteLigne ligne = getItem();
                    if (ligne != null) supprimerPoste(ligne);
                });
            }

            @Override
            protected void updateItem(PosteLigne item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else setGraphic(conteneur);
            }
        });
    }


    private void configurerFiltres() {
        ObservableList<String> categories = FXCollections.observableArrayList();
        categories.add(null);
        categories.addAll("Administration", "Enseignement", "Encadrement", "Technique", "Support");
        filtreCategorie.setItems(categories);
        filtreCategorie.getSelectionModel().selectFirst();
        filtreCategorie.valueProperty().addListener((o, a, n) -> lancerRecherche());

        ObservableList<String> statuts = FXCollections.observableArrayList("Tous", "Actif", "Inactif");
        filtreStatut.setItems(statuts);
        filtreStatut.getSelectionModel().selectFirst();
        filtreStatut.valueProperty().addListener((o, a, n) -> lancerRecherche());
    }

    @FXML
    public void onNouveauPoste() {
        ouvrirModalCreation();
    }

    private void lancerRecherche() {
        if (idEcoleCourante == null) return;

        Boolean statutFiltre = null;
        String selStatut = filtreStatut.getValue();
        if ("Actif".equals(selStatut)) statutFiltre = true;
        else if ("Inactif".equals(selStatut)) statutFiltre = false;

        DepartementItem deptSel = filtreDepartement.getValue();
        String idDept = (deptSel != null) ? deptSel.id() : null;

        FiltrePoste filtre = new FiltrePoste(
                idEcoleCourante,
                fieldRecherche.getText(),
                idDept,
                filtreCategorie.getValue(),
                statutFiltre,
                1,
                Integer.MAX_VALUE
        );

        tablePostes.setPlaceholder(new Label("Chargement des postes..."));

        service.rechercherAsync(filtre,
                resultat -> Platform.runLater(() -> {
                    gestionnairePagination.mettreAJourDonnees(resultat.elements());
                    tablePostes.setPlaceholder(new Label("Aucun poste trouvé."));
                    lblFooterPostes.setText(resultat.total() + " poste(s)");
                    compterPostesActifs();
                }),
                erreur -> Platform.runLater(() -> {
                    tablePostes.setPlaceholder(new Label("Erreur de chargement."));
                })
        );
    }

    private void compterPostesActifs() {
//        service.compterActifsAsync(idEcoleCourante,
//                nb -> Platform.runLater(() ->
//                        lblContextePostes.setText("Répertoire des postes - " + nb + " poste(s) actif(s)")
//                ),
//                err -> {}
//        );
    }

    private void ouvrirModalCreation() {
        String fxmlPath = "/org/erpklassup/erpklassup/view/GRH/PosteModal.fxml";
        PosteModalController ctrl = ModalUtil.ouvrirModal(
                getClass(), fxmlPath, "Nouveau poste",
                tablePostes.getScene().getWindow(), 560, 620
        );
        if (ctrl != null) {
            ctrl.initDataForCreate(this.idEcoleCourante, this::lancerRecherche);
        }
    }

    private void ouvrirModalEdition(String idPoste) {
        service.trouverParIdAsync(idPoste,
                opt -> Platform.runLater(() -> {
                    if (opt.isPresent()) {
                        String fxmlPath = "/org/erpklassup/erpklassup/view/GRH/PosteModal.fxml";
                        PosteModalController ctrl = ModalUtil.ouvrirModal(
                                getClass(), fxmlPath, "Modifier le poste",
                                tablePostes.getScene().getWindow(), 560, 620
                        );
                        if (ctrl != null) {
                            ctrl.initDataForUpdate(opt.get(), this::lancerRecherche);
                        }
                    }
                }),
                err -> {}
        );
    }

    private void ouvrirModalVoir(String idPoste) {
        service.trouverParIdAsync(idPoste,
                opt -> Platform.runLater(() -> {
                    if (opt.isPresent()) {
                        String fxmlPath = "/org/erpklassup/erpklassup/view/GRH/PosteModal.fxml";
                        PosteModalController ctrl = ModalUtil.ouvrirModal(
                                getClass(), fxmlPath, "Consulter le poste",
                                tablePostes.getScene().getWindow(), 560, 620
                        );
                        if (ctrl != null) {
                            ctrl.initDataVoir(opt.get(), this::lancerRecherche);
                        }
                    }
                }),
                err -> {}
        );
    }

    private void supprimerPoste(PosteLigne ligne) {
        boolean ok = AlertUtil.afficherConfirmation(
                "Suppression",
                "Êtes-vous sûr de vouloir supprimer le poste : " + ligne.intitule() + " ?",
                tablePostes.getScene().getWindow()
        );

        if (ok) {
            service.supprimerAsync(ligne.idPoste(),
                    () -> Platform.runLater(() -> {
                        auditService.tracerActionAsync("GRH", "SUPPRESSION_POSTE", "Poste supprimé : " + ligne.intitule());
                        ToastNotification.succes((Stage) tablePostes.getScene().getWindow(), "Poste supprimé avec succès.");
                        lancerRecherche();
                    }),
                    err -> Platform.runLater(() -> ToastNotification.erreur((Stage) tablePostes.getScene().getWindow(), err.getMessage()))
            );
        }
    }

    private record DepartementItem(String id, String libelle) {
        @Override
        public String toString() {
            return libelle;
        }
    }
}
//package org.erpklassup.erpklassup.controllers.actionPage.GRH;
//
//import javafx.application.Platform;
//import javafx.beans.property.SimpleObjectProperty;
//import javafx.beans.property.SimpleStringProperty;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.geometry.Pos;
//import javafx.scene.control.*;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.StackPane;
//import javafx.stage.Stage;
//import org.erpklassup.erpklassup.dto.FiltrePoste;
//import org.erpklassup.erpklassup.dto.PosteLigne;
//import org.erpklassup.erpklassup.service.AuditService;
//import org.erpklassup.erpklassup.service.GRH.PosteService;
//import org.erpklassup.erpklassup.service.SessionManager;
//import org.erpklassup.erpklassup.util.*;
//import org.kordamp.ikonli.javafx.FontIcon;
//
//import java.net.URL;
//import java.util.List;
//import java.util.ResourceBundle;
//
//@SuppressWarnings({"unused", "SpellCheckingInspection"})
//public class PosteController implements Initializable, VueDisposable {
//
//    @FXML private StackPane rootStack;
//    @FXML private Label lblContextePostes;
//    @FXML private TextField fieldRecherche;
//    @FXML private Button btnClearSearch;
//    @FXML private ComboBox<String> filtreCategorie;
//    @FXML private ComboBox<String> filtreStatut;
//
//    @FXML private Button btnNouveauPoste;
//
//    @FXML private TableView<PosteLigne> tablePostes;
//    @FXML private TableColumn<PosteLigne, String> colPosteIntitule;
//    @FXML private TableColumn<PosteLigne, String> colPosteDepartement;
//    @FXML private TableColumn<PosteLigne, String> colPosteCategorie;
//    @FXML private TableColumn<PosteLigne, String> colPosteDescription;
//    @FXML private TableColumn<PosteLigne, String> colPosteEffectif;
//    @FXML private TableColumn<PosteLigne, PosteLigne> colPosteStatut;
//    @FXML private TableColumn<PosteLigne, PosteLigne> colPosteActions;
//
//    @FXML private Label lblFooterPostes;
//    @FXML private Pagination paginationPostes;
//
//    private static final int TAILLE_PAGE = 15;
//    private final PosteService service = new PosteService();
//    private final AuditService auditService = new AuditService();
//
//    private String idEcoleCourante;
//    private GestionnairePagination<PosteLigne> gestionnairePagination;
//    private AutoRefreshService<List<PosteLigne>> autoRefresh;
//
//    @Override
//    public void initialize(URL url, ResourceBundle rb) {
//        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();
//
//        gestionnairePagination = new GestionnairePagination<>(
//                tablePostes, paginationPostes, TAILLE_PAGE,
//                PosteLigne::idPoste
//        );
//        tablePostes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
//        configurerTable();
//        configurerFiltres();
//
//        RechercheUtil.configurer(fieldRecherche, btnClearSearch, this::lancerRecherche);
//
//        if (idEcoleCourante == null) {
//            tablePostes.setPlaceholder(new Label("Aucune école sélectionnée."));
//            return;
//        }
//
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
//    }
//
//    private void demarrerRafraichissementAuto() {
//        autoRefresh = new AutoRefreshService<List<PosteLigne>>(javafx.util.Duration.seconds(5))
//                .loader(this::chargerPostesPourRefresh)
//                .onSuccess(this::appliquerResultatRefresh)
//                .onFailure(err -> System.err.println("❌ Auto-refresh postes : " + err.getMessage()))
//                .start();
//    }
//
//    private List<PosteLigne> chargerPostesPourRefresh() {
//        if (idEcoleCourante == null) return List.of();
//
//        Boolean statutFiltre = null;
//        String selStatut = filtreStatut.getSelectionModel().getSelectedItem();
//        if ("Actif".equals(selStatut)) statutFiltre = true;
//        else if ("Inactif".equals(selStatut)) statutFiltre = false;
//
//        FiltrePoste filtre = new FiltrePoste(
//                idEcoleCourante,
//                fieldRecherche.getText(),
//                filtreCategorie.getSelectionModel().getSelectedItem(),
//                statutFiltre,
//                1,
//                Integer.MAX_VALUE
//        );
//
//        var res = service.rechercher(filtre);
//        return res != null ? res.elements() : List.of();
//    }
//
//    private void appliquerResultatRefresh(List<PosteLigne> resultat) {
//        if (resultat == null) return;
//        ObservableList<PosteLigne> listeCourante = tablePostes.getItems();
//        FusionListeUtil.fusionner(listeCourante, resultat, PosteLigne::idPoste);
//        gestionnairePagination.mettreAJourDonnees(listeCourante);
//        compterPostesActifs();
//    }
//
//    private void configurerTable() {
//        // Icône fas-user-tie sur l'intitulé
//        colPosteIntitule.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().intitule()));
//        colPosteIntitule.setCellFactory(col -> new TableCell<>() {
//            private final FontIcon icon = new FontIcon("fas-user-tie");
//            private final HBox box = new HBox(8, icon, new Label());
//
//            {
//                box.setAlignment(Pos.CENTER_LEFT);
//                icon.setIconSize(13);
//                icon.getStyleClass().add("icon-orange");
//            }
//
//            @Override
//            protected void updateItem(String item, boolean empty) {
//                super.updateItem(item, empty);
//                if (empty || item == null) {
//                    setGraphic(null);
//                } else {
//                    ((Label) box.getChildren().get(1)).setText(item);
//                    setGraphic(box);
//                }
//            }
//        });
//
//        colPosteDepartement.setCellValueFactory(d -> new SimpleStringProperty(
//                d.getValue().departementLibelle() != null && !d.getValue().departementLibelle().isBlank()
//                        ? d.getValue().departementLibelle() : "—"));
//
//        colPosteCategorie.setCellValueFactory(d -> new SimpleStringProperty(
//                d.getValue().categorie() != null && !d.getValue().categorie().isBlank() ? d.getValue().categorie() : "—"));
//
//        colPosteDescription.setCellValueFactory(d -> new SimpleStringProperty(
//                d.getValue().description() != null && !d.getValue().description().isBlank() ? d.getValue().description() : "—"));
//
//        colPosteEffectif.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().effectifActuel())));
//
//        colPosteStatut.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
//        colPosteStatut.setCellFactory(col -> new TableCell<>() {
//            @Override
//            protected void updateItem(PosteLigne item, boolean empty) {
//                super.updateItem(item, empty);
//                if (empty || item == null) {
//                    setGraphic(null);
//                    return;
//                }
//                boolean actif = item.estActif();
//                Label badge = new Label(actif ? "Actif" : "Inactif");
//                badge.getStyleClass().addAll("event-tag", actif ? "tag-green" : "tag-red");
//                setGraphic(badge);
//            }
//        });
//
//        colPosteActions.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
//        colPosteActions.setCellFactory(col -> new TableCell<>() {
//            private final Button btnEditer = new Button();
//            private final Button btnSupprimer = new Button();
//            private final Button btnVoir = new Button();
//            private final HBox conteneur = new HBox(6, btnVoir, btnEditer, btnSupprimer);
//
//            {
//                conteneur.setAlignment(Pos.CENTER);
//
//                FontIcon iconEditer = new FontIcon("fth-edit-2");
//                iconEditer.setIconSize(14);
//                btnEditer.setGraphic(iconEditer);
//                btnEditer.getStyleClass().add("btn-icon-light");
//
//                FontIcon iconDelete = new FontIcon("fth-trash-2");
//                iconDelete.setIconSize(14);
//                btnSupprimer.setGraphic(iconDelete);
//                btnSupprimer.getStyleClass().addAll("btn-icon-light", "btn-danger-icon", "icon-red");
//
//                FontIcon iconVoir = new FontIcon("fth-eye");
//                iconVoir.setIconSize(14);
//                btnVoir.setGraphic(iconVoir);
//                btnVoir.getStyleClass().addAll("btn-icon-light");
//
//                btnVoir.setOnAction(e -> {
//                    PosteLigne ligne = getItem();
//                    if (ligne != null) ouvrirModalVoir(ligne.idPoste());
//                });
//                btnEditer.setOnAction(e -> {
//                    PosteLigne ligne = getItem();
//                    if (ligne != null) ouvrirModalEdition(ligne.idPoste());
//                });
//                btnSupprimer.setOnAction(e -> {
//                    PosteLigne ligne = getItem();
//                    if (ligne != null) supprimerPoste(ligne);
//                });
//            }
//
//            @Override
//            protected void updateItem(PosteLigne item, boolean empty) {
//                super.updateItem(item, empty);
//                if (empty || item == null) setGraphic(null);
//                else setGraphic(conteneur);
//            }
//        });
//    }
//
//    private void configurerFiltres() {
//        ObservableList<String> categories = FXCollections.observableArrayList();
//        categories.add(null);
//        categories.addAll("Administration", "Enseignement", "Encadrement", "Technique");
//        filtreCategorie.setItems(categories);
//        filtreCategorie.getSelectionModel().selectFirst();
//        filtreCategorie.valueProperty().addListener((o, a, n) -> lancerRecherche());
//
//        ObservableList<String> statuts = FXCollections.observableArrayList("Tous", "Actif", "Inactif");
//        filtreStatut.setItems(statuts);
//        filtreStatut.getSelectionModel().selectFirst();
//        filtreStatut.valueProperty().addListener((o, a, n) -> lancerRecherche());
//    }
//
//    @FXML
//    public void onNouveauPoste() {}
//
//    private void lancerRecherche() {
//        if (idEcoleCourante == null) return;
//
//        Boolean statutFiltre = null;
//        String selStatut = filtreStatut.getValue();
//        if ("Actif".equals(selStatut)) statutFiltre = true;
//        else if ("Inactif".equals(selStatut)) statutFiltre = false;
//
//        FiltrePoste filtre = new FiltrePoste(
//                idEcoleCourante,
//                fieldRecherche.getText(),
//                filtreCategorie.getValue(),
//                statutFiltre,
//                1,
//                Integer.MAX_VALUE
//        );
//
//        tablePostes.setPlaceholder(new Label("Chargement des postes..."));
//
//        service.rechercherAsync(filtre,
//                resultat -> Platform.runLater(() -> {
//                    gestionnairePagination.mettreAJourDonnees(resultat.elements());
//                    tablePostes.setPlaceholder(new Label("Aucun poste trouvé."));
//                    lblFooterPostes.setText(resultat.total() + " poste(s)");
//                    compterPostesActifs();
//                }),
//                erreur -> Platform.runLater(() -> {
//                    tablePostes.setPlaceholder(new Label("Erreur de chargement."));
//                })
//        );
//    }
//
//    private void compterPostesActifs() {
////        service.compterActifsAsync(idEcoleCourante,
////                nb -> Platform.runLater(() ->
////                        lblContextePostes.setText("Répertoire des postes - " + nb + " poste(s) actif(s)")
////                ),
////                err -> {}
////        );
//    }
//
//    private void ouvrirModalCreation() {}
//    private void ouvrirModalEdition(String id) {}
//    private void ouvrirModalVoir(String id) {}
//
//    private void supprimerPoste(PosteLigne ligne) {
//        boolean ok = AlertUtil.afficherConfirmation(
//                "Suppression",
//                "Êtes-vous sûr de vouloir supprimer le poste : " + ligne.intitule() + " ?",
//                tablePostes.getScene().getWindow()
//        );
//
//        if (ok) {
//            service.supprimerAsync(ligne.idPoste(),
//                    () -> Platform.runLater(() -> {
//                        auditService.tracerActionAsync("GRH", "SUPPRESSION_POSTE", "Poste supprimé : " + ligne.intitule());
//                        ToastNotification.succes((Stage) tablePostes.getScene().getWindow(), "Poste supprimé avec succès.");
//                        lancerRecherche();
//                    }),
//                    err -> Platform.runLater(() -> ToastNotification.erreur((Stage) tablePostes.getScene().getWindow(), err.getMessage()))
//            );
//        }
//    }
//}