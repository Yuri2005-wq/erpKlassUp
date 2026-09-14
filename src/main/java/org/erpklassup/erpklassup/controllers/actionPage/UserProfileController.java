package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class UserProfileController implements Initializable {

    // ==========================================
    // EN-TÊTE PROFIL
    // ==========================================
    @FXML private Label avatarLabel;
    @FXML private Label nomCompletLabel;
    @FXML private Label usernameLabel;
    @FXML private Label badgeStatut;
    @FXML private VBox popupContainer;

    // ==========================================
    // ONGLET 1 : MES INFORMATIONS
    // ==========================================
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private ComboBox<String> langueCombo;
    @FXML private ComboBox<String> fuseauCombo;
    @FXML private Button btnAnnulerInfo;
    @FXML private Button btnEnregistrerInfo;
    @FXML private Button btnFermer;

    // ==========================================
    // ONGLET 2 : SÉCURITÉ - MOT DE PASSE
    // ==========================================
    @FXML private PasswordField ancienMdpField;
    @FXML private PasswordField nouveauMdpField;
    @FXML private PasswordField confirmMdpField;
    @FXML private Label forceMdpLabel;
    @FXML private ProgressBar forceMdpProgress;
    @FXML private Button btnChangerMdp;

    // ==========================================
    // ONGLET 2 : 2FA
    // ==========================================
    @FXML private HBox box2FADesactive;
    @FXML private VBox boxConfiguration2FA;
    @FXML private VBox box2FAActive;
    @FXML private Label statut2FALabel;
    @FXML private ImageView qrCodeImageView;
    @FXML private TextField cleSecreteField;
    @FXML private TextField codeVerification2FAField;
    @FXML private Button btnActiver2FA;
    @FXML private Button btnCopierCle;
    @FXML private Button btnConfirmer2FA;
    @FXML private Button btnAnnuler2FA;
    @FXML private Button btnAfficherCodesSecours;
    @FXML private Button btnDesactiver2FA;

    // ==========================================
    // ONGLET 2 : SESSIONS ACTIVES
    // ==========================================
    @FXML private TableView<?> sessionsTable;
    @FXML private TableColumn<?, ?> colAppareilSession;
    @FXML private TableColumn<?, ?> colIpSession;
    @FXML private TableColumn<?, ?> colDateSession;
    @FXML private Button btnDeconnecterAutres;

    // ==========================================
    // ONGLET 3 : MES ACTIVITÉS
    // ==========================================
    @FXML private TableView<?> activitesTable;
    @FXML private TableColumn<?, ?> colDateActivite;
    @FXML private TableColumn<?, ?> colActionActivite;
    @FXML private TableColumn<?, ?> colDetailActivite;
    @FXML private TableColumn<?, ?> colIpActivite;

    // ==========================================
    // MODAL CODES DE SECOURS
    // ==========================================
    @FXML private StackPane modalCodesSecours;
    @FXML private ListView<String> listCodesSecours;
    @FXML private Button btnFermerModalCodes;
    @FXML private Button btnImprimerCodes;

    // ==========================================
    // SERVICES ET ÉTAT
    // ==========================================
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final Security2FAService security2FAService = new Security2FAService();

    private Utilisateur utilisateurCourant;
    private String tempSecret2FA;

    // ==========================================
    // INITIALISATION
    // ==========================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        utilisateurCourant = SessionManager.getInstance().getUtilisateurCourant();

        if (utilisateurCourant != null) {
            chargerDonneesProfil();
            mettreAJourAffichage2FA();
        }

        configurerEvenements();
        masquerElementsNonUtilises();
        Platform.runLater(this::appliquerClipArrondi);

    }

    private void masquerElementsNonUtilises() {
        if (forceMdpProgress != null) {
            forceMdpProgress.setVisible(false);
        }
        if (forceMdpLabel != null) {
            forceMdpLabel.setText("—");
        }
        if (badgeStatut != null && utilisateurCourant != null) {
            badgeStatut.setText(utilisateurCourant.isEstActif() ? "● Actif" : "● Inactif");
        }
    }

    private void appliquerClipArrondi() {
        if (popupContainer == null) return;

        final double RADIUS = 16;

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(RADIUS * 2);
        clip.setArcHeight(RADIUS * 2);
        clip.widthProperty().bind(popupContainer.widthProperty());
        clip.heightProperty().bind(popupContainer.heightProperty());

        popupContainer.setClip(clip);
    }

    @FXML
    private void handleFermer() {
        Stage stage = getStage();
        if (stage != null) {
            stage.close();
        }
    }


    private String convertirLangueEnCode(String libelle) {
        if (libelle == null) return "FR";
        return switch (libelle) {
            case "Français" -> "FR";
            case "English"  -> "EN";
            default         -> "FR";
        };
    }

    /**
     * Convertit un libellé affiché ("UTC+01:00 (Afrique Centrale)") en code BDD ("Africa/Douala").
     */
    private String convertirFuseauEnCode(String libelle) {
        if (libelle == null) return "Africa/Douala";
        return switch (libelle) {
            case "UTC+01:00 (Afrique Centrale)" -> "Africa/Douala";
            case "UTC+00:00 (GMT)"              -> "UTC";
            default                             -> "Africa/Douala";
        };
    }




    private void enregistrerInformations() {
        if (utilisateurCourant == null) return;

        utilisateurCourant.setNom(safeText(nomField));
        utilisateurCourant.setPrenom(safeText(prenomField));
        utilisateurCourant.setEmail(safeText(emailField));
        utilisateurCourant.setTelephone(safeText(telephoneField));

        // ✅ AJOUT : lire les ComboBox
        if (langueCombo != null && langueCombo.getValue() != null) {
            utilisateurCourant.setLanguePreference(convertirLangueEnCode(langueCombo.getValue()));
        }
        if (fuseauCombo != null && fuseauCombo.getValue() != null) {
            utilisateurCourant.setFuseauHoraire(convertirFuseauEnCode(fuseauCombo.getValue()));
        }

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() {
                utilisateurDAO.update(utilisateurCourant);
                return null;
            }
        };


        saveTask.setOnSucceeded(evt ->
                ToastNotification.succes(getStage(), "Informations mises à jour avec succès !"));
        saveTask.setOnFailed(evt -> ToastNotification.erreur(getStage(),
                "Erreur lors de la mise à jour."));

        AppExecutor.get().submit(saveTask);
    }

    private void chargerDonneesProfil() {
        if (utilisateurCourant == null) return;

        nomCompletLabel.setText(utilisateurCourant.getNomComplet());
        usernameLabel.setText("@" + utilisateurCourant.getUsername());
        avatarLabel.setText(obtenirInitiales(utilisateurCourant.getNomComplet()));

        nomField.setText(utilisateurCourant.getNom());
        prenomField.setText(utilisateurCourant.getPrenom());
        emailField.setText(utilisateurCourant.getEmail());
        telephoneField.setText(utilisateurCourant.getTelephone());

        // ✅ ComboBox Langue
        if (langueCombo != null) {
            langueCombo.setItems(FXCollections.observableArrayList("Français", "English"));
            langueCombo.setValue(convertirCodeEnLangue(utilisateurCourant.getLanguePreference()));
        }

        // ✅ ComboBox Fuseau
        if (fuseauCombo != null) {
            fuseauCombo.setItems(FXCollections.observableArrayList(
                    "UTC+01:00 (Afrique Centrale)", "UTC+00:00 (GMT)"));
            fuseauCombo.setValue(convertirCodeEnFuseau(utilisateurCourant.getFuseauHoraire()));
        }
    }

    private String convertirCodeEnLangue(String code) {
        if (code == null) return "Français";
        return switch (code) {
            case "EN" -> "English";
            default   -> "Français";
        };
    }

    private String convertirCodeEnFuseau(String code) {
        if (code == null) return "UTC+01:00 (Afrique Centrale)";
        return switch (code) {
            case "UTC" -> "UTC+00:00 (GMT)";
            default    -> "UTC+01:00 (Afrique Centrale)";
        };
    }

    /**
     * Convertit un libellé affiché ("Français") en code BDD ("FR").
     */


    private void configurerEvenements() {
        // Onglet 1 - Informations
        if (btnEnregistrerInfo != null) {
            btnEnregistrerInfo.setOnAction(e -> enregistrerInformations());
        }
        if (btnAnnulerInfo != null) {
            btnAnnulerInfo.setOnAction(e -> chargerDonneesProfil());
        }

        // Onglet 2 - Mot de passe
        if (btnChangerMdp != null) {
            btnChangerMdp.setOnAction(e -> changerMotDePasse());
        }

        // Onglet 2 - 2FA
        if (btnActiver2FA != null) {
            btnActiver2FA.setOnAction(e -> initierActivation2FA());
        }
        if (btnCopierCle != null) {
            btnCopierCle.setOnAction(e -> copierCleSecrete());
        }
        if (btnConfirmer2FA != null) {
            btnConfirmer2FA.setOnAction(e -> validerEtActiver2FA());
        }
        if (btnAnnuler2FA != null) {
            btnAnnuler2FA.setOnAction(e -> annulerActivation2FA());
        }
        if (btnDesactiver2FA != null) {
            btnDesactiver2FA.setOnAction(e -> desactiver2FA());
        }
        if (btnAfficherCodesSecours != null) {
            btnAfficherCodesSecours.setOnAction(e -> afficherModalCodesSecours());
        }

        // Modal codes secours
        if (btnFermerModalCodes != null && modalCodesSecours != null) {
            btnFermerModalCodes.setOnAction(e -> {
                modalCodesSecours.setVisible(false);
                modalCodesSecours.setManaged(false);
            });
        }
        if (btnImprimerCodes != null) {
            btnImprimerCodes.setOnAction(e ->
                    ToastNotification.info(getStage(), "Fonction d'impression en cours..."));
        }
    }

    // ==========================================
    // LOGIQUE 2FA
    // ==========================================

    private void mettreAJourAffichage2FA() {
        if (utilisateurCourant == null) return;

        boolean estActive = utilisateurCourant.isDeuxFacteursActive();

        if (statut2FALabel != null) {
            statut2FALabel.setText(estActive ? "Activée" : "Non activée");
            statut2FALabel.setStyle(estActive
                    ? "-fx-font-weight: bold; -fx-text-fill: #10B981;"
                    : "-fx-font-weight: bold; -fx-text-fill: #94A3B8;");
        }

        if (box2FADesactive != null) {
            box2FADesactive.setVisible(!estActive);
            box2FADesactive.setManaged(!estActive);
        }

        if (boxConfiguration2FA != null) {
            boxConfiguration2FA.setVisible(false);
            boxConfiguration2FA.setManaged(false);
        }

        if (box2FAActive != null) {
            box2FAActive.setVisible(estActive);
            box2FAActive.setManaged(estActive);
        }
    }

    private void initierActivation2FA() {
        if (utilisateurCourant == null) return;

        // 1. Générer la clé secrète
        tempSecret2FA = security2FAService.genererCleSecrete();
        cleSecreteField.setText(tempSecret2FA);

        // 2. Générer + afficher le QR code
        Image qr = security2FAService.genererQrCode(
                utilisateurCourant.getUsername(),
                "KlassUp",
                tempSecret2FA
        );

        if (qr != null && qrCodeImageView != null) {
            qrCodeImageView.setImage(qr);
        } else {
            ToastNotification.avertissement(getStage(),
                    "QR code non disponible — utilisez la clé manuelle.");
        }

        // 3. Basculer l'affichage
        box2FADesactive.setVisible(false);
        box2FADesactive.setManaged(false);

        boxConfiguration2FA.setVisible(true);
        boxConfiguration2FA.setManaged(true);

        box2FAActive.setVisible(false);
        box2FAActive.setManaged(false);

        if (codeVerification2FAField != null) {
            codeVerification2FAField.requestFocus();
        }
    }

    private void copierCleSecrete() {
        if (cleSecreteField == null) return;
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(cleSecreteField.getText());
        clipboard.setContent(content);
        ToastNotification.info(getStage(), "Clé secrète copiée dans le presse-papier !");
    }

    private void validerEtActiver2FA() {
        if (utilisateurCourant == null) return;

        String codeSaisi = codeVerification2FAField != null
                && codeVerification2FAField.getText() != null
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
        if (codeVerification2FAField != null) {
            codeVerification2FAField.clear();
        }
        if (qrCodeImageView != null) {
            qrCodeImageView.setImage(null);
        }
        mettreAJourAffichage2FA();
    }

    private void desactiver2FA() {
        if (utilisateurCourant == null) return;

        boolean confirmation = AlertUtil.afficherConfirmation(
                "Désactivation de la 2FA",
                "Voulez-vous vraiment désactiver la double authentification ?",
                getStage()
        );

        if (!confirmation) return;

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
            ToastNotification.avertissement(getStage(),
                    "Double authentification désactivée.");
        });

        taskDesactiver.setOnFailed(evt -> ToastNotification.erreur(getStage(),
                "Erreur lors de la désactivation."));

        AppExecutor.get().submit(taskDesactiver);
    }

    private void afficherModalCodesSecours() {
        if (utilisateurCourant == null) return;

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
        if (listCodesSecours == null || modalCodesSecours == null) return;

        listCodesSecours.setItems(FXCollections.observableArrayList(codes));
        modalCodesSecours.setVisible(true);
        modalCodesSecours.setManaged(true);
    }

    // ==========================================
    // MODIFICATION DE PROFIL
    // ==========================================



    private void changerMotDePasse() {
        if (utilisateurCourant == null) return;

        String ancienMdp = ancienMdpField != null ? ancienMdpField.getText() : null;
        String nouveauMdp = nouveauMdpField != null ? nouveauMdpField.getText() : null;
        String confirmMdp = confirmMdpField != null ? confirmMdpField.getText() : null;

        if (ancienMdp == null || nouveauMdp == null || confirmMdp == null
                || ancienMdp.isEmpty() || nouveauMdp.isEmpty() || confirmMdp.isEmpty()) {
            ToastNotification.erreur(getStage(), "Tous les champs sont obligatoires.");
            return;
        }

        if (!nouveauMdp.equals(confirmMdp)) {
            ToastNotification.erreur(getStage(),
                    "Le nouveau mot de passe et la confirmation ne correspondent pas.");
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

        changeMdpTask.setOnFailed(evt -> ToastNotification.erreur(getStage(),
                "Erreur lors du changement de mot de passe."));

        return changeMdpTask;
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private String safeText(TextField field) {
        if (field == null || field.getText() == null) return "";
        return field.getText().trim();
    }

    private Stage getStage() {
        if (nomCompletLabel == null || nomCompletLabel.getScene() == null) return null;
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