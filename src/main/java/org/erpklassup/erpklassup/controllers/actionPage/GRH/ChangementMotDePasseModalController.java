package org.erpklassup.erpklassup.controllers.actionPage.GRH;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.dao.UtilisateurDAO;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.service.PasswordService;
import org.erpklassup.erpklassup.util.ToastNotification;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

public class ChangementMotDePasseModalController implements Initializable {

    /* ============================================================
       FXML — CHAMPS MASQUÉS (PasswordField)
       ============================================================ */
    @FXML private PasswordField txtAncienMdp;
    @FXML private PasswordField txtNouveauMdp;
    @FXML private PasswordField txtConfirmMdp;

    /* ============================================================
       FXML — CHAMPS VISIBLES (TextField)
       ============================================================ */
    @FXML private TextField txtAncienMdpVisible;
    @FXML private TextField txtNouveauMdpVisible;
    @FXML private TextField txtConfirmMdpVisible;

    /* ============================================================
       FXML — ICÔNES DES BOUTONS
       ============================================================ */
    @FXML private FontIcon iconAncien;
    @FXML private FontIcon iconNouveau;
    @FXML private FontIcon iconConfirm;

    /* ============================================================
       FXML — AUTRES
       ============================================================ */
    @FXML private Label errGlobal;
    @FXML private Button btnValider;

    /* ============================================================
       ÉTAT
       ============================================================ */
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private Utilisateur utilisateur;
    private Runnable onSucces;

    /* ============================================================
       INITIALISATION
       ============================================================ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Empêche la fermeture par croix / Alt+F4
        Platform.runLater(() -> {
            Stage stage = getStage();
            if (stage != null) {
                stage.setOnCloseRequest(event -> {
                    event.consume();
                    afficherErreur("Vous devez changer votre mot de passe avant de continuer.");
                });
            }
        });
    }

    public void initData(Utilisateur user, Runnable onSucces) {
        this.utilisateur = user;
        this.onSucces = onSucces;
        Platform.runLater(() -> txtAncienMdp.requestFocus());
    }

    /* ============================================================
       BASCULES ŒIL / ŒIL FERMÉ
       ============================================================ */

    @FXML
    private void handleToggleAncien(ActionEvent event) {
        basculerVisibilite(
                txtAncienMdp, txtAncienMdpVisible,
                iconAncien
        );
    }

    @FXML
    private void handleToggleNouveau(ActionEvent event) {
        basculerVisibilite(
                txtNouveauMdp, txtNouveauMdpVisible,
                iconNouveau
        );
    }

    @FXML
    private void handleToggleConfirm(ActionEvent event) {
        basculerVisibilite(
                txtConfirmMdp, txtConfirmMdpVisible,
                iconConfirm
        );
    }

    /**
     * Bascule entre le PasswordField (masqué) et le TextField (visible).
     * Synchronise le texte dans les deux sens et change l'icône du bouton.
     */
    private void basculerVisibilite(PasswordField masque, TextField visible, FontIcon icone) {
        if (visible.isVisible()) {
            // On repasse en mode masqué
            masque.setText(visible.getText());
            visible.setVisible(false);
            visible.setManaged(false);
            masque.setVisible(true);
            masque.setManaged(true);
            masque.requestFocus();
            masque.positionCaret(masque.getText().length());
            icone.setIconLiteral("fth-eye");
        } else {
            // On passe en mode visible
            visible.setText(masque.getText());
            visible.setVisible(true);
            visible.setManaged(true);
            masque.setVisible(false);
            masque.setManaged(false);
            visible.requestFocus();
            visible.positionCaret(visible.getText().length());
            icone.setIconLiteral("fth-eye-off");
        }
    }

    /**
     * Retourne le texte saisi en cherchant dans le champ visible en priorité,
     * sinon dans le champ masqué. Cela garantit qu'on récupère toujours la
     * bonne valeur, quel que soit le mode actif.
     */
    private String getValeur(PasswordField masque, TextField visible) {
        return visible.isVisible() ? visible.getText() : masque.getText();
    }

    /* ============================================================
       VALIDATION
       ============================================================ */

    @FXML
    private void handleValider(ActionEvent event) {
        masquerErreur();

        String ancien = getValeur(txtAncienMdp, txtAncienMdpVisible);
        String nouveau = getValeur(txtNouveauMdp, txtNouveauMdpVisible);
        String confirm = getValeur(txtConfirmMdp, txtConfirmMdpVisible);

        if (ancien == null || ancien.isBlank()) {
            afficherErreur("Veuillez saisir votre mot de passe actuel.");
            return;
        }
        if (nouveau == null || nouveau.isBlank()) {
            afficherErreur("Veuillez saisir un nouveau mot de passe.");
            return;
        }
        if (!nouveau.equals(confirm)) {
            afficherErreur("Les deux mots de passe ne correspondent pas.");
            return;
        }
        if (!PasswordService.verifier(ancien, utilisateur.getPasswordHash())) {
            afficherErreur("Le mot de passe actuel est incorrect.");
            return;
        }
        if (!estMotDePasseValide(nouveau)) {
            afficherErreur("Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule et un chiffre.");
            return;
        }
        if (PasswordService.verifier(nouveau, utilisateur.getPasswordHash())) {
            afficherErreur("Le nouveau mot de passe doit être différent de l'ancien.");
            return;
        }

        btnValider.setDisable(true);
        String nouveauHash = PasswordService.hacher(nouveau);
        boolean ok = utilisateurDAO.updatePassword(utilisateur.getIdUtilisateur(), nouveauHash);

        if (ok) {
            utilisateur.setPasswordHash(nouveauHash);
            utilisateur.setDoitChangerMotDePasse(false);

            Stage stage = getStage();
            if (stage != null) stage.setOnCloseRequest(null);

            ToastNotification.succes(stage, "Mot de passe changé avec succès !");
            if (onSucces != null) onSucces.run();
            fermer();
        } else {
            btnValider.setDisable(false);
            afficherErreur("Erreur lors de la mise à jour. Veuillez réessayer.");
        }
    }

    private boolean estMotDePasseValide(String mdp) {
        if (mdp == null || mdp.length() < 8) return false;
        boolean aMaj = false, aMin = false, aChiffre = false;
        for (char c : mdp.toCharArray()) {
            if (Character.isUpperCase(c)) aMaj = true;
            else if (Character.isLowerCase(c)) aMin = true;
            else if (Character.isDigit(c)) aChiffre = true;
        }
        return aMaj && aMin && aChiffre;
    }

    private void afficherErreur(String msg) {
        errGlobal.setText(msg);
        errGlobal.setVisible(true);
        errGlobal.setManaged(true);
    }

    private void masquerErreur() {
        errGlobal.setVisible(false);
        errGlobal.setManaged(false);
    }

    private Stage getStage() {
        return (Stage) btnValider.getScene().getWindow();
    }

    private void fermer() {
        getStage().close();
    }
}