package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import org.erpklassup.erpklassup.dto.PersonneDTO;
import org.erpklassup.erpklassup.dto.RoleOption;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.UtilisateurService;
import org.erpklassup.erpklassup.util.ToastNotification;

import java.net.URL;
import java.util.ResourceBundle;

public class UserCreateController implements Initializable {

    // ===== FXID HEADER & STEPPER =====
    @FXML private Button btnFermer;
    @FXML private Circle circleEtape1, circleEtape2, circleEtape3;
    @FXML private Label numEtape1, numEtape2, numEtape3;
    @FXML private Label labelEtape1, labelEtape2, labelEtape3;

    // ===== FXID ÉTAPES =====
    @FXML private ScrollPane scrollEtape1, scrollEtape2, scrollEtape3;
    @FXML private VBox etape1, etape2, etape3;
    @FXML private VBox cardPersonnel, cardEleve, cardParent, cardPromoteur;
    @FXML private Label infoTypeLabel;

    // ===== FXID ÉTAPE 2 : RECHERCHE =====
    @FXML private VBox sectionRecherche, sectionSaisieDirecte;
    @FXML private Label titreRechercheLabel, instructionRechercheLabel, infoSelectionLabel;
    @FXML private TextField searchPersonneField;
    @FXML private Button btnRechercher;
    @FXML private TableView<PersonneDTO> tableResultats;
    @FXML private TableColumn<PersonneDTO, String> colMatricule, colNom, colPrenom, colInfoSup;

    // ===== FXID ÉTAPE 2 : SAISIE DIRECTE =====
    @FXML private TextField nomDirectField, prenomDirectField, telephoneDirectField;
    @FXML private ComboBox<String> roleSystemeCombo;

    // ===== FXID ÉTAPE 3 : IDENTIFIANTS =====
    @FXML private Label recapNomLabel, recapTypeLabel, recapIdMetierLabel;
    @FXML private TextField usernameField, emailField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private TextField passwordTextField, confirmPasswordTextField; // Pour le mode afficher mdp
    @FXML private Button btnTogglePassword;
    @FXML private Label erreurUsername, erreurEmail, erreurConfirmMdp, forceMdpLabel;
    @FXML private ProgressBar forceMdpProgress;
    @FXML private CheckBox doitChangerMdpCheckBox, emailVerifieCheckBox;

    // NOUVEAU : CheckBox pour mot de passe par défaut
    @FXML private CheckBox useDefaultPasswordCheckBox;

    // ===== FXID FOOTER =====
    @FXML private Button btnPrecedent, btnSuivant, btnAnnuler, btnCreer;

    // ===== SERVICES & VARIABLES D'ÉTAT =====
    private final UtilisateurService utilisateurService = new UtilisateurService();
    private final AuditService auditService = new AuditService();

    private static final Color COULEUR_ACTIVE = Color.web("#F5A524");
    private static final Color COULEUR_INACTIVE = Color.web("#2A3B61");

    private int etapeActuelle = 1;
    private String typeCompteSelectionne = ""; // PERSONNEL, ELEVE, PARENT, PROMOTEUR
    private PersonneDTO personneSelectionnee = null;
    private String idEcoleCourante;
    private Runnable callbackSuccess;
    private boolean mdpVisible = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();

        configurerCartesEtape1();
        configurerTableau();
        configurerNavigation();
        configurerMotDePasse();
        configurerChampsDirects();
        mettreAJourAffichageEtape();
    }

    public void initData(String idEcoleCourante, ObservableList<RoleOption> items, Runnable onSuccess) {
        this.callbackSuccess = onSuccess;
    }

    // -------------------------------------------------------------
    // 1. ÉTAPE 1 (SÉLECTION DU TYPE DE COMPTE)
    // -------------------------------------------------------------
    private void configurerCartesEtape1() {
        cardPersonnel.setOnMouseClicked(e -> selectionnerType("PERSONNEL", cardPersonnel, "Sélectionnez un membre du personnel actif n'ayant pas encore de compte utilisateur."));
        cardEleve.setOnMouseClicked(e -> selectionnerType("ELEVE", cardEleve, "Sélectionnez un élève inscrit pour lui ouvrir un accès au portail."));
        cardParent.setOnMouseClicked(e -> selectionnerType("PARENT", cardParent, "Sélectionnez un parent d'élève pour lui donner accès au suivi."));
        cardPromoteur.setOnMouseClicked(e -> selectionnerType("PROMOTEUR", cardPromoteur, "Création directe d'un compte Administrateur / Promoteur sans fiche préalable."));
    }

    private void selectionnerType(String type, VBox cardTarget, String description) {
        this.typeCompteSelectionne = type;

        cardPersonnel.getStyleClass().remove("card-clickable-active");
        cardEleve.getStyleClass().remove("card-clickable-active");
        cardParent.getStyleClass().remove("card-clickable-active");
        cardPromoteur.getStyleClass().remove("card-clickable-active");
        cardTarget.getStyleClass().add("card-clickable-active");

        if (infoTypeLabel != null) {
            infoTypeLabel.setText(description);
            infoTypeLabel.setVisible(true);
            infoTypeLabel.setManaged(true);
        }
    }

    // -------------------------------------------------------------
    // 2. ÉTAPE 2 (RECHERCHE & SAISIE)
    // -------------------------------------------------------------
    private void configurerTableau() {
        colMatricule.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMatricule()));
        colNom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNom()));
        colPrenom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPrenom()));
        colInfoSup.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getInfoSup()));

        tableResultats.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            this.personneSelectionnee = newVal;
            if (newVal != null && infoSelectionLabel != null) {
                infoSelectionLabel.setText("Sélectionné : " + newVal.getNom() + " " + newVal.getPrenom() + " (" + newVal.getMatricule() + ")");
                infoSelectionLabel.setVisible(true);
                infoSelectionLabel.setManaged(true);
            }
        });

        btnRechercher.setOnAction(e -> effectuerRechercheAsync());
        searchPersonneField.setOnAction(e -> effectuerRechercheAsync());
    }

    private void configurerChampsDirects() {
        roleSystemeCombo.setItems(FXCollections.observableArrayList("ADMINISTRATEUR", "PROMOTEUR", "SUPER_ADMIN"));
        roleSystemeCombo.getSelectionModel().selectFirst();
    }

    private void effectuerRechercheAsync() {
        String query = searchPersonneField.getText().trim();
        tableResultats.setPlaceholder(new Label("Recherche en cours..."));

        Task<ObservableList<PersonneDTO>> task = new Task<>() {
            @Override
            protected ObservableList<PersonneDTO> call() throws Exception {
                return utilisateurService.rechercherPersonnesSansCompte(idEcoleCourante, typeCompteSelectionne, query);
            }
        };

        task.setOnSucceeded(e -> {
            tableResultats.setItems(task.getValue());
            if (task.getValue().isEmpty()) {
                tableResultats.setPlaceholder(new Label("Aucune personne disponible trouvée."));
            }
        });

        task.setOnFailed(e -> {
            tableResultats.setPlaceholder(new Label("Erreur lors de la recherche."));
            ToastNotification.erreur(getStage(), "Erreur de chargement des fiches.");
        });

        new Thread(task).start();
    }

    // -------------------------------------------------------------
    // 3. NAVIGATION ET STEPPER
    // -------------------------------------------------------------
    private void configurerNavigation() {
        btnSuivant.setOnAction(e -> avancerEtape());
        btnPrecedent.setOnAction(e -> reculerEtape());
        btnAnnuler.setOnAction(e -> fermerFenetre());
        if (btnFermer != null) btnFermer.setOnAction(e -> fermerFenetre());
        btnCreer.setOnAction(e -> enregistrerUtilisateurAsync());
    }

    private void avancerEtape() {
        if (etapeActuelle == 1) {
            if (typeCompteSelectionne.isEmpty()) {
                ToastNotification.avertissement(getStage(), "Veuillez choisir un type de compte.");
                return;
            }
            preparerEtape2();
            etapeActuelle = 2;
        } else if (etapeActuelle == 2) {
            if ("PROMOTEUR".equals(typeCompteSelectionne)) {
                if (nomDirectField.getText().isBlank() || prenomDirectField.getText().isBlank()) {
                    ToastNotification.avertissement(getStage(), "Veuillez renseigner au moins le nom et le prénom.");
                    return;
                }
            } else if (personneSelectionnee == null) {
                ToastNotification.avertissement(getStage(), "Veuillez sélectionner une personne dans la liste.");
                return;
            }
            preparerEtape3();
            etapeActuelle = 3;
        }
        mettreAJourAffichageEtape();
    }

    private void reculerEtape() {
        if (etapeActuelle > 1) {
            etapeActuelle--;
            mettreAJourAffichageEtape();
        }
    }

    private void preparerEtape2() {
        boolean estPromoteur = "PROMOTEUR".equals(typeCompteSelectionne);

        sectionRecherche.setVisible(!estPromoteur);
        sectionRecherche.setManaged(!estPromoteur);

        sectionSaisieDirecte.setVisible(estPromoteur);
        sectionSaisieDirecte.setManaged(estPromoteur);

        if (!estPromoteur) {
            titreRechercheLabel.setText("Rechercher un " + typeCompteSelectionne.toLowerCase());
            tableResultats.getItems().clear();
            personneSelectionnee = null;
            if (infoSelectionLabel != null) infoSelectionLabel.setVisible(false);
            effectuerRechercheAsync();
        }
    }

    private void preparerEtape3() {
        String baseUser = "";
        if ("PROMOTEUR".equals(typeCompteSelectionne)) {
            recapNomLabel.setText(nomDirectField.getText().trim() + " " + prenomDirectField.getText().trim());
            recapTypeLabel.setText("Promoteur / Admin (" + roleSystemeCombo.getValue() + ")");
            recapIdMetierLabel.setText("Nouveau profil");

            baseUser = (prenomDirectField.getText().trim().substring(0, 1) + nomDirectField.getText().trim()).toLowerCase().replaceAll("\\s+", "");
        } else if (personneSelectionnee != null) {
            recapNomLabel.setText(personneSelectionnee.getNom() + " " + personneSelectionnee.getPrenom());
            recapTypeLabel.setText(typeCompteSelectionne);
            recapIdMetierLabel.setText(personneSelectionnee.getMatricule());

            baseUser = (personneSelectionnee.getPrenom().substring(0, 1) + personneSelectionnee.getNom()).toLowerCase().replaceAll("\\s+", "");
        }

        usernameField.setText(baseUser);

        // Mise à jour du mot de passe par défaut si la case est cochée
        if (useDefaultPasswordCheckBox != null && useDefaultPasswordCheckBox.isSelected()) {
            appliquerMotDePasseParDefaut();
        }
    }

    private void mettreAJourAffichageEtape() {
        scrollEtape1.setVisible(etapeActuelle == 1);
        scrollEtape1.setManaged(etapeActuelle == 1);

        scrollEtape2.setVisible(etapeActuelle == 2);
        scrollEtape2.setManaged(etapeActuelle == 2);

        scrollEtape3.setVisible(etapeActuelle == 3);
        scrollEtape3.setManaged(etapeActuelle == 3);

        btnPrecedent.setVisible(etapeActuelle > 1);
        btnPrecedent.setManaged(etapeActuelle > 1);

        btnSuivant.setVisible(etapeActuelle < 3);
        btnSuivant.setManaged(etapeActuelle < 3);

        btnCreer.setVisible(etapeActuelle == 3);
        btnCreer.setManaged(etapeActuelle == 3);

        mettreAJourStepper();
    }

    private void mettreAJourStepper() {
        appliquerEtatEtape(circleEtape1, numEtape1, labelEtape1, etapeActuelle >= 1, etapeActuelle > 1);
        appliquerEtatEtape(circleEtape2, numEtape2, labelEtape2, etapeActuelle >= 2, etapeActuelle > 2);
        appliquerEtatEtape(circleEtape3, numEtape3, labelEtape3, etapeActuelle >= 3, false);
    }

    private void appliquerEtatEtape(Circle cercle, Label numero, Label libelle, boolean atteinte, boolean completee) {
        cercle.setFill(atteinte ? COULEUR_ACTIVE : COULEUR_INACTIVE);

        numero.getStyleClass().removeAll("active", "completed");
        libelle.getStyleClass().removeAll("active", "completed");
        if (completee) {
            numero.getStyleClass().add("completed");
            libelle.getStyleClass().add("completed");
        } else if (atteinte) {
            numero.getStyleClass().add("active");
            libelle.getStyleClass().add("active");
        }
    }

    // -------------------------------------------------------------
    // 4. MOT DE PASSE & VALIDEURS
    // -------------------------------------------------------------
    private void configurerMotDePasse() {
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> calculerForceMotDePasse(newVal));

        // Mettre à jour le mot de passe si le nom d'utilisateur change et que la case est cochée
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (useDefaultPasswordCheckBox != null && useDefaultPasswordCheckBox.isSelected()) {
                appliquerMotDePasseParDefaut();
            }
        });

        // Écouteur pour la case à cocher "Mot de passe par défaut"
        if (useDefaultPasswordCheckBox != null) {
            useDefaultPasswordCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    appliquerMotDePasseParDefaut();
                } else {
                    passwordField.setDisable(false);
                    confirmPasswordField.setDisable(false);
                    passwordField.clear();
                    confirmPasswordField.clear();
                }
            });
        }

        if (btnTogglePassword != null) {
            btnTogglePassword.setOnAction(e -> {
                mdpVisible = !mdpVisible;
                if (mdpVisible) {
                    passwordTextField.setText(passwordField.getText());
                    passwordTextField.setVisible(true); passwordTextField.setManaged(true);
                    passwordField.setVisible(false); passwordField.setManaged(false);
                } else {
                    passwordField.setText(passwordTextField.getText());
                    passwordField.setVisible(true); passwordField.setManaged(true);
                    passwordTextField.setVisible(false); passwordTextField.setManaged(false);
                }
            });
        }
    }

    /**
     * Calcule et applique le mot de passe par défaut : MATRICULE + USERNAME
     */
    private void appliquerMotDePasseParDefaut() {
        String matriculeOuNom = "";
        if ("PROMOTEUR".equals(typeCompteSelectionne)) {
            matriculeOuNom = nomDirectField.getText().trim();
        } else if (personneSelectionnee != null) {
            matriculeOuNom = personneSelectionnee.getMatricule();
        }

        String username = usernameField.getText().trim();
        String mdpParDefaut = (matriculeOuNom + username).toLowerCase().replaceAll("\\s+", "");

        passwordField.setText(mdpParDefaut);
        confirmPasswordField.setText(mdpParDefaut);

        passwordField.setDisable(true);
        confirmPasswordField.setDisable(true);
    }

    private void calculerForceMotDePasse(String password) {
        if (password == null || password.isEmpty()) {
            forceMdpLabel.setText("—");
            if (forceMdpProgress != null) forceMdpProgress.setProgress(0);
            return;
        }

        double score = 0;
        if (password.length() >= 8) score += 0.3;
        if (password.matches(".*[A-Z].*")) score += 0.25;
        if (password.matches(".*[0-9].*")) score += 0.25;
        if (password.matches(".*[@#$%^&+=!_].*")) score += 0.2;

        if (forceMdpProgress != null) {
            forceMdpProgress.setVisible(true);
            forceMdpProgress.setProgress(score);
        }

        if (score < 0.4) {
            forceMdpLabel.setText("Faible");
            forceMdpLabel.setStyle("-fx-text-fill: #E5484D; -fx-font-weight: bold;");
        } else if (score < 0.75) {
            forceMdpLabel.setText("Moyen");
            forceMdpLabel.setStyle("-fx-text-fill: #F5A524; -fx-font-weight: bold;");
        } else {
            forceMdpLabel.setText("Fort");
            forceMdpLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
        }
    }

    // -------------------------------------------------------------
    // 5. ENREGISTREMENT ET SOUMISSION ASYNCHRONE
    // -------------------------------------------------------------
    private void enregistrerUtilisateurAsync() {
        String username = usernameField.getText().trim();

        // CORRECTION UNIQUE EMAIL : Si l'email est vide, envoyer null au lieu de ""
        String emailSaisie = emailField.getText().trim();
        String email = emailSaisie.isBlank() ? null : emailSaisie;

        String password = passwordField.getText();
        String confirmMdp = confirmPasswordField.getText();

        if (username.isBlank()) {
            masquerEtAfficherErreur(erreurUsername, "Username requis.");
            return;
        } else {
            masquerErreur(erreurUsername);
        }

        if (password.length() < 6) {
            ToastNotification.avertissement(getStage(), "Le mot de passe doit faire au moins 6 caractères.");
            return;
        }

        if (!password.equals(confirmMdp)) {
            masquerEtAfficherErreur(erreurConfirmMdp, "Les mots de passe ne correspondent pas !");
            return;
        } else {
            masquerErreur(erreurConfirmMdp);
        }

        btnCreer.setDisable(true);

        String idLiaison = ("PROMOTEUR".equals(typeCompteSelectionne)) ? null : personneSelectionnee.getMatricule();
        String nom = ("PROMOTEUR".equals(typeCompteSelectionne)) ? nomDirectField.getText().trim() : personneSelectionnee.getNom();
        String prenom = ("PROMOTEUR".equals(typeCompteSelectionne)) ? prenomDirectField.getText().trim() : personneSelectionnee.getPrenom();

        utilisateurService.creerUtilisateurAsync(
                idEcoleCourante,
                username,
                password,
                nom,
                prenom,
                email, // Transmet null si vide
                typeCompteSelectionne,
                () -> Platform.runLater(() -> {
                    auditService.tracerActionAsync("HABILITATIONS", "CREATION_UTILISATEUR", "Compte créé pour : " + username + " (" + typeCompteSelectionne + ")");
                    ToastNotification.succes(getStage(), "Compte utilisateur créé avec succès !");
                    if (callbackSuccess != null) callbackSuccess.run();
                    fermerFenetre();
                }),
                erreur -> Platform.runLater(() -> {
                    btnCreer.setDisable(false);
                    ToastNotification.erreur(getStage(), "Erreur : " + erreur.getMessage());
                })
        );
    }

    private void masquerEtAfficherErreur(Label lbl, String msg) {
        if (lbl != null) {
            lbl.setText(msg);
            lbl.setVisible(true);
            lbl.setManaged(true);
        }
    }

    private void masquerErreur(Label lbl) {
        if (lbl != null) {
            lbl.setVisible(false);
            lbl.setManaged(false);
        }
    }

    private void fermerFenetre() {
        Stage stage = getStage();
        if (stage != null) stage.close();
    }

    private Stage getStage() {
        return (Stage) btnAnnuler.getScene().getWindow();
    }
}