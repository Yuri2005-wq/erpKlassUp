package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;

import org.erpklassup.erpklassup.dto.AuditLigne;
import org.erpklassup.erpklassup.util.ToastNotification;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class AuditDetailDialogController implements Initializable {

    @FXML private Label lblIdAudit;
    @FXML private Label lblGravite;
    @FXML private Label lblTempsEcoule;
    @FXML private Label lblStatutVerification;

    @FXML private Label lblHorodatage;
    @FXML private Label lblAuteur;
    @FXML private Label lblCategorie;
    @FXML private Label lblAction;
    @FXML private Label lblAdresseIp;
    @FXML private Label lblAppareil;

    @FXML private TextArea txtDetails;

    @FXML private Button btnCopierJson;
    @FXML private Button btnExporterPdf;
    @FXML private Button btnCopier;
    @FXML private Button btnCloseBottom;

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private AuditLigne auditLigneCourante;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        btnCloseBottom.setOnAction(e -> fermer());
        btnCopier.setOnAction(e -> copierDansPressePapier());
        btnCopierJson.setOnAction(e -> copierJsonBrut());
    }

    /**
     * Reçoit les données de la ligne d'audit et remplit l'ensemble de la vue FXML.
     */
    public void initData(AuditLigne ligne) {
        this.auditLigneCourante = ligne;

        // ID d'Audit
        lblIdAudit.setText("ID Log : #" + (ligne.idLogAudit() != null ? ligne.idLogAudit() : "N/A"));

        // Horodatage & calcul du temps écoulé
        if (ligne.horodatage() != null) {
            lblHorodatage.setText(ligne.horodatage().format(FORMAT_DATE));
            lblTempsEcoule.setText(calculerTempsEcoule(ligne.horodatage()));
        } else {
            lblHorodatage.setText("—");
            lblTempsEcoule.setText("Indéterminé");
        }

        // Informations principales
        lblAuteur.setText(ligne.auteur() != null ? ligne.auteur() : "Inconnu");
        lblCategorie.setText(ligne.categorie() != null ? ligne.categorie() : "GÉNÉRAL");
        lblAction.setText(ligne.action() != null ? ligne.action() : "—");
        lblAdresseIp.setText(ligne.adresseIp() != null ? ligne.adresseIp() : "—");
        lblAppareil.setText(ligne.appareil() != null ? ligne.appareil() : "—");

        // Détails
        txtDetails.setText(ligne.details() != null ? ligne.details() : "Aucun détail complémentaire enregistré pour cette action.");

        // Gravité dynamique basée sur les mots-clés d'action
        evaluerGraviteEtStatut(ligne.action());
    }

    private void evaluerGraviteEtStatut(String action) {
        if (action == null) action = "";
        String actionLower = action.toLowerCase();

        if (actionLower.contains("erreur") || actionLower.contains("échec") || actionLower.contains("suppression") || actionLower.contains("refus")) {
            lblGravite.setText("CRITIQUE");
            lblGravite.getStyleClass().removeAll("tag-blue", "tag-orange");
            lblGravite.getStyleClass().add("tag-red");

            lblStatutVerification.setText("⚠ Alerte Système");
            lblStatutVerification.getStyleClass().removeAll("stat-value-success");
            lblStatutVerification.getStyleClass().add("stat-value-danger");
        } else if (actionLower.contains("modification") || actionLower.contains("renommage") || actionLower.contains("mise à jour")) {
            lblGravite.setText("AVERTISSEMENT");
            lblGravite.getStyleClass().removeAll("tag-blue", "tag-red");
            lblGravite.getStyleClass().add("tag-orange");

            lblStatutVerification.setText("✓ Vérifié");
        } else {
            lblGravite.setText("INFO");
            lblGravite.getStyleClass().removeAll("tag-red", "tag-orange");
            lblGravite.getStyleClass().add("tag-blue");

            lblStatutVerification.setText("✓ Vérifié");
        }
    }

    private String calculerTempsEcoule(LocalDateTime dateLog) {
        Duration duration = Duration.between(dateLog, LocalDateTime.now());
        long minutes = duration.toMinutes();
        long heures = duration.toHours();
        long jours = duration.toDays();

        if (minutes < 1) return "à l'instant";
        if (minutes < 60) return "il y a " + minutes + " minute" + (minutes > 1 ? "s" : "");
        if (heures < 24) return "il y a " + heures + " heure" + (heures > 1 ? "s" : "");
        return "il y a " + jours + " jour" + (jours > 1 ? "s" : "");
    }

    private void copierDansPressePapier() {
        if (auditLigneCourante == null) return;

        String contenu = String.format(
                "ID Log: %s\nHorodatage: %s\nAuteur: %s\nCatégorie: %s\nAction: %s\nIP: %s\nAppareil: %s\nDétails: %s",
                auditLigneCourante.idLogAudit(),
                lblHorodatage.getText(),
                auditLigneCourante.auteur(),
                auditLigneCourante.categorie(),
                auditLigneCourante.action(),
                auditLigneCourante.adresseIp(),
                auditLigneCourante.appareil(),
                auditLigneCourante.details()
        );
        Stage stage = (Stage) this.btnCopier.getScene().getWindow();
        ClipboardContent content = new ClipboardContent();
        content.putString(contenu);
        Clipboard.getSystemClipboard().setContent(content);
        ToastNotification.succes(stage, "Audit copier !");
    }

    private void copierJsonBrut() {
        if (auditLigneCourante == null) return;

        String json = String.format(
                "{\n  \"idLog\": \"%s\",\n  \"horodatage\": \"%s\",\n  \"auteur\": \"%s\",\n  \"categorie\": \"%s\",\n  \"action\": \"%s\",\n  \"ip\": \"%s\",\n  \"appareil\": \"%s\",\n  \"details\": \"%s\"\n}",
                auditLigneCourante.idLogAudit(),
                lblHorodatage.getText(),
                auditLigneCourante.auteur(),
                auditLigneCourante.categorie(),
                auditLigneCourante.action(),
                auditLigneCourante.adresseIp(),
                auditLigneCourante.appareil(),
                auditLigneCourante.details()
        );
        Stage stage = (Stage) btnCopierJson.getScene().getWindow();
        ClipboardContent content = new ClipboardContent();
        content.putString(json);
        Clipboard.getSystemClipboard().setContent(content);
        ToastNotification.succes(stage, "Audit Copier au Format Json !");
    }

    private void fermer() {
        Stage stage = (Stage) btnCloseBottom.getScene().getWindow();
        stage.close();
    }
}