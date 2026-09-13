package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.erpklassup.erpklassup.dao.UtilisateurDAO;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.service.AppExecutor;
import org.erpklassup.erpklassup.service.PasswordService;
import org.erpklassup.erpklassup.service.Security2FAService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.AlertUtil;
import org.erpklassup.erpklassup.util.ToastNotification;
import org.erpklassup.erpklassup.util.TotpUtil;

import java.util.List;

public class UserProfileController {

    // En-tête Profil
    @FXML private Label avatarLabel;
    @FXML private Label nomCompletLabel;
    @FXML private Label usernameLabel;
    @FXML private Label badgeStatut;

    // Onglet 1 : Mes informations
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private ComboBox<String> langueCombo;
    @FXML private ComboBox<String> fuseauCombo;
    @FXML private Button btnAnnulerInfo;
    @FXML private Button btnEnregistrerInfo;

    // Onglet 2 : Sécurité - Mot de Passe
    @FXML private PasswordField ancienMdpField;
    @FXML private PasswordField nouveauMdpField;
    @FXML private PasswordField confirmMdpField;
    @FXML private Label forceMdpLabel;
    @FXML private ProgressBar forceMdpProgress;
    @FXML private Button btnChangerMdp;

    // Onglet 2 : Sécurité - Double Authentification (2FA)
    @FXML private HBox box2FADesactive;
    @FXML private VBox boxConfiguration2FA;
    @FXML private VBox box2FAActive;
    @FXML private Label statut2FALabel;
    @FXML private TextField cleSecreteField;
    @FXML private TextField codeVerification2FAField;
    @FXML private Button btnActiver2FA;
    @FXML private Button btnCopierCle;
    @FXML private Button btnConfirmer2FA;
    @FXML private Button btnAnnuler2FA;
    @FXML private Button btnAfficherCodesSecours;
    @FXML private Button btnDesactiver2FA;

    // Modal - Codes de secours
    @FXML private StackPane modalCodesSecours;
    @FXML private ListView<String> listCodesSecours;
    @FXML private Button btnFermerModalCodes;
    @FXML private Button btnImprimerCodes;

    // Services et variables d'état
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private Utilisateur utilisateurCourant;
    private String tempSecret2FA;
    private final Security2FAService security2FAService = new Security2FAService();   // ✅ AJOUTER CETTE LIGNE


    @FXML
    public void initialize() {
        utilisateurCourant = SessionManager.getInstance().getUtilisateurCourant();

        if (utilisateurCourant != null) {
            chargerDonneesProfil();
            mettreAJourAffichage2FA();
        }

        configurerEvenements();
        masquerElementsNonUtilises();
    }

    private void masquerElementsNonUtilises() {
        if (forceMdpProgress != null) forceMdpProgress.setVisible(false);
        if (forceMdpLabel != null) forceMdpLabel.setText("—");
        if (badgeStatut != null && utilisateurCourant != null) {
            badgeStatut.setText(utilisateurCourant.isEstActif() ? "● Actif" : "● Inactif");
        }
    }

    private void chargerDonneesProfil() {
        nomCompletLabel.setText(utilisateurCourant.getNomComplet());
        usernameLabel.setText(utilisateurCourant.getUsername());
        avatarLabel.setText(obtenirInitiales(utilisateurCourant.getNomComplet()));

        nomField.setText(utilisateurCourant.getNom());
        prenomField.setText(utilisateurCourant.getPrenom());
        emailField.setText(utilisateurCourant.getEmail());
        telephoneField.setText(utilisateurCourant.getTelephone());

        langueCombo.setItems(FXCollections.observableArrayList("Français", "English"));
        langueCombo.setValue("Français");

        fuseauCombo.setItems(FXCollections.observableArrayList("UTC+01:00 (Afrique Centrale)", "UTC+00:00 (GMT)"));
        fuseauCombo.setValue("UTC+01:00 (Afrique Centrale)");
    }

    private void configurerEvenements() {
        // Informations
        btnEnregistrerInfo.setOnAction(evt -> enregistrerInformations());
        btnAnnulerInfo.setOnAction(evt -> chargerDonneesProfil());

        // Mot de passe
        btnChangerMdp.setOnAction(evt -> changerMotDePasse());

        // Double Authentification (2FA)
        btnActiver2FA.setOnAction(evt -> initierActivation2FA());
        btnCopierCle.setOnAction(evt -> copierCleSecrete());
        btnConfirmer2FA.setOnAction(evt -> validerEtActiver2FA());
        btnAnnuler2FA.setOnAction(evt -> annulerActivation2FA());
        btnDesactiver2FA.setOnAction(evt -> desactiver2FA());
        btnAfficherCodesSecours.setOnAction(evt -> afficherModalCodesSecours());
        btnFermerModalCodes.setOnAction(evt -> modalCodesSecours.setVisible(false));
        btnImprimerCodes.setOnAction(evt -> ToastNotification.info(getStage(), "Fonction d'impression en cours..."));
    }

    // =========================================================================
    // LOGIQUE WORKFLOW 2FA
    // =========================================================================

    private void mettreAJourAffichage2FA() {
        boolean estActive = utilisateurCourant.isDeuxFacteursActive();

        statut2FALabel.setText(estActive ? "Activée" : "Non activée");
        statut2FALabel.setStyle(estActive ? "-fx-font-weight: bold; -fx-text-fill: #10B981;" : "-fx-font-weight: bold; -fx-text-fill: #94A3B8;");

        box2FADesactive.setVisible(!estActive);
        box2FADesactive.setManaged(!estActive);

        boxConfiguration2FA.setVisible(false);
        boxConfiguration2FA.setManaged(false);

        box2FAActive.setVisible(estActive);
        box2FAActive.setManaged(estActive);
    }

    private void initierActivation2FA() {
        tempSecret2FA = TotpUtil.genererSecret();
        cleSecreteField.setText(tempSecret2FA);

        box2FADesactive.setVisible(false);
        box2FADesactive.setManaged(false);

        boxConfiguration2FA.setVisible(true);
        boxConfiguration2FA.setManaged(true);
    }

    private void copierCleSecrete() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(cleSecreteField.getText());
        clipboard.setContent(content);
        ToastNotification.info(getStage(), "Clé secrète copiée dans le presse-papier !");
    }

    private void validerEtActiver2FA() {
        String codeSaisi = codeVerification2FAField.getText() != null
                ? codeVerification2FAField.getText().trim() : "";

        if (codeSaisi.length() != 6) {
            ToastNotification.erreur(getStage(), "Veuillez saisir un code à 6 chiffres.");
            return;
        }

        try {
            int codeInt = Integer.parseInt(codeSaisi);

            if (!security2FAService.verifierCodeTOTP(tempSecret2FA, codeInt)) {
                ToastNotification.erreur(getStage(),
                        "Code invalide. Vérifiez l'heure de votre téléphone.");
                return;
            }

            Task<List<String>> task2FA = new Task<>() {
                @Override
                protected List<String> call() {
                    // 1. Activer la 2FA en BDD
                    utilisateurDAO.activer2FA(
                            utilisateurCourant.getIdUtilisateur(),
                            tempSecret2FA
                    );

                    // 2. Générer + enregistrer les codes HACHÉS
                    return security2FAService.genererCodesSecours(
                            utilisateurCourant.getIdUtilisateur(), 8
                    );
                }
            };

            task2FA.setOnSucceeded(evt -> {
                utilisateurCourant.setDeuxFacteursActive(true);
                utilisateurCourant.setSecret2FA(tempSecret2FA);
                codeVerification2FAField.clear();

                mettreAJourAffichage2FA();
                ToastNotification.succes(getStage(), "Double authentification activée !");

                // Afficher les codes EN CLAIR (une seule fois)
                afficherCodesSecours(task2FA.getValue());
            });

            task2FA.setOnFailed(evt -> {
                Throwable ex = task2FA.getException();
                ToastNotification.erreur(getStage(),
                        "Erreur : " + (ex != null ? ex.getMessage() : "inconnue"));
            });

            AppExecutor.get().submit(task2FA);

        } catch (NumberFormatException e) {
            ToastNotification.erreur(getStage(), "Le code doit être numérique.");
        }
    }
    private void annulerActivation2FA() {
        tempSecret2FA = null;
        codeVerification2FAField.clear();
        mettreAJourAffichage2FA();
    }

    private void desactiver2FA() {
        boolean confirmation = AlertUtil.afficherConfirmation(
                "Désactivation de la 2FA",
                "Voulez-vous vraiment désactiver la double authentification ?",
                getStage()
        );

        if (confirmation) {
            Task<Void> taskDesactiver = new Task<>() {
                @Override
                protected Void call() {
                    utilisateurDAO.desactiver2FA(utilisateurCourant.getIdUtilisateur());
                    return null;
                }
            };

            taskDesactiver.setOnSucceeded(evt -> {
                utilisateurCourant.setDeuxFacteursActive(false);
                utilisateurCourant.setSecret2FA(null);
                mettreAJourAffichage2FA();
                ToastNotification.avertissement(getStage(), "Double authentification désactivée.");
            });

            AppExecutor.get().submit(taskDesactiver);
        }
    }

    private void afficherModalCodesSecours() {
        // ⚠️ Impossible d'afficher les codes en clair : ils sont HACHÉS en BDD.
        // On propose à l'utilisateur de RÉGÉNÉRER un nouveau lot.
        boolean confirmation = AlertUtil.afficherConfirmation(
                "Codes de secours",
                "Vos anciens codes de secours ne peuvent plus être affichés (ils sont hachés).\n\n"
                        + "Voulez-vous générer un NOUVEAU lot de 8 codes ?\n"
                        + "Les anciens seront supprimés.",
                getStage()
        );

        if (!confirmation) return;

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                return security2FAService.genererCodesSecours(
                        utilisateurCourant.getIdUtilisateur(), 8
                );
            }
        };

        task.setOnSucceeded(evt -> {
            ToastNotification.succes(getStage(), "Nouveaux codes générés.");
            afficherCodesSecours(task.getValue());
        });

        task.setOnFailed(evt -> ToastNotification.erreur(getStage(),
                "Erreur lors de la génération."));

        AppExecutor.get().submit(task);
    }

    private void afficherCodesSecours(List<String> codes) {
        listCodesSecours.setItems(FXCollections.observableArrayList(codes));
        modalCodesSecours.setVisible(true);
        modalCodesSecours.setManaged(true);
    }

    // =========================================================================
    // MODIFICATION DE PROFIL ET MOT DE PASSE
    // =========================================================================

    private void enregistrerInformations() {
        utilisateurCourant.setNom(nomField.getText().trim());
        utilisateurCourant.setPrenom(prenomField.getText().trim());
        utilisateurCourant.setEmail(emailField.getText().trim());
        utilisateurCourant.setTelephone(telephoneField.getText().trim());

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() {
                utilisateurDAO.update(utilisateurCourant);
                return null;
            }
        };

        saveTask.setOnSucceeded(evt -> ToastNotification.succes(getStage(), "Informations mises à jour avec succès !"));
        AppExecutor.get().submit(saveTask);
    }

    private void changerMotDePasse() {
        String ancienMdp = ancienMdpField.getText();
        String nouveauMdp = nouveauMdpField.getText();
        String confirmMdp = confirmMdpField.getText();

        if (ancienMdp.isEmpty() || nouveauMdp.isEmpty() || confirmMdp.isEmpty()) {
            ToastNotification.erreur(getStage(), "Tous les champs sont obligatoires.");
            return;
        }

        if (!nouveauMdp.equals(confirmMdp)) {
            ToastNotification.erreur(getStage(), "Le nouveau mot de passe et la confirmation ne correspondent pas.");
            return;
        }

        if (!PasswordService.verifier(ancienMdp, utilisateurCourant.getPasswordHash())) {
            ToastNotification.erreur(getStage(), "Mot de passe actuel incorrect.");
            return;
        }

        Task<Void> task = executerChangementMotDePasse(nouveauMdp);
        AppExecutor.get().submit(task);
    }

    private Task<Void> executerChangementMotDePasse(String nouveauMdp) {
        Task<Void> changeMdpTask = new Task<>() {
            @Override
            protected Void call() {
                String hash = PasswordService.hacher(nouveauMdp);
                utilisateurDAO.updatePassword(utilisateurCourant.getIdUtilisateur(), hash);
                utilisateurCourant.setPasswordHash(hash);
                return null;
            }
        };

        changeMdpTask.setOnSucceeded(evt -> {
            ancienMdpField.clear();
            nouveauMdpField.clear();
            confirmMdpField.clear();
            ToastNotification.succes(getStage(), "Mot de passe modifié avec succès !");
        });

        return changeMdpTask;
    }

    private Stage getStage() {
        return (Stage) nomCompletLabel.getScene().getWindow();
    }

    private String obtenirInitiales(String nomComplet) {
        if (nomComplet == null || nomComplet.isBlank()) return "U";
        String[] parts = nomComplet.trim().split("\\s+");
        if (parts.length >= 2) {
            return (String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)).toUpperCase();
        }
        return nomComplet.substring(0, Math.min(2, nomComplet.length())).toUpperCase();
    }
}