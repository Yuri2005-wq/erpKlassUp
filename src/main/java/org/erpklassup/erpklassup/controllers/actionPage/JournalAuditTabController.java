package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import org.erpklassup.erpklassup.dto.AuditLigne;
import org.erpklassup.erpklassup.dto.FiltreAudit;
import org.erpklassup.erpklassup.service.AppExecutor;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.ControleAcces;
import org.erpklassup.erpklassup.util.VueDisposable;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class JournalAuditTabController implements Initializable, VueDisposable {

    @FXML private TextField searchAudit;
    @FXML private DatePicker datePickerDebut;
    @FXML private DatePicker datePickerFin;
    @FXML private ComboBox<String> filterTypeAction;
    @FXML private Button btnExporterAudit;

    @FXML private TableView<AuditLigne> tableAudit;
    @FXML private TableColumn<AuditLigne, String> colHorodatage;
    @FXML private TableColumn<AuditLigne, String> colAuteur;
    @FXML private TableColumn<AuditLigne, String> colAction;
    @FXML private TableColumn<AuditLigne, String> colAdresseIp;
    @FXML private TableColumn<AuditLigne, String> colAppareil;

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AuditService service = new AuditService();
    private final PauseTransition debounceRecherche = new PauseTransition(Duration.millis(350));
    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;

    private String idEcoleCourante;
    private List<AuditLigne> dernierResultat = List.of();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();

        configurerTable();
        configurerFiltres();
        configurerRecherche();

        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
        appliquerControlesAcces();

        if (idEcoleCourante == null) {
            tableAudit.setPlaceholder(new Label("Aucune école active — connectez-vous d'abord."));
            return;
        }

        chargerCategories();
        lancerRecherche();
    }

    @Override
    public void disposer() {
        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
    }

    private void appliquerControlesAcces() {
        ControleAcces.appliquerAction(btnExporterAudit, "AUDIT_EXPORTER");
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
        datePickerDebut.valueProperty().addListener((obs, a, n) -> lancerRecherche());
        datePickerFin.valueProperty().addListener((obs, a, n) -> lancerRecherche());
        filterTypeAction.getSelectionModel().selectedItemProperty().addListener((obs, a, n) -> lancerRecherche());
        btnExporterAudit.setOnAction(e -> exporterVersCsv());
    }

    private void configurerRecherche() {
        debounceRecherche.setOnFinished(e -> lancerRecherche());
        searchAudit.textProperty().addListener((obs, ancien, nouveau) -> debounceRecherche.playFromStart());
    }

    private void chargerCategories() {
        service.listerCategoriesAsync(idEcoleCourante,
                categories -> Platform.runLater(() -> {
                    filterTypeAction.setItems(FXCollections.observableArrayList(categories));
                }),
                erreur -> Platform.runLater(erreur::printStackTrace));
    }

    private void lancerRecherche() {
        if (idEcoleCourante == null) return;

        FiltreAudit filtre = new FiltreAudit(
                idEcoleCourante,
                searchAudit.getText(),
                datePickerDebut.getValue(),
                datePickerFin.getValue(),
                filterTypeAction.getSelectionModel().getSelectedItem()
        );

        tableAudit.setPlaceholder(new Label("Chargement..."));

        service.rechercherAsync(filtre,
                resultat -> Platform.runLater(() -> {
                    dernierResultat = resultat;
                    tableAudit.setItems(FXCollections.observableArrayList(resultat));
                    tableAudit.setPlaceholder(new Label("Aucun enregistrement dans le journal d'audit"));
                }),
                erreur -> Platform.runLater(() -> {
                    tableAudit.setPlaceholder(new Label("Erreur de chargement du journal"));
                    erreur.printStackTrace();
                }));
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

                writer.write('\uFEFF'); // BOM UTF-8 : Excel affiche correctement les accents
                writer.write("Horodatage;Auteur;Catégorie;Action;Adresse IP;Appareil");
                writer.newLine();

                for (AuditLigne ligne : aExporter) {
                    writer.write(String.join(";",
                            ligne.horodatage().format(FORMAT_DATE),
                            echapperCsv(ligne.auteur()),
                            echapperCsv(ligne.categorie()),
                            echapperCsv(ligne.action()),
                            echapperCsv(ligne.adresseIp()),
                            echapperCsv(ligne.appareil())));
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