package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.dao.*;
import org.erpklassup.erpklassup.models.*;
import org.erpklassup.erpklassup.service.PasswordService;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserFormController implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
//
//    // ========== COMPOSANTS FXML ==========
//    @FXML private Label titreLabel;
//    @FXML private Label sousTitreLabel;
//    @FXML private Label badgeEcoleLabel;
//    @FXML private TextField nomField;
//    @FXML private TextField prenomField;
//    @FXML private TextField usernameField;
//    @FXML private TextField emailField;
//    @FXML private TextField telephoneField;
//    @FXML private ComboBox<String> typeUserCombo;
//    @FXML private VBox ecoleBox;
//    @FXML private ComboBox<Ecole> ecoleCombo;
//    @FXML private PasswordField passwordField;
//    @FXML private PasswordField confirmPasswordField;
//    @FXML private Button togglePasswordBtn;
//    @FXML private Label forceMdpLabel;
//    @FXML private ProgressBar forceMdpProgress;
//    @FXML private CheckBox changerMdpCheckBox;
//    @FXML private CheckBox activer2FACheckBox;
//    @FXML private TextField searchRoleField;
//    @FXML private FlowPane rolesFlowPane;
//    @FXML private Label roleCountLabel;
//    @FXML private ComboBox<Role> rolePrincipalCombo;
//    @FXML private Label permCountLabel;
//    @FXML private CheckBox showExceptionsCheckBox;
//    @FXML private VBox permissionsContainer;
//    @FXML private Label errorLabel;
//    @FXML private Button btnAnnuler;
//    @FXML private Button btnEnregistrer;
//    @FXML private Button btnRetour;
//
//    // ========== DAOs ==========
//    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
//    private final RoleDAO roleDAO = new RoleDAO();
//    private final PermissionDAO permissionDAO = new PermissionDAO();
//    private final EcoleDAO ecoleDAO = new EcoleDAO();
//
//    // ========== DONNÉES ==========
//    private final Map<CheckBox, Role> roleCheckBoxes = new HashMap<>();
//    private final Map<CheckBox, Permission> permissionCheckBoxes = new HashMap<>();
//    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
//    private String idEcoleCourante = "ECO_001";
//    private boolean modeModification = false;
//    private Utilisateur utilisateurAModifier;
//
//    @Override
//    public void initialize(URL location, ResourceBundle resources) {
//        configurerTypes();
//        chargerEcolesAsynchrone();
//        chargerRolesAsynchrone();
//        chargerPermissionsAsynchrone();
//        configurerEvenements();
//    }
//
//    // ========== CONFIGURATION ==========
//    private void configurerTypes() {
//        typeUserCombo.getItems().addAll("ADMIN", "ENSEIGNANT", "SECRETAIRE", "COMPTABLE", "PARENT", "ELEVE");
//        typeUserCombo.setValue("ENSEIGNANT");
//    }
//
//    private void chargerEcolesAsynchrone() {
//        Task<List<Ecole>> task = new Task<>() {
//            @Override
//            protected List<Ecole> call() {
//                return ecoleDAO.findAll();
//            }
//        };
//        task.setOnSucceeded(e -> {
//            ecoleCombo.getItems().addAll(task.getValue());
//            if (!task.getValue().isEmpty()) {
//                ecoleCombo.setValue(task.getValue().get(0));
//                badgeEcoleLabel.setText(task.getValue().get(0).getNomEcole());
//            }
//        });
//        executorService.submit(task);
//    }
//
//    private void chargerRolesAsynchrone() {
//        Task<List<Role>> task = new Task<>() {
//            @Override
//            protected List<Role> call() {
//                return roleDAO.findByEcole(idEcoleCourante);
//            }
//        };
//        task.setOnSucceeded(e -> {
//            List<Role> roles = task.getValue();
//            rolesFlowPane.getChildren().clear();
//            roleCheckBoxes.clear();
//
//            for (Role role : roles) {
//                CheckBox cb = new CheckBox(role.getNomRole());
//                cb.setStyle("-fx-text-fill: #334155;");
//                roleCheckBoxes.put(cb, role);
//                rolesFlowPane.getChildren().add(cb);
//            }
//
//            rolePrincipalCombo.getItems().addAll(roles);
//            if (!roles.isEmpty()) {
//                rolePrincipalCombo.setValue(roles.get(0));
//            }
//        });
//        executorService.submit(task);
//    }
//
//    private void chargerPermissionsAsynchrone() {
//        Task<List<Permission>> task = new Task<>() {
//            @Override
//            protected List<Permission> call() {
//                return permissionDAO.findAll();
//            }
//        };
//        task.setOnSucceeded(e -> {
//            List<Permission> permissions = task.getValue();
//            permissionsContainer.getChildren().clear();
//            permissionCheckBoxes.clear();
//
//            for (Permission permission : permissions) {
//                CheckBox cb = new CheckBox(permission.getLibelle());
//                cb.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");
//                permissionCheckBoxes.put(cb, permission);
//                permissionsContainer.getChildren().add(cb);
//            }
//        });
//        executorService.submit(task);
//    }
//
//    private void configurerEvenements() {
//        // Afficher/masquer le mot de passe
//        togglePasswordBtn.setOnAction(e -> {
//            if (passwordField.getVisible()) {
//                passwordField.setVisible(false);
//                passwordField.setManaged(false);
//            } else {
//                passwordField.setVisible(true);
//                passwordField.setManaged(true);
//            }
//        });
//
//        // Indicateur de force du mot de passe
//        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
//            mettreAJourForceMdp(newVal);
//        });
//
//        // Afficher/masquer les permissions d'exception
//        showExceptionsCheckBox.setOnAction(e -> {
//            boolean visible = showExceptionsCheckBox.isSelected();
//            permissionsContainer.setVisible(visible);
//            permissionsContainer.setManaged(visible);
//        });
//
//        // Mise à jour du compteur de rôles
//        for (CheckBox cb : roleCheckBoxes.keySet()) {
//            cb.setOnAction(e -> mettreAJourCompteurRoles());
//        }
//
//        // Mise à jour du compteur de permissions
//        for (CheckBox cb : permissionCheckBoxes.keySet()) {
//            cb.setOnAction(e -> mettreAJourCompteurPermissions());
//        }
//
//        // Recherche de rôle
//        searchRoleField.textProperty().addListener((obs, oldVal, newVal) -> {
//            filtrerRoles(newVal);
//        });
//
//        // Boutons
//        btnAnnuler.setOnAction(e -> fermerFenetre());
//        btnRetour.setOnAction(e -> fermerFenetre());
//        btnEnregistrer.setOnAction(e -> enregistrerUtilisateur());
//    }
//
//    // ========== MÉTHODES DE VALIDATION ==========
//    private boolean validerFormulaire() {
//        errorLabel.setVisible(false);
//
//        if (nomField.getText() == null || nomField.getText().trim().isEmpty()) {
//            afficherErreur("Le nom est obligatoire");
//            return false;
//        }
//
//        if (usernameField.getText() == null || usernameField.getText().trim().isEmpty()) {
//            afficherErreur("Le username est obligatoire");
//            return false;
//        }
//
//        if (emailField.getText() == null || !emailField.getText().contains("@")) {
//            afficherErreur("L'email est invalide");
//            return false;
//        }
//
//        if (passwordField.getText() == null || passwordField.getText().length() < 8) {
//            afficherErreur("Le mot de passe doit contenir au moins 8 caractères");
//            return false;
//        }
//
//        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
//            afficherErreur("Les mots de passe ne correspondent pas");
//            return false;
//        }
//
//        if (roleCheckBoxes.keySet().stream().noneMatch(CheckBox::isSelected)) {
//            afficherErreur("Veuillez sélectionner au moins un rôle");
//            return false;
//        }
//
//        return true;
//    }
//
//    // ========== ENREGISTREMENT ==========
//    private void enregistrerUtilisateur() {
//        if (!validerFormulaire()) return;
//
//        btnEnregistrer.setDisable(true);
//
//        // Créer l'utilisateur
//        Utilisateur user = new Utilisateur();
//        user.setNom(nomField.getText().trim());
//        user.setPrenom(prenomField.getText().trim());
//        user.setUsername(usernameField.getText().trim());
//        user.setEmail(emailField.getText().trim());
//        user.setTelephone(telephoneField.getText().trim());
//        user.setIdEcole(idEcoleCourante);
//        user.setTypeUtilisateur(typeUserCombo.getValue());
//        user.setPasswordHash(PasswordService.hacher(passwordField.getText()));
//        user.setDoitChangerMotDePasse(changerMdpCheckBox.isSelected());
//        user.setDeuxFacteursActive(activer2FACheckBox.isSelected());
//        user.setEstActif(true);
//
//        // Récupérer les rôles sélectionnés
//        List<String> rolesSelectionnes = new ArrayList<>();
//        for (Map.Entry<CheckBox, Role> entry : roleCheckBoxes.entrySet()) {
//            if (entry.getKey().isSelected()) {
//                rolesSelectionnes.add(entry.getValue().getIdRole());
//            }
//        }
//
//        // Récupérer les permissions d'exception
//        List<String> permissionsException = new ArrayList<>();
//        for (Map.Entry<CheckBox, Permission> entry : permissionCheckBoxes.entrySet()) {
//            if (entry.getKey().isSelected()) {
//                permissionsException.add(entry.getValue().getIdPermission());
//            }
//        }
//
//        Task<Boolean> task = new Task<>() {
//            @Override
//            protected Boolean call() {
//                // 1. Insérer l'utilisateur
//                boolean userCree = utilisateurDAO.create(user);
//                if (!userCree) return false;
//
//                // 2. Assigner les rôles
//                for (String idRole : rolesSelectionnes) {
//                    UtilisateurRole ur = new UtilisateurRole(user.getIdUtilisateur(), idRole);
//                    new UtilisateurRoleDAO().ajouterRole(ur);
//                }
//
//                // 3. Ajouter les permissions d'exception
//                for (String idPermission : permissionsException) {
//                    UtilisateurPermission up = new UtilisateurPermission(user.getIdUtilisateur(), idPermission);
//                    new UtilisateurPermissionDAO().ajouter(up);
//                }
//
//                return true;
//            }
//        };
//
//        task.setOnSucceeded(e -> {
//            Platform.runLater(() -> {
//                if (task.getValue()) {
//                    afficherSucces("Utilisateur créé avec succès !");
//                    fermerFenetre();
//                } else {
//                    afficherErreur("Erreur lors de la création");
//                    btnEnregistrer.setDisable(false);
//                }
//            });
//        });
//
//        task.setOnFailed(e -> {
//            Platform.runLater(() -> {
//                afficherErreur("Erreur : " + task.getException().getMessage());
//                btnEnregistrer.setDisable(false);
//            });
//        });
//
//        executorService.submit(task);
//    }
//
//    // ========== HELPERS ==========
//    private void mettreAJourForceMdp(String mdp) {
//        if (mdp == null || mdp.isEmpty()) {
//            forceMdpLabel.setText("—");
//            forceMdpProgress.setVisible(false);
//            return;
//        }
//
//        int score = 0;
//        if (mdp.length() >= 8) score++;
//        if (mdp.matches(".*[A-Z].*")) score++;
//        if (mdp.matches(".*[a-z].*")) score++;
//        if (mdp.matches(".*[0-9].*")) score++;
//        if (mdp.matches(".*[!@#$%].*")) score++;
//
//        forceMdpProgress.setVisible(true);
//        forceMdpProgress.setProgress(score / 5.0);
//
//        switch (score) {
//            case 0:
//            case 1:
//                forceMdpLabel.setText("Faible");
//                forceMdpLabel.setStyle("-fx-text-fill: #E5484D;");
//                break;
//            case 2:
//            case 3:
//                forceMdpLabel.setText("Moyen");
//                forceMdpLabel.setStyle("-fx-text-fill: #F5A524;");
//                break;
//            case 4:
//            case 5:
//                forceMdpLabel.setText("Fort");
//                forceMdpLabel.setStyle("-fx-text-fill: #16A34A;");
//                break;
//        }
//    }
//
//    private void mettreAJourCompteurRoles() {
//        long count = roleCheckBoxes.keySet().stream().filter(CheckBox::isSelected).count();
//        roleCountLabel.setText(count + " sélectionné(s)");
//    }
//
//    private void mettreAJourCompteurPermissions() {
//        long count = permissionCheckBoxes.keySet().stream().filter(CheckBox::isSelected).count();
//        permCountLabel.setText(count + " sélectionnée(s)");
//    }
//
//    private void filtrerRoles(String recherche) {
//        if (recherche == null || recherche.isEmpty()) {
//            for (CheckBox cb : roleCheckBoxes.keySet()) {
//                cb.setVisible(true);
//                cb.setManaged(true);
//            }
//        } else {
//            String lower = recherche.toLowerCase();
//            for (Map.Entry<CheckBox, Role> entry : roleCheckBoxes.entrySet()) {
//                boolean correspond = entry.getValue().getNomRole().toLowerCase().contains(lower);
//                entry.getKey().setVisible(correspond);
//                entry.getKey().setManaged(correspond);
//            }
//        }
//    }
//
//    private void afficherErreur(String message) {
//        errorLabel.setText(message);
//        errorLabel.setVisible(true);
//        errorLabel.setManaged(true);
//    }
//
//    private void afficherSucces(String message) {
//        Alert alert = new Alert(Alert.AlertType.INFORMATION);
//        alert.setTitle("Succès");
//        alert.setHeaderText(null);
//        alert.setContentText(message);
//        alert.showAndWait();
//    }
//
//    private void fermerFenetre() {
//        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
//        stage.close();
//    }
//
//    public void shutdown() {
//        executorService.shutdown();
//    }
}