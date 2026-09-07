package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.erpklassup.erpklassup.BoutonChargement;
import org.erpklassup.erpklassup.HelloApplication;
import org.erpklassup.erpklassup.WindowsTitleBar;
import org.erpklassup.erpklassup.dao.UtilisateurDAO;
import org.erpklassup.erpklassup.dao.UtilisateurRoleDAO;
import org.erpklassup.erpklassup.models.Role;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.service.AppExecutor;
import org.erpklassup.erpklassup.service.AuthService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.util.StageHelper;
import org.erpklassup.erpklassup.util.ToastNotification;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class LoginController {

    @FXML private Button btnConnecter;
    @FXML private HBox loginAppBar;

    @FXML private TextField champEmail;
    @FXML private PasswordField champMotDePasse;
    @FXML private TextField champMotDePasseVisible;
    @FXML private SVGPath iconeOeil;
    @FXML private Label labelErreur;
    @FXML private Label nbEleves;
    @FXML private Label nbEnseignants;
    @FXML private Label nbClasses;

    private static final String APP_BAR_COLOR = "#16213A";

    private boolean motDePasseVisible = false;

    private static final String OEIL_OUVERT =
            "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z";
    private static final String OEIL_FERME =
            "M17.94 17.94A10.94 10.94 0 0 1 12 20c-7 0-11-8-11-8a18.5 18.5 0 0 1 5.06-5.94 " +
                    "M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19 " +
                    "M1 1l22 22";

    @FXML
    public void initialize() {
        chargerStatistiquesGlobales();
    }

    @FXML
    public void handleClose() {
        Stage stage = (Stage) loginAppBar.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void toggleMotDePasse() {
        motDePasseVisible = !motDePasseVisible;

        if (motDePasseVisible) {
            champMotDePasseVisible.setText(champMotDePasse.getText());
            champMotDePasse.setVisible(false);
            champMotDePasse.setManaged(false);
            champMotDePasseVisible.setVisible(true);
            champMotDePasseVisible.setManaged(true);
            iconeOeil.setContent(OEIL_FERME);
        } else {
            champMotDePasse.setText(champMotDePasseVisible.getText());
            champMotDePasseVisible.setVisible(false);
            champMotDePasseVisible.setManaged(false);
            champMotDePasse.setVisible(true);
            champMotDePasse.setManaged(true);
            iconeOeil.setContent(OEIL_OUVERT);
        }
    }

    @FXML
//    public void handleLogin() {
//        String login = champEmail.getText().trim();
//        String motDePasse = motDePasseVisible ? champMotDePasseVisible.getText() : champMotDePasse.getText();
//        if (login.isEmpty() || motDePasse.isEmpty()) {
//            afficherErreur("Veuillez remplir tous les champs.");
//            return;
//        }
//        String ecole = "ECO_001";
//        BoutonChargement.demarrer(btnConnecter, "Connexion en cours...");
//        Task<AuthService.ResultatConnexion> tacheConnexion = new Task<>() {
//            @Override
//            protected AuthService.ResultatConnexion call() {
//                return new AuthService().seConnecter(login, motDePasse, ecole);
//            }
//        };
//        tacheConnexion.setOnSucceeded(e -> {
//            AuthService.ResultatConnexion resultat = tacheConnexion.getValue();
//            if (resultat.isSucces()) {
//                try {
//                    Stage stage = (Stage) btnConnecter.getScene().getWindow();
//                    Parent nouvelleRacine = chargerVue("/org/erpklassup/erpklassup/hello-view.fxml").load();
//                    stage.setResizable(true);
//                    stage.setWidth(1180);
//                    stage.centerOnScreen();
//                    stage.setTitle("KlassUp");
//                    stage.getScene().setRoot(nouvelleRacine);
//                } catch (IOException ex) {
//                    BoutonChargement.arreter(btnConnecter);
//                    afficherErreur("Impossible de charger le tableau de bord.");
//                }
//            } else {
//                BoutonChargement.arreter(btnConnecter);
//                afficherErreur(resultat.getMessage());
//            }
//        });
//        tacheConnexion.setOnFailed(e -> {
//            BoutonChargement.arreter(btnConnecter);
//            tacheConnexion.getException().printStackTrace();
//            afficherErreur("Erreur technique lors de la connexion.");
//        });
//        AppExecutor.get().submit(tacheConnexion);
//    }

    public void handleLogin() {
        String login = champEmail.getText().trim();
        String motDePasse = motDePasseVisible ? champMotDePasseVisible.getText() : champMotDePasse.getText();

        if (login.isEmpty() || motDePasse.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs.");
            return;
        }

        String ecole = "ECO_001";
        BoutonChargement.demarrer(btnConnecter, "Connexion en cours...");

        Task<AuthService.ResultatConnexion> tacheConnexion = new Task<>() {
            @Override
            protected AuthService.ResultatConnexion call() {
                return new AuthService().seConnecter(login, motDePasse, ecole);
            }
        };

        tacheConnexion.setOnSucceeded(event -> {
            AuthService.ResultatConnexion resultat = tacheConnexion.getValue();

            if (resultat.isSucces()) {
                SessionManager sessionManager = SessionManager.getInstance();

                if (sessionManager.estConnecte()) {
                    Utilisateur user = sessionManager.getUtilisateurCourant();

                    // ✅ LOG SANS getIdRole
                    System.out.println("✅ Connecté en tant que : " + user.getNomComplet());
                    System.out.println("   Username : " + user.getUsername());

                    // ✅ Récupérer les rôles via UtilisateurRoleDAO
                    List<Role> roles = new UtilisateurRoleDAO()
                            .findRolesByUtilisateur(user.getIdUtilisateur());
                    if (!roles.isEmpty()) {
                        String nomsRoles = roles.stream()
                                .map(Role::getNomRole)
                                .reduce((a, b) -> a + ", " + b)
                                .orElse("Aucun");
                        System.out.println("   Rôles : " + nomsRoles);
                    }

                    // ✅ Toast de bienvenue
                    ToastNotification.succes(
                            StageHelper.getStage(btnConnecter),
                            "Bienvenue " + user.getNomComplet() + " !"
                    );

                    naviguerVersDashboard();

                } else {
                    BoutonChargement.arreter(btnConnecter);
                    afficherErreur("Erreur : session non démarrée");
                }

            } else {
                BoutonChargement.arreter(btnConnecter);
                afficherErreur(resultat.getMessage());
            }
        });

        tacheConnexion.setOnFailed(event -> {
            BoutonChargement.arreter(btnConnecter);
            System.err.println("Erreur de connexion : " + tacheConnexion.getException().getMessage());
            afficherErreur("Erreur technique lors de la connexion.");
        });

        AppExecutor.get().submit(tacheConnexion);
    }

    private void naviguerVersDashboard() {
        try {
            Stage stage = (Stage) btnConnecter.getScene().getWindow();
            Parent nouvelleRacine = chargerVue("/org/erpklassup/erpklassup/hello-view.fxml").load();

            stage.setResizable(true);
            stage.centerOnScreen();
            stage.setTitle("KlassUp");
            stage.getScene().setRoot(nouvelleRacine);

        } catch (IOException ex) {
            BoutonChargement.arreter(btnConnecter);
            afficherErreur("Impossible de charger le tableau de bord.");
            System.err.println("Erreur navigation : " + ex.getMessage());
        }
    }

    private void chargerStatistiquesGlobales() {
        try (Connection conn = Database.getConnexion()) {

            // 1. Compter les élèves
            String sqlEleves = "SELECT COUNT(*) FROM Eleve";
            try (PreparedStatement stmt = conn.prepareStatement(sqlEleves);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    nbEleves.setText(String.format("%,d", rs.getInt(1)));
                }
            }

            // 2. Compter les enseignants
            String sqlEnseignants = "SELECT COUNT(*) FROM Enseignant";
            try (PreparedStatement stmt = conn.prepareStatement(sqlEnseignants);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    nbEnseignants.setText(String.valueOf(rs.getInt(1)));
                }
            }

            // 3. Compter les classes
            String sqlClasses = "SELECT COUNT(*) FROM Classe";
            try (PreparedStatement stmt = conn.prepareStatement(sqlClasses);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    nbClasses.setText(String.valueOf(rs.getInt(1)));
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des compteurs du Dashboard : " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void afficherErreur(String message) {
        labelErreur.setText(message);
        labelErreur.setVisible(true);
        labelErreur.setManaged(true);
    }

    public static FXMLLoader chargerVue(String cheminAbsolu) {
        var url = LoginController.class.getResource(cheminAbsolu);
        if (url == null) {
            throw new IllegalStateException("FXML introuvable : " + cheminAbsolu
                    + " — vérifie qu'il est bien dans src/main/resources et dans le classpath.");
        }
        return new FXMLLoader(url);
    }
}