package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import org.erpklassup.erpklassup.dto.AuditLigne;
import org.erpklassup.erpklassup.dto.FiltreAudit;
import org.erpklassup.erpklassup.service.AppExecutor;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.ControleAcces;
import org.erpklassup.erpklassup.util.ModalUtil;
import org.erpklassup.erpklassup.util.VueDisposable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class JournalAuditTabController implements Initializable, VueDisposable {

    private static final String OPTION_TOUS = "Tous les audits";

    @FXML private TextField searchAudit;
    @FXML private DatePicker datePickerDebut;
    @FXML private DatePicker datePickerFin;
    @FXML private ComboBox<String> filterTypeAction;
    @FXML private Button btnExporterAudit;
    @FXML private Button btnClearSearch;

    @FXML private Label statTotalLabel;
    @FXML private Label statConnectesLabel;
    @FXML private Label statSuspendusLabel;

    @FXML private TableView<AuditLigne> tableAudit;
    @FXML private TableColumn<AuditLigne, String> colHorodatage;
    @FXML private TableColumn<AuditLigne, String> colAuteur;
    @FXML private TableColumn<AuditLigne, String> colAction;
    @FXML private TableColumn<AuditLigne, String> colAdresseIp;
    @FXML private TableColumn<AuditLigne, String> colAppareil;

    @FXML private Pagination paginationAudit;

    private static final int ELEMENTS_PAR_PAGE = 20;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AuditService service = new AuditService();
    private final PauseTransition debounceRecherche = new PauseTransition(Duration.millis(350));
    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;

    private String idEcoleCourante;
    private List<AuditLigne> dernierResultat = List.of();

    private ScheduledService<List<AuditLigne>> serviceRafraichissementAuto;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();

        configurerTable();
        configurerFiltres();
        configurerRecherche();
        configurerPagination();
        configurerDoubleClic();

        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
        appliquerControlesAcces();

        if (idEcoleCourante == null) {
            tableAudit.setPlaceholder(new Label("Aucune école active — connectez-vous d'abord."));
            return;
        }

        chargerCategories();
        initialiserServiceRafraichissementAuto();
    }

    @Override
    public void disposer() {
        if (serviceRafraichissementAuto != null && serviceRafraichissementAuto.isRunning()) {
            serviceRafraichissementAuto.cancel();
        }
        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
    }

    private void initialiserServiceRafraichissementAuto() {
        serviceRafraichissementAuto = new ScheduledService<>() {
            @Override
            protected Task<List<AuditLigne>> createTask() {
                return new Task<>() {
                    @Override
                    protected List<AuditLigne> call() {
                        if (idEcoleCourante == null) return List.of();

                        // Récupération du type sélectionné
                        String selection = filterTypeAction.getSelectionModel().getSelectedItem();

                        // Si "Tous les audits" ou rien n'est sélectionné, transmettre null
                        String typeActionFiltre = (selection == null || OPTION_TOUS.equals(selection))
                                ? null
                                : selection;

                        FiltreAudit filtre = new FiltreAudit(
                                idEcoleCourante,
                                searchAudit.getText(),
                                datePickerDebut.getValue(),
                                datePickerFin.getValue(),
                                typeActionFiltre
                        );

                        return service.rechercher(filtre);
                    }
                };
            }
        };

        serviceRafraichissementAuto.setPeriod(Duration.seconds(3));

        serviceRafraichissementAuto.setOnSucceeded(event -> {
            List<AuditLigne> resultat = serviceRafraichissementAuto.getValue();
            if (resultat != null) {
                this.dernierResultat = resultat;
                mettreAJourStatistiques(resultat);

                int pageIndexActuel = paginationAudit.getCurrentPageIndex();
                int nombrePages = Math.max(1, (int) Math.ceil((double) resultat.size() / ELEMENTS_PAR_PAGE));

                paginationAudit.setPageCount(nombrePages);

                if (pageIndexActuel >= nombrePages) {
                    paginationAudit.setCurrentPageIndex(0);
                    mettreAJourPageTable(0);
                } else {
                    mettreAJourPageTable(pageIndexActuel);
                }

                tableAudit.setPlaceholder(new Label("Aucun enregistrement dans le journal d'audit"));
            }
        });

        serviceRafraichissementAuto.setOnFailed(event -> {
            tableAudit.setPlaceholder(new Label("Erreur de chargement du journal"));
            Throwable err = serviceRafraichissementAuto.getException();
            if (err != null) err.printStackTrace();
        });

        tableAudit.setPlaceholder(new Label("Chargement..."));
        serviceRafraichissementAuto.start();
    }

    private void relancerServiceInmediatement() {
        if (serviceRafraichissementAuto != null && serviceRafraichissementAuto.isRunning()) {
            serviceRafraichissementAuto.restart();
        }
    }

    private void appliquerControlesAcces() {
        ControleAcces.appliquerAction(btnExporterAudit, "user.modifier");
    }

    private void configurerTable() {
        tableAudit.setItems(FXCollections.observableArrayList());

        colHorodatage.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().horodatage().format(FORMAT_DATE)));
        colAuteur.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().auteur()));
        colAction.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().action()));
        colAdresseIp.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().adresseIp() != null ? data.getValue().adresseIp() : "—"));
        colAppareil.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().appareil() != null ? data.getValue().appareil() : "—"));
    }

    private void configurerFiltres() {
        datePickerDebut.valueProperty().addListener((obs, a, n) -> relancerServiceInmediatement());
        datePickerFin.valueProperty().addListener((obs, a, n) -> relancerServiceInmediatement());
        filterTypeAction.getSelectionModel().selectedItemProperty().addListener((obs, a, n) -> relancerServiceInmediatement());
        btnExporterAudit.setOnAction(e -> exporterVersCsv());
    }

    private void configurerRecherche() {
        debounceRecherche.setOnFinished(e -> relancerServiceInmediatement());
        searchAudit.textProperty().addListener((obs, ancien, nouveau) -> {
            boolean aDutexte = nouveau != null && !nouveau.trim().isEmpty();
            debounceRecherche.playFromStart();

            if (btnClearSearch != null) {
                btnClearSearch.setVisible(aDutexte);
            }
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnAction(e -> {
                searchAudit.clear();
                searchAudit.requestFocus();
            });
        }
    }

    private void configurerPagination() {
        paginationAudit.currentPageIndexProperty().addListener((obs, anciennePage, nouvellePage) ->
                mettreAJourPageTable(nouvellePage.intValue())
        );
    }

    private void configurerDoubleClic() {
        tableAudit.setRowFactory(tv -> {
            TableRow<AuditLigne> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    afficherDetailsAudit(row.getItem());
                }
            });
            return row;
        });
    }

    private void chargerCategories() {
        service.listerCategoriesAsync(idEcoleCourante,
                categories -> Platform.runLater(() -> {
                    List<String> listeComplete = new ArrayList<>();
                    listeComplete.add(OPTION_TOUS);
                    if (categories != null) {
                        listeComplete.addAll(categories);
                    }
                    filterTypeAction.setItems(FXCollections.observableArrayList(listeComplete));
                    filterTypeAction.getSelectionModel().select(OPTION_TOUS);
                }),
                erreur -> Platform.runLater(erreur::printStackTrace));
    }

    private void mettreAJourPageTable(int pageIndex) {
        if (dernierResultat.isEmpty()) {
            tableAudit.setItems(FXCollections.observableArrayList());
            return;
        }

        int indexDebut = pageIndex * ELEMENTS_PAR_PAGE;
        int indexFin = Math.min(indexDebut + ELEMENTS_PAR_PAGE, dernierResultat.size());

        if (indexDebut < dernierResultat.size()) {
            List<AuditLigne> sousListe = dernierResultat.subList(indexDebut, indexFin);
            tableAudit.setItems(FXCollections.observableArrayList(sousListe));
        } else {
            tableAudit.setItems(FXCollections.observableArrayList());
        }
    }

    private void mettreAJourStatistiques(List<AuditLigne> audits) {
        long total = audits.size();
        long alertes = audits.stream().filter(a ->
                a.action().toLowerCase().contains("erreur") ||
                        a.action().toLowerCase().contains("échec") ||
                        a.action().toLowerCase().contains("refus")
        ).count();
        long succes = total - alertes;

        statTotalLabel.setText(String.valueOf(total));
        statConnectesLabel.setText(String.valueOf(succes));
        statSuspendusLabel.setText(String.valueOf(alertes));
    }

    private void afficherDetailsAudit(AuditLigne ligne) {
        if (ligne == null) return;

        String fxmlPath = "/org/erpklassup/erpklassup/view/audit-detail-view.fxml";

        AuditDetailDialogController controller = ModalUtil.ouvrirModal(
                getClass(),
                fxmlPath,
                "Détail de l'Audit #" + ligne.idLogAudit(),
                tableAudit.getScene().getWindow(),
                650,
                680
        );

        if (controller != null) {
            controller.initData(ligne);
        }
    }

    private void exporterVersCsv() {
        if (dernierResultat.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Aucune donnée à exporter avec les filtres actuels.").showAndWait();
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter le journal d'audit");
        chooser.setInitialFileName("journal_audit.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        File fichier = chooser.showSaveDialog(btnExporterAudit.getScene().getWindow());
        if (fichier == null) return;

        List<AuditLigne> aExporter = dernierResultat;
        btnExporterAudit.setDisable(true);

        AppExecutor.get().submit(() -> {
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(fichier), StandardCharsets.UTF_8))) {

                writer.write('\uFEFF');
                writer.write("Horodatage;Auteur;Catégorie;Action;Adresse IP;Appareil;Détails");
                writer.newLine();

                for (AuditLigne ligne : aExporter) {
                    writer.write(String.join(";",
                            ligne.horodatage().format(FORMAT_DATE),
                            echapperCsv(ligne.auteur()),
                            echapperCsv(ligne.categorie()),
                            echapperCsv(ligne.action()),
                            echapperCsv(ligne.adresseIp()),
                            echapperCsv(ligne.appareil()),
                            echapperCsv(ligne.details())));
                    writer.newLine();
                }

                Platform.runLater(() -> {
                    btnExporterAudit.setDisable(false);
                    new Alert(Alert.AlertType.INFORMATION, "Export terminé : " + fichier.getName()).showAndWait();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnExporterAudit.setDisable(false);
                    new Alert(Alert.AlertType.ERROR, "Échec de l'export : " + e.getMessage()).showAndWait();
                });
            }
        });
    }

    private String echapperCsv(String valeur) {
        if (valeur == null) return "";
        String nettoye = valeur.replace("\"", "\"\"");
        return nettoye.contains(";") || nettoye.contains("\n") ? "\"" + nettoye + "\"" : nettoye;
    }
}
//package org.erpklassup.erpklassup.controllers.actionPage;
//
//import javafx.animation.PauseTransition;
//import javafx.application.Platform;
//import javafx.collections.FXCollections;
//import javafx.concurrent.ScheduledService;
//import javafx.concurrent.Task;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.geometry.Insets;
//import javafx.scene.control.*;
//import javafx.scene.input.MouseButton;
//import javafx.scene.layout.GridPane;
//import javafx.scene.layout.Priority;
//import javafx.stage.FileChooser;
//import javafx.stage.Window;
//import javafx.util.Duration;
//
//import org.erpklassup.erpklassup.dto.AuditLigne;
//import org.erpklassup.erpklassup.dto.FiltreAudit;
//import org.erpklassup.erpklassup.dto.RoleOption;
//import org.erpklassup.erpklassup.dto.StatutCompte;
//import org.erpklassup.erpklassup.service.AppExecutor;
//import org.erpklassup.erpklassup.service.AuditService;
//import org.erpklassup.erpklassup.service.SessionManager;
//import org.erpklassup.erpklassup.util.ControleAcces;
//import org.erpklassup.erpklassup.util.ModalUtil;
//import org.erpklassup.erpklassup.util.VueDisposable;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.OutputStreamWriter;
//import java.net.URL;
//import java.nio.charset.StandardCharsets;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.util.ResourceBundle;
//
//public class JournalAuditTabController implements Initializable, VueDisposable {
//
//    @FXML private TextField searchAudit;
//    @FXML private DatePicker datePickerDebut;
//    @FXML private DatePicker datePickerFin;
//    @FXML private ComboBox<String> filterTypeAction;
//    @FXML private Button btnExporterAudit;
//
//    @FXML private Label statTotalLabel;
//    @FXML private Label statConnectesLabel;
//    @FXML private Label statSuspendusLabel;
//
//    @FXML private TableView<AuditLigne> tableAudit;
//    @FXML private TableColumn<AuditLigne, String> colHorodatage;
//    @FXML private TableColumn<AuditLigne, String> colAuteur;
//    @FXML private TableColumn<AuditLigne, String> colAction;
//    @FXML private TableColumn<AuditLigne, String> colAdresseIp;
//    @FXML private TableColumn<AuditLigne, String> colAppareil;
//
//    @FXML private Pagination paginationAudit;
//
//    private static final int ELEMENTS_PAR_PAGE = 20;
//    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
//
//    private final AuditService service = new AuditService();
//    private final PauseTransition debounceRecherche = new PauseTransition(Duration.millis(350));
//    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;
//
//    private String idEcoleCourante;
//    private List<AuditLigne> dernierResultat = List.of();
//
//    // 🔄 Service de rafraîchissement automatique
//    private ScheduledService<List<AuditLigne>> serviceRafraichissementAuto;
//
//    @Override
//    public void initialize(URL url, ResourceBundle rb) {
//        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();
//
//        configurerTable();
//        configurerFiltres();
//        configurerRecherche();
//        configurerPagination();
//        configurerDoubleClic();
//
//        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
//        appliquerControlesAcces();
//
//        if (idEcoleCourante == null) {
//            tableAudit.setPlaceholder(new Label("Aucune école active — connectez-vous d'abord."));
//            return;
//        }
//
//        chargerCategories();
//        initialiserServiceRafraichissementAuto();
//    }
//
//    @Override
//    public void disposer() {
//        // ⚠️ Arrêt du service d'arrière-plan pour éviter les fuites mémoire
//        if (serviceRafraichissementAuto != null && serviceRafraichissementAuto.isRunning()) {
//            serviceRafraichissementAuto.cancel();
//        }
//        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
//    }
//
//    /**
//     * Initialise et démarre le rechargement périodique en tâche de fond.
//     */
//    private void initialiserServiceRafraichissementAuto() {
//        serviceRafraichissementAuto = new ScheduledService<>() {
//            @Override
//            protected Task<List<AuditLigne>> createTask() {
//                return new Task<>() {
//                    @Override
//                    protected List<AuditLigne> call() {
//                        if (idEcoleCourante == null) return List.of();
//
//                        FiltreAudit filtre = new FiltreAudit(
//                                idEcoleCourante,
//                                searchAudit.getText(),
//                                datePickerDebut.getValue(),
//                                datePickerFin.getValue(),
//                                filterTypeAction.getSelectionModel().getSelectedItem()
//                        );
//
//                        // Requête Synchrone exécutée sur le thread d'arrière-plan du ScheduledService
//                        return service.rechercher(filtre);
//                    }
//                };
//            }
//        };
//
//        // Intervalle de mise à jour (ex: toutes les 3 secondes)
//        serviceRafraichissementAuto.setPeriod(Duration.seconds(3));
//
//        // Mise à jour du composant UI à la fin de chaque cycle
//        serviceRafraichissementAuto.setOnSucceeded(event -> {
//            List<AuditLigne> resultat = serviceRafraichissementAuto.getValue();
//            if (resultat != null) {
//                this.dernierResultat = resultat;
//                mettreAJourStatistiques(resultat);
//
//                int pageIndexActuel = paginationAudit.getCurrentPageIndex();
//                int nombrePages = Math.max(1, (int) Math.ceil((double) resultat.size() / ELEMENTS_PAR_PAGE));
//
//                paginationAudit.setPageCount(nombrePages);
//
//                // Maintient la page courante si elle reste valide, sinon réinitialise
//                if (pageIndexActuel >= nombrePages) {
//                    paginationAudit.setCurrentPageIndex(0);
//                    mettreAJourPageTable(0);
//                } else {
//                    mettreAJourPageTable(pageIndexActuel);
//                }
//
//                tableAudit.setPlaceholder(new Label("Aucun enregistrement dans le journal d'audit"));
//            }
//        });
//
//        serviceRafraichissementAuto.setOnFailed(event -> {
//            tableAudit.setPlaceholder(new Label("Erreur de chargement du journal"));
//            Throwable err = serviceRafraichissementAuto.getException();
//            if (err != null) err.printStackTrace();
//        });
//
//        tableAudit.setPlaceholder(new Label("Chargement..."));
//        serviceRafraichissementAuto.start();
//    }
//
//    private void relancerServiceInmediatement() {
//        if (serviceRafraichissementAuto != null && serviceRafraichissementAuto.isRunning()) {
//            serviceRafraichissementAuto.restart();
//        }
//    }
//
//    private void appliquerControlesAcces() {
//        ControleAcces.appliquerAction(btnExporterAudit, "AUDIT_EXPORTER");
//    }
//
//    private void configurerTable() {
//        tableAudit.setItems(FXCollections.observableArrayList());
//
//        colHorodatage.setCellValueFactory(data ->
//                new javafx.beans.property.SimpleStringProperty(data.getValue().horodatage().format(FORMAT_DATE)));
//        colAuteur.setCellValueFactory(data ->
//                new javafx.beans.property.SimpleStringProperty(data.getValue().auteur()));
//        colAction.setCellValueFactory(data ->
//                new javafx.beans.property.SimpleStringProperty(data.getValue().action()));
//        colAdresseIp.setCellValueFactory(data ->
//                new javafx.beans.property.SimpleStringProperty(
//                        data.getValue().adresseIp() != null ? data.getValue().adresseIp() : "—"));
//        colAppareil.setCellValueFactory(data ->
//                new javafx.beans.property.SimpleStringProperty(
//                        data.getValue().appareil() != null ? data.getValue().appareil() : "—"));
//    }
//
//    private void configurerFiltres() {
//        datePickerDebut.valueProperty().addListener((obs, a, n) -> relancerServiceInmediatement());
//        datePickerFin.valueProperty().addListener((obs, a, n) -> relancerServiceInmediatement());
//
//
//        filterTypeAction.getSelectionModel().selectedItemProperty().addListener((obs, a, n) -> relancerServiceInmediatement());
//        btnExporterAudit.setOnAction(e -> exporterVersCsv());
//    }
//
//    private void configurerRecherche() {
//        debounceRecherche.setOnFinished(e -> relancerServiceInmediatement());
//        searchAudit.textProperty().addListener((obs, ancien, nouveau) -> debounceRecherche.playFromStart());
//    }
//
//    private void configurerPagination() {
//        paginationAudit.currentPageIndexProperty().addListener((obs, anciennePage, nouvellePage) ->
//                mettreAJourPageTable(nouvellePage.intValue())
//        );
//    }
//
//    private void configurerDoubleClic() {
//        tableAudit.setRowFactory(tv -> {
//            TableRow<AuditLigne> row = new TableRow<>();
//            row.setOnMouseClicked(event -> {
//                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
//                    afficherDetailsAudit(row.getItem());
//                }
//            });
//            return row;
//        });
//    }
//
//    private void chargerCategories() {
//        service.listerCategoriesAsync(idEcoleCourante,
//                categories -> Platform.runLater(() ->
//                        filterTypeAction.setItems(FXCollections.observableArrayList(categories))
//                ),
//                erreur -> Platform.runLater(erreur::printStackTrace));
//    }
//
//    private void mettreAJourPageTable(int pageIndex) {
//        if (dernierResultat.isEmpty()) {
//            tableAudit.setItems(FXCollections.observableArrayList());
//            return;
//        }
//
//        int indexDebut = pageIndex * ELEMENTS_PAR_PAGE;
//        int indexFin = Math.min(indexDebut + ELEMENTS_PAR_PAGE, dernierResultat.size());
//
//        if (indexDebut < dernierResultat.size()) {
//            List<AuditLigne> sousListe = dernierResultat.subList(indexDebut, indexFin);
//            tableAudit.setItems(FXCollections.observableArrayList(sousListe));
//        } else {
//            tableAudit.setItems(FXCollections.observableArrayList());
//        }
//    }
//
//    private void mettreAJourStatistiques(List<AuditLigne> audits) {
//        long total = audits.size();
//        long alertes = audits.stream().filter(a ->
//                a.action().toLowerCase().contains("erreur") ||
//                        a.action().toLowerCase().contains("échec") ||
//                        a.action().toLowerCase().contains("refus")
//        ).count();
//        long succes = total - alertes;
//
//        statTotalLabel.setText(String.valueOf(total));
//        statConnectesLabel.setText(String.valueOf(succes));
//        statSuspendusLabel.setText(String.valueOf(alertes));
//    }
//
//    private void afficherDetailsAudit(AuditLigne ligne) {
//        if (ligne == null) return;
//
//        String fxmlPath = "/org/erpklassup/erpklassup/view/audit-detail-view.fxml";
//
//        AuditDetailDialogController controller = ModalUtil.ouvrirModal(
//                getClass(),
//                fxmlPath,
//                "Détail de l'Audit #" + ligne.idLogAudit(),
//                tableAudit.getScene().getWindow(),
//                650,
//                680
//        );
//
//        if (controller != null) {
//            controller.initData(ligne);
//        }
//    }
//
//    private void exporterVersCsv() {
//        if (dernierResultat.isEmpty()) {
//            new Alert(Alert.AlertType.INFORMATION, "Aucune donnée à exporter avec les filtres actuels.").showAndWait();
//            return;
//        }
//
//        FileChooser chooser = new FileChooser();
//        chooser.setTitle("Exporter le journal d'audit");
//        chooser.setInitialFileName("journal_audit.csv");
//        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
//        File fichier = chooser.showSaveDialog(btnExporterAudit.getScene().getWindow());
//        if (fichier == null) return;
//
//        List<AuditLigne> aExporter = dernierResultat;
//        btnExporterAudit.setDisable(true);
//
//        AppExecutor.get().submit(() -> {
//            try (BufferedWriter writer = new BufferedWriter(
//                    new OutputStreamWriter(new FileOutputStream(fichier), StandardCharsets.UTF_8))) {
//
//                writer.write('\uFEFF');
//                writer.write("Horodatage;Auteur;Catégorie;Action;Adresse IP;Appareil;Détails");
//                writer.newLine();
//
//                for (AuditLigne ligne : aExporter) {
//                    writer.write(String.join(";",
//                            ligne.horodatage().format(FORMAT_DATE),
//                            echapperCsv(ligne.auteur()),
//                            echapperCsv(ligne.categorie()),
//                            echapperCsv(ligne.action()),
//                            echapperCsv(ligne.adresseIp()),
//                            echapperCsv(ligne.appareil()),
//                            echapperCsv(ligne.details())));
//                    writer.newLine();
//                }
//
//                Platform.runLater(() -> {
//                    btnExporterAudit.setDisable(false);
//                    new Alert(Alert.AlertType.INFORMATION, "Export terminé : " + fichier.getName()).showAndWait();
//                });
//            } catch (Exception e) {
//                Platform.runLater(() -> {
//                    btnExporterAudit.setDisable(false);
//                    new Alert(Alert.AlertType.ERROR, "Échec de l'export : " + e.getMessage()).showAndWait();
//                });
//            }
//        });
//    }
//
//    private String echapperCsv(String valeur) {
//        if (valeur == null) return "";
//        String nettoye = valeur.replace("\"", "\"\"");
//        return nettoye.contains(";") || nettoye.contains("\n") ? "\"" + nettoye + "\"" : nettoye;
//    }
//}