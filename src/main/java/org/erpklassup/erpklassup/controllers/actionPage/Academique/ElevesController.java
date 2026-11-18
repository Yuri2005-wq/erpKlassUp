package org.erpklassup.erpklassup.controllers.actionPage.Academique;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.models.Academique.Eleve;
import org.erpklassup.erpklassup.service.Academique.EleveService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.AlertUtil;
import org.erpklassup.erpklassup.util.AvatarUtil;
import org.erpklassup.erpklassup.util.ModalUtil;
import org.erpklassup.erpklassup.util.ToastNotification;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ElevesController implements Initializable {

    @FXML private TextField rechercheField;
    @FXML private Label totalLabel;
    @FXML private Label comptesLabel;
    @FXML private Label sansCompteLabel;
    @FXML private TableView<Eleve> tableEleves;
    @FXML private TableColumn<Eleve, String> colMatricule;
    @FXML private TableColumn<Eleve, String> colNom;
    @FXML private TableColumn<Eleve, String> colSexe;
    @FXML private TableColumn<Eleve, String> colDateNaissance;
    @FXML private TableColumn<Eleve, String> colTuteur;
    @FXML private TableColumn<Eleve, String> colTelephone;
    @FXML private TableColumn<Eleve, String> colCompte;
    @FXML private FlowPane kanbanAvecCompte;
    @FXML private FlowPane kanbanSansCompte;
    @FXML private VBox vueListe;
    @FXML private VBox vueKanban;
    @FXML private ToggleButton btnVueListe;
    @FXML private ToggleButton btnVueKanban;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;

    private final EleveService eleveService = new EleveService();
    private String idEcoleCourante;
    private List<Eleve> eleves = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();
        configurerTableau();
        configurerActions();
        basculerVue(true);
        lancerRecherche();
    }

    private void configurerTableau() {
        colMatricule.setCellValueFactory(cell ->
                new SimpleStringProperty(valeur(cell.getValue().getMatricule())));
        colNom.setCellValueFactory(cell ->
                new SimpleStringProperty(valeur(cell.getValue().getNomComplet())));
        colSexe.setCellValueFactory(cell ->
                new SimpleStringProperty(valeur(cell.getValue().getSexe())));
        colDateNaissance.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDateNaissance() == null
                        ? "—" : cell.getValue().getDateNaissance().toString()));
        colTuteur.setCellValueFactory(cell ->
                new SimpleStringProperty(valeur(cell.getValue().getNomTuteur())));
        colTelephone.setCellValueFactory(cell ->
                new SimpleStringProperty(valeur(cell.getValue().getTelephoneTuteur())));
        colCompte.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getIdUtilisateur() == null
                        || cell.getValue().getIdUtilisateur().isBlank() ? "Sans compte" : "Compte lié"));
        tableEleves.setRowFactory(table -> {
            TableRow<Eleve> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2) {
                    ouvrirEdition(row.getItem());
                }
            });
            return row;
        });
        tableEleves.getSelectionModel().selectedItemProperty().addListener((obs, ancien, selection) -> {
            boolean aucun = selection == null;
            btnModifier.setDisable(aucun);
            btnSupprimer.setDisable(aucun);
        });
    }

    private void configurerActions() {
        rechercheField.textProperty().addListener((obs, ancien, nouveau) -> lancerRecherche());
    }

    @FXML
    private void handleActualiser() {
        lancerRecherche();
    }

    @FXML
    private void handleNouvelEleve() {
        ModalEleveController controller = ModalUtil.ouvrirModal(
                getClass(),
                "/org/erpklassup/erpklassup/view/Academique/eleve-modal.fxml",
                "Nouvel élève",
                tableEleves.getScene().getWindow(),
                920,
                760
        );
        if (controller != null) controller.initCreation(idEcoleCourante, this::lancerRecherche);
    }

    @FXML
    private void handleModifier() {
        ouvrirEdition(tableEleves.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void handleSupprimer() {
        supprimer(tableEleves.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void handleVueListe() {
        basculerVue(true);
    }

    @FXML
    private void handleVueKanban() {
        basculerVue(false);
    }

    private void basculerVue(boolean liste) {
        vueListe.setVisible(liste);
        vueListe.setManaged(liste);
        vueKanban.setVisible(!liste);
        vueKanban.setManaged(!liste);
        btnVueListe.setSelected(liste);
        btnVueKanban.setSelected(!liste);
    }

    private void lancerRecherche() {
        if (idEcoleCourante == null || idEcoleCourante.isBlank()) {
            afficherErreur(new IllegalStateException("Aucune école active n'est sélectionnée."));
            return;
        }
        eleveService.rechercherAsync(idEcoleCourante, rechercheField.getText(),
                resultat -> {
                    eleves = resultat;
                    tableEleves.setItems(FXCollections.observableArrayList(resultat));
                    afficherKanban(resultat);
                    mettreAJourStatistiques(resultat);
                },
                this::afficherErreur
        );
    }

    private void afficherKanban(List<Eleve> resultat) {
        kanbanAvecCompte.getChildren().clear();
        kanbanSansCompte.getChildren().clear();
        for (Eleve eleve : resultat) {
            FlowPane colonne = eleve.getIdUtilisateur() == null || eleve.getIdUtilisateur().isBlank()
                    ? kanbanSansCompte : kanbanAvecCompte;
            colonne.getChildren().add(creerCarte(eleve));
        }
        if (kanbanAvecCompte.getChildren().isEmpty()) kanbanAvecCompte.getChildren().add(placeholder("Aucun élève avec compte."));
        if (kanbanSansCompte.getChildren().isEmpty()) kanbanSansCompte.getChildren().add(placeholder("Aucun élève sans compte."));
    }

    private Node creerCarte(Eleve eleve) {
        HBox identite = new HBox(10,
                AvatarUtil.creer(eleve.getPhotoPath(), eleve.getNom(), eleve.getPrenom(), eleve.getIdEleve(), 42),
                new VBox(3, new Label(eleve.getNomComplet()), new Label(eleve.getMatricule())));
        identite.setAlignment(Pos.CENTER_LEFT);
        Label sexe = new Label("Sexe : " + valeur(eleve.getSexe()));
        Label tuteur = new Label("Tuteur : " + valeur(eleve.getNomTuteur()));
        Label telephone = new Label(valeur(eleve.getTelephoneTuteur()));
        Button modifier = new Button("Modifier");
        modifier.setOnAction(event -> ouvrirEdition(eleve));
        Button supprimer = new Button("Supprimer");
        supprimer.setOnAction(event -> supprimer(eleve));
        HBox actions = new HBox(8, modifier, supprimer);
        VBox carte = new VBox(10, identite, sexe, tuteur, telephone, actions);
        carte.setPrefWidth(250);
        carte.setMaxWidth(280);
        carte.getStyleClass().add("card");
        return carte;
    }

    private Node placeholder(String texte) {
        Label label = new Label(texte);
        label.getStyleClass().add("footer-info");
        return label;
    }

    private void mettreAJourStatistiques(List<Eleve> resultat) {
        long avecCompte = resultat.stream()
                .filter(e -> e.getIdUtilisateur() != null && !e.getIdUtilisateur().isBlank())
                .count();
        totalLabel.setText(String.valueOf(resultat.size()));
        comptesLabel.setText(String.valueOf(avecCompte));
        sansCompteLabel.setText(String.valueOf(resultat.size() - avecCompte));
    }

    private void ouvrirEdition(Eleve eleve) {
        if (eleve == null) return;
        eleveService.chargerAsync(idEcoleCourante, eleve.getIdEleve(),
                resultat -> resultat.ifPresentOrElse(
                        frais -> {
                            ModalEleveController controller = ModalUtil.ouvrirModal(
                                    getClass(),
                                    "/org/erpklassup/erpklassup/view/Academique/eleve-modal.fxml",
                                    "Modifier l'élève : " + frais.getNomComplet(),
                                    tableEleves.getScene().getWindow(),
                                    920,
                                    760
                            );
                            if (controller != null) controller.initEdition(frais, this::lancerRecherche);
                        },
                        () -> afficherErreur(new IllegalStateException("Élève introuvable."))
                ),
                this::afficherErreur
        );
    }

    private void supprimer(Eleve eleve) {
        if (eleve == null) return;
        boolean confirmation = AlertUtil.afficherConfirmation(
                "Supprimer l'élève",
                "Voulez-vous vraiment supprimer " + eleve.getNomComplet() + " ?",
                tableEleves.getScene().getWindow()
        );
        if (!confirmation) return;
        eleveService.supprimerAsync(idEcoleCourante, eleve.getIdEleve(),
                () -> {
                    ToastNotification.succes(getStage(), "Élève supprimé.");
                    lancerRecherche();
                },
                this::afficherErreur
        );
    }

    private void afficherErreur(Throwable erreur) {
        Platform.runLater(() -> ToastNotification.erreur(getStage(),
                erreur == null || erreur.getMessage() == null ? "Une erreur est survenue." : erreur.getMessage()));
    }

    private Stage getStage() {
        return (Stage) tableEleves.getScene().getWindow();
    }

    private String valeur(String texte) {
        return texte == null || texte.isBlank() ? "—" : texte;
    }
}
