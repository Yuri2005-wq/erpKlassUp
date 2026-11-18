package org.erpklassup.erpklassup.controllers.actionPage.Academique;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.models.Academique.Eleve;
import org.erpklassup.erpklassup.service.Academique.EleveService;
import org.erpklassup.erpklassup.util.AvatarUtil;
import org.erpklassup.erpklassup.util.ToastNotification;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class ModalEleveController implements Initializable {

    @FXML private Label titreLabel;
    @FXML private TextField matriculeField;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private ComboBox<String> sexeCombo;
    @FXML private DatePicker dateNaissancePicker;
    @FXML private TextField lieuNaissanceField;
    @FXML private TextField nationaliteField;
    @FXML private TextField adresseField;
    @FXML private TextField nomTuteurField;
    @FXML private TextField telephoneTuteurField;
    @FXML private TextField emailTuteurField;
    @FXML private TextField compteField;
    @FXML private TextField photoPathField;
    @FXML private StackPane avatarPreview;
    @FXML private TextArea antecedentsField;
    @FXML private Button enregistrerButton;

    private final EleveService eleveService = new EleveService();
    private String idEcoleCourante;
    private Eleve eleveEnEdition;
    private Runnable onSucces;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sexeCombo.getItems().setAll("MASCULIN", "FEMININ", "AUTRE");
        sexeCombo.setValue("MASCULIN");
        nationaliteField.setText("Camerounaise");
        photoPathField.textProperty().addListener((obs, ancien, nouveau) -> actualiserAvatar());
        nomField.textProperty().addListener((obs, ancien, nouveau) -> actualiserAvatar());
        prenomField.textProperty().addListener((obs, ancien, nouveau) -> actualiserAvatar());
    }

    public void initCreation(String idEcole, Runnable onSucces) {
        this.idEcoleCourante = idEcole;
        this.onSucces = onSucces;
        this.eleveEnEdition = null;
        titreLabel.setText("Nouveau dossier élève");
        matriculeField.setEditable(true);
        actualiserAvatar();
    }

    public void initEdition(Eleve eleve, Runnable onSucces) {
        if (eleve == null) return;
        this.eleveEnEdition = eleve;
        this.idEcoleCourante = eleve.getIdEcole();
        this.onSucces = onSucces;
        titreLabel.setText("Modifier le dossier élève");
        matriculeField.setText(eleve.getMatricule());
        nomField.setText(eleve.getNom());
        prenomField.setText(eleve.getPrenom());
        sexeCombo.setValue(eleve.getSexe());
        dateNaissancePicker.setValue(eleve.getDateNaissance());
        lieuNaissanceField.setText(eleve.getLieuNaissance());
        nationaliteField.setText(eleve.getNationalite());
        adresseField.setText(eleve.getAdresse());
        nomTuteurField.setText(eleve.getNomTuteur());
        telephoneTuteurField.setText(eleve.getTelephoneTuteur());
        emailTuteurField.setText(eleve.getEmailTuteur());
        compteField.setText(eleve.getIdUtilisateur());
        photoPathField.setText(eleve.getPhotoPath());
        antecedentsField.setText(eleve.getAntecedentsMedicaux());
        actualiserAvatar();
    }

    @FXML
    private void handleChoisirPhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        File fichier = chooser.showOpenDialog(getStage());
        if (fichier != null) photoPathField.setText(fichier.getAbsolutePath());
    }

    @FXML
    private void handleEnregistrer() {
        Eleve eleve = eleveEnEdition == null ? new Eleve() : eleveEnEdition;
        eleve.setIdEcole(idEcoleCourante);
        eleve.setMatricule(texte(matriculeField));
        eleve.setNom(texte(nomField));
        eleve.setPrenom(texte(prenomField));
        eleve.setSexe(sexeCombo.getValue());
        eleve.setDateNaissance(dateNaissancePicker.getValue());
        eleve.setLieuNaissance(texte(lieuNaissanceField));
        eleve.setNationalite(texte(nationaliteField));
        eleve.setAdresse(texte(adresseField));
        eleve.setNomTuteur(texte(nomTuteurField));
        eleve.setTelephoneTuteur(texte(telephoneTuteurField));
        eleve.setEmailTuteur(texte(emailTuteurField));
        eleve.setIdUtilisateur(texte(compteField));
        eleve.setPhotoPath(texte(photoPathField));
        eleve.setAntecedentsMedicaux(antecedentsField.getText());

        enregistrerButton.setDisable(true);
        if (eleveEnEdition == null) {
            eleveService.creerAsync(eleve, this::terminerEnregistrement, this::afficherErreur);
        } else {
            eleveService.modifierAsync(eleve, this::terminerEnregistrement, this::afficherErreur);
        }
    }

    @FXML
    private void handleAnnuler() {
        getStage().close();
    }

    private void actualiserAvatar() {
        if (avatarPreview == null) return;
        avatarPreview.getChildren().setAll(AvatarUtil.creer(
                texte(photoPathField), texte(nomField), texte(prenomField),
                eleveEnEdition == null ? valeurOuDefaut(texte(matriculeField)) : eleveEnEdition.getIdEleve(), 72));
    }

    private String valeurOuDefaut(String valeur) {
        return valeur == null ? "eleve" : valeur;
    }

    private void terminerEnregistrement() {
        ToastNotification.succes(getStage(), "Le dossier de l'élève a été enregistré.");
        if (onSucces != null) onSucces.run();
        getStage().close();
    }

    private void afficherErreur(Throwable erreur) {
        enregistrerButton.setDisable(false);
        String message = erreur == null || erreur.getMessage() == null
                ? "Impossible d'enregistrer l'élève."
                : erreur.getMessage();
        ToastNotification.erreur(getStage(), message);
    }

    private String texte(TextInputControl champ) {
        String valeur = champ.getText();
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    private Stage getStage() {
        return (Stage) enregistrerButton.getScene().getWindow();
    }
}
