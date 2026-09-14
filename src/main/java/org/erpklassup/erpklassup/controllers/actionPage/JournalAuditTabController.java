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
import org.erpklassup.erpklassup.util.I18nManager;
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
    private final Runnable ecouteurI18n = this::rafraichirTextes;

    private String idEcoleCourante;
    private List<AuditLigne> dernierResultat = List.of();

    private ScheduledService<List<AuditLigne>> serviceRafraichissementAuto;

    // ✅ Raccourci i18n
    private I18nManager i18n() {
        return I18nManager.getInstance();
    }

    // ✅ Option "Tous les audits" traduite dynamiquement
    private String optionTous() {
        return i18n().t("audit.filter.all");
    }

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

        // ✅ S'abonner aux changements de langue
        i18n().ecouterChangement(ecouteurI18n);

        if (idEcoleCourante == null) {
            tableAudit.setPlaceholder(new Label(i18n().t("audit.value.no_school")));
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
        i18n().arreterEcoute(ecouteurI18n);
    }

    /** ✅ Rafraîchit les textes dynamiques après changement de langue. */
    private void rafraichirTextes() {
        if (tableAudit != null) tableAudit.refresh();

        // Met à jour le placeholder si vide
        if (tableAudit != null && tableAudit.getItems().isEmpty()) {
            tableAudit.setPlaceholder(new Label(i18n().t("audit.value.empty")));
        }

        // Recharge les catégories (l'option "Tous" change de libellé)
        if (idEcoleCourante != null) chargerCategories();
    }

    private void initialiserServiceRafraichissementAuto() {
        serviceRafraichissementAuto = new ScheduledService<>() {
            @Override
            protected Task<List<AuditLigne>> createTask() {
                return new Task<>() {
                    @Override
                    protected List<AuditLigne> call() {
                        if (idEcoleCourante == null) return List.of();

                        String selection = filterTypeAction.getSelectionModel().getSelectedItem();

                        // Si "Tous les audits" (traduit) ou rien n'est sélectionné → null
                        String typeActionFiltre = (selection == null || optionTous().equals(selection))
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

                tableAudit.setPlaceholder(new Label(i18n().t("audit.value.empty")));
            }
        });

        serviceRafraichissementAuto.setOnFailed(event -> {
            tableAudit.setPlaceholder(new Label(i18n().t("audit.value.error_load")));
            Throwable err = serviceRafraichissementAuto.getException();
            if (err != null) err.printStackTrace();
        });

        tableAudit.setPlaceholder(new Label(i18n().t("audit.value.loading")));
        serviceRafraichissementAuto.start();
    }

    private void relancerServiceImmediatement() {
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
        datePickerDebut.valueProperty().addListener((obs, a, n) -> relancerServiceImmediatement());
        datePickerFin.valueProperty().addListener((obs, a, n) -> relancerServiceImmediatement());
        filterTypeAction.getSelectionModel().selectedItemProperty().addListener((obs, a, n) -> relancerServiceImmediatement());
        btnExporterAudit.setOnAction(e -> exporterVersCsv());
    }

    private void configurerRecherche() {
        debounceRecherche.setOnFinished(e -> relancerServiceImmediatement());
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
                    listeComplete.add(optionTous());
                    if (categories != null) {
                        listeComplete.addAll(categories);
                    }
                    filterTypeAction.setItems(FXCollections.observableArrayList(listeComplete));
                    filterTypeAction.getSelectionModel().selectFirst();
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
                i18n().t("audit.detail.title", ligne.idLogAudit()),
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
            new Alert(Alert.AlertType.INFORMATION,
                    i18n().t("audit.export.no_data")).showAndWait();
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(i18n().t("audit.export.title"));
        chooser.setInitialFileName("journal_audit.csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(i18n().t("audit.export.filter_csv"), "*.csv"));
        File fichier = chooser.showSaveDialog(btnExporterAudit.getScene().getWindow());
        if (fichier == null) return;

        List<AuditLigne> aExporter = dernierResultat;
        btnExporterAudit.setDisable(true);

        AppExecutor.get().submit(() -> {
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(fichier), StandardCharsets.UTF_8))) {

                writer.write('\uFEFF');
                writer.write(i18n().t("audit.export.header"));
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
                    new Alert(Alert.AlertType.INFORMATION,
                            i18n().t("audit.export.success", fichier.getName())).showAndWait();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnExporterAudit.setDisable(false);
                    new Alert(Alert.AlertType.ERROR,
                            i18n().t("audit.export.error", e.getMessage())).showAndWait();
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