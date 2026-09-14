package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.BoutonChargement;
import org.erpklassup.erpklassup.Database;
import org.erpklassup.erpklassup.dao.EcoleDAO;
import org.erpklassup.erpklassup.dao.UtilisateurRoleDAO;
import org.erpklassup.erpklassup.models.Ecole;
import org.erpklassup.erpklassup.models.Role;
import org.erpklassup.erpklassup.models.Utilisateur;
import org.erpklassup.erpklassup.service.AppExecutor;
import org.erpklassup.erpklassup.service.AuthService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.I18nManager;
import org.erpklassup.erpklassup.util.ModalUtil;
import org.erpklassup.erpklassup.util.StageHelper;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Enumeration;
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
    @FXML private VBox container2FA;
    @FXML private TextField champCode2FA;

    private boolean motDePasseVisible = false;

    private static final String OEIL_OUVERT =
            "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z";
    private static final String OEIL_FERME =
            "M17.94 17.94A10.94 10.94 0 0 1 12 20c-7 0-11-8-11-8a18.5 18.5 0 0 1 5.06-5.94 " +
                    "M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19 " +
                    "M1 1l22 22";

    private static final String CHEMIN_SETUP_2FA = "/org/erpklassup/erpklassup/view/setup-2fa.fxml";

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
    public void handleLogin() {
        String login = champEmail.getText().trim();
        String motDePasse = motDePasseVisible ? champMotDePasseVisible.getText() : champMotDePasse.getText();
        String code2FA = (champCode2FA != null) ? champCode2FA.getText().trim() : "";

        // 1. Validation des champs de base
        if (login.isEmpty() || motDePasse.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs.");
            return;
        }

        // 2. Validation si le champ 2FA est affiché à l'écran
        if (container2FA != null && container2FA.isVisible() && code2FA.isEmpty()) {
            afficherErreur("Veuillez saisir votre code de vérification à 6 chiffres.");
            return;
        }

        Stage stageCourant = StageHelper.getStage(btnConnecter);
        BoutonChargement.demarrer(btnConnecter, "Connexion en cours...");

        Task<AuthService.ResultatConnexion> tacheConnexion = new Task<>() {
            @Override
            protected AuthService.ResultatConnexion call() {
                EcoleDAO ecoleDAO = new EcoleDAO();
                var ecoleOpt = ecoleDAO.findPremiereEcoleActive();

                if (ecoleOpt.isEmpty()) {
                    return new AuthService.ResultatConnexion(false, "Aucune école active trouvée dans la base de données");
                }

                Ecole ecole = ecoleOpt.get();
                System.out.println("🏫 École chargée : " + ecole.getNomEcole() + " (ID: " + ecole.getIdEcole() + ")");

                SessionManager.getInstance().setEcoleCourante(ecole);

                String ipAdresse = obtenirAdresseIpLocale();
                String nomApp = obtenirInfoAppareil();
                String userAgentCustom = "KlassUp-DesktopClient/1.0 (" + System.getProperty("os.name") + ")";

                AuthService authService = new AuthService();
                return authService.seConnecter(stageCourant, login, motDePasse, code2FA,
                        ecole.getIdEcole(), ipAdresse, userAgentCustom, nomApp);
            }
        };

        tacheConnexion.setOnSucceeded(event -> {
            AuthService.ResultatConnexion resultat = tacheConnexion.getValue();

            // ==========================================
            // CAS 1 : Connexion réussie
            // ==========================================
            if (resultat != null && resultat.isSucces()) {
                SessionManager sessionManager = SessionManager.getInstance();

                if (sessionManager.estConnecte()) {
                    Utilisateur user = sessionManager.getUtilisateurCourant();

                    System.out.println("✅ Connecté : " + user.getNomComplet());
                    System.out.println("   Username : " + user.getUsername());
                    System.out.println("   École : " + sessionManager.getEcoleCourante().getNomEcole());

                    List<Role> roles = new UtilisateurRoleDAO().findRolesByUtilisateur(user.getIdUtilisateur());
                    if (!roles.isEmpty()) {
                        String nomsRoles = roles.stream()
                                .map(Role::getNomRole)
                                .reduce((a, b) -> a + ", " + b)
                                .orElse("Aucun");
                        System.out.println("   Rôles : " + nomsRoles);
                    }

                    naviguerVersDashboard();
                } else {
                    BoutonChargement.arreter(btnConnecter);
                    afficherErreur("Erreur : session non démarrée");
                }
                return;
            }

            // ==========================================
            // CAS 2 : L'utilisateur doit CONFIGURER sa 2FA
            // ==========================================
            if (resultat != null && resultat.isDoitConfigurer2FA()) {
                BoutonChargement.arreter(btnConnecter);
                ouvrirEcranSetup2FA(resultat.getUtilisateur(), stageCourant);
                return;
            }

            // ==========================================
            // CAS 3 : L'utilisateur doit SAISIR son code 2FA
            // ==========================================
            if (resultat != null && resultat.isRequiert2FA()) {
                BoutonChargement.arreter(btnConnecter);

                if (container2FA != null) {
                    container2FA.setVisible(true);
                    container2FA.setManaged(true);
                    if (champCode2FA != null) {
                        champCode2FA.requestFocus();
                    }
                }

                afficherErreur("Authentification à deux facteurs requise. Veuillez entrer le code TOTP.");
                return;
            }

            // ==========================================
            // CAS 4 : Échec
            // ==========================================
            BoutonChargement.arreter(btnConnecter);
            String msg = (resultat != null) ? resultat.getMessage() : "Échec de connexion inconnu";
            afficherErreur(msg);
        });

        tacheConnexion.setOnFailed(event -> {
            BoutonChargement.arreter(btnConnecter);
            Throwable exception = tacheConnexion.getException();
            if (exception != null) {
                exception.printStackTrace();
            }
            afficherErreur("Erreur technique lors de la connexion.");
        });

        AppExecutor.get().submit(tacheConnexion);
    }

    /**
     * ✅ Ouvre la fenêtre modale de configuration 2FA via ModalUtil.
     * Après configuration réussie, navigue automatiquement vers le dashboard.
     */
    private void ouvrirEcranSetup2FA(Utilisateur user, Stage stageParent) {
        if (user == null) {
            afficherErreur("Erreur : utilisateur introuvable pour la configuration 2FA.");
            return;
        }

        String idEcole = SessionManager.getInstance().getIdEcoleCourante();
        String ipAdresse = obtenirAdresseIpLocale();
        String nomApp = obtenirInfoAppareil();
        String userAgentCustom = "KlassUp-DesktopClient/1.0 (" + System.getProperty("os.name") + ")";

        // Ouverture via ModalUtil
        Setup2FAController ctrl = ModalUtil.ouvrirModal(
                getClass(),
                CHEMIN_SETUP_2FA,
                "Configuration 2FA obligatoire",
                stageParent,
                480,
                620
        );

        if (ctrl == null) {
            afficherErreur("Impossible d'ouvrir la configuration 2FA.");
            return;
        }

        // Initialisation des données + callback
        ctrl.initData(
                user.getIdUtilisateur(),
                user.getUsername(),
                user.getEmail(),
                "KlassUp",
                idEcole,
                ipAdresse,
                userAgentCustom,
                nomApp,
                resultatFinal -> {
                    // Callback : après configuration réussie, on navigue vers le dashboard
                    if (resultatFinal != null && resultatFinal.isSucces()) {
                        naviguerVersDashboard();
                    }
                }
        );
    }

//    private void naviguerVersDashboard() {
//        try {
//            Stage stage = (Stage) btnConnecter.getScene().getWindow();
//            Parent nouvelleRacine = chargerVue("/org/erpklassup/erpklassup/hello-view.fxml").load();
//
//            stage.setResizable(true);
//            stage.centerOnScreen();
//            stage.setTitle("KlassUp");
//            stage.getScene().setRoot(nouvelleRacine);
//
//        } catch (IOException ex) {
//            BoutonChargement.arreter(btnConnecter);
//            afficherErreur("Impossible de charger le tableau de bord.");
//            System.err.println("Erreur navigation : " + ex.getMessage());
//        }
//    }
private void naviguerVersDashboard() {
    try {
        Stage stage = (Stage) btnConnecter.getScene().getWindow();

        FXMLLoader loader = I18nManager.getInstance().creerLoader(
                getClass(),
                "/org/erpklassup/erpklassup/hello-view.fxml"
        );
        Parent nouvelleRacine = loader.load();

        stage.setResizable(true);
        stage.centerOnScreen();
        stage.setTitle("KlassUp");
        stage.getScene().setRoot(nouvelleRacine);

    } catch (IOException ex) {
        BoutonChargement.arreter(btnConnecter);
        afficherErreur("Impossible de charger le tableau de bord.");
        System.err.println("Erreur navigation : " + ex.getMessage());
        ex.printStackTrace();
    }
}

    private void chargerStatistiquesGlobales() {
        try (Connection conn = Database.getConnexion()) {

            String sqlEleves = "SELECT COUNT(*) FROM Eleve";
            try (PreparedStatement stmt = conn.prepareStatement(sqlEleves);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    nbEleves.setText(String.format("%,d", rs.getInt(1)));
                }
            }

            // ⚠️ Corrigé : Personnel au lieu de Enseignant (ta table s'appelle Personnel)
            String sqlEnseignants = "SELECT COUNT(*) FROM Personnel";
            try (PreparedStatement stmt = conn.prepareStatement(sqlEnseignants);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    nbEnseignants.setText(String.valueOf(rs.getInt(1)));
                }
            }

            String sqlClasses = "SELECT COUNT(*) FROM Classe";
            try (PreparedStatement stmt = conn.prepareStatement(sqlClasses);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    nbClasses.setText(String.valueOf(rs.getInt(1)));
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des compteurs du Dashboard : " + e.getMessage());
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

    public String obtenirAdresseIpLocale() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }

            InetAddress localHost = InetAddress.getLocalHost();
            if (!localHost.isLoopbackAddress()) {
                return localHost.getHostAddress();
            }
        } catch (Exception e) {
            System.err.println("Impossible de déterminer l'adresse IP locale : " + e.getMessage());
        }

        return "127.0.0.1";
    }

    public String obtenirInfoAppareil() {
        String hostName = "Machine";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {}

        String osName = System.getProperty("os.name", "Inconnu");
        String osArch = System.getProperty("os.arch", "");

        return String.format("%s (%s %s)", hostName, osName, osArch).trim();
    }
}
//package org.erpklassup.erpklassup.controllers.actionPage;
//
//import javafx.concurrent.Task;
//import javafx.fxml.FXML;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.control.Button;
//import javafx.scene.control.Label;
//import javafx.scene.control.PasswordField;
//import javafx.scene.control.TextField;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.VBox;
//import javafx.scene.shape.SVGPath;
//import javafx.stage.Stage;
//import org.erpklassup.erpklassup.BoutonChargement;
//import org.erpklassup.erpklassup.Database;
//import org.erpklassup.erpklassup.dao.EcoleDAO;
//import org.erpklassup.erpklassup.dao.UtilisateurRoleDAO;
//import org.erpklassup.erpklassup.models.Ecole;
//import org.erpklassup.erpklassup.models.Role;
//import org.erpklassup.erpklassup.models.Utilisateur;
//import org.erpklassup.erpklassup.service.AppExecutor;
//import org.erpklassup.erpklassup.service.AuthService;
//import org.erpklassup.erpklassup.service.SessionManager;
//import org.erpklassup.erpklassup.util.StageHelper;
//
//import java.io.IOException;
//import java.net.Inet4Address;
//import java.net.InetAddress;
//import java.net.NetworkInterface;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.util.Enumeration;
//import java.util.List;
//
//public class LoginController {
//
//    @FXML private Button btnConnecter;
//    @FXML private HBox loginAppBar;
//
//    @FXML private TextField champEmail;
//    @FXML private PasswordField champMotDePasse;
//    @FXML private TextField champMotDePasseVisible;
//    @FXML private SVGPath iconeOeil;
//    @FXML private Label labelErreur;
//    @FXML private Label nbEleves;
//    @FXML private Label nbEnseignants;
//    @FXML private Label nbClasses;
//    @FXML private VBox container2FA;
//    @FXML private TextField champCode2FA;
//
//    private boolean motDePasseVisible = false;
//
//    private static final String OEIL_OUVERT =
//            "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z";
//    private static final String OEIL_FERME =
//            "M17.94 17.94A10.94 10.94 0 0 1 12 20c-7 0-11-8-11-8a18.5 18.5 0 0 1 5.06-5.94 " +
//                    "M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19 " +
//                    "M1 1l22 22";
//
//    @FXML
//    public void initialize() {
//        chargerStatistiquesGlobales();
//    }
//
//    @FXML
//    public void handleClose() {
//        Stage stage = (Stage) loginAppBar.getScene().getWindow();
//        stage.close();
//    }
//
//    @FXML
//    public void toggleMotDePasse() {
//        motDePasseVisible = !motDePasseVisible;
//
//        if (motDePasseVisible) {
//            champMotDePasseVisible.setText(champMotDePasse.getText());
//            champMotDePasse.setVisible(false);
//            champMotDePasse.setManaged(false);
//            champMotDePasseVisible.setVisible(true);
//            champMotDePasseVisible.setManaged(true);
//            iconeOeil.setContent(OEIL_FERME);
//        } else {
//            champMotDePasse.setText(champMotDePasseVisible.getText());
//            champMotDePasseVisible.setVisible(false);
//            champMotDePasseVisible.setManaged(false);
//            champMotDePasse.setVisible(true);
//            champMotDePasse.setManaged(true);
//            iconeOeil.setContent(OEIL_OUVERT);
//        }
//    }
//
//
//    @FXML
//    public void handleLogin() {
//        String login = champEmail.getText().trim();
//        String motDePasse = motDePasseVisible ? champMotDePasseVisible.getText() : champMotDePasse.getText();
//        String code2FA = (champCode2FA != null) ? champCode2FA.getText().trim() : "";
//
//        // 1. Validation des champs de base
//        if (login.isEmpty() || motDePasse.isEmpty()) {
//            afficherErreur("Veuillez remplir tous les champs.");
//            return;
//        }
//
//        // 2. Validation si le champ 2FA est affiché à l'écran
//        if (container2FA != null && container2FA.isVisible() && code2FA.isEmpty()) {
//            afficherErreur("Veuillez saisir votre code de vérification à 6 chiffres.");
//            return;
//        }
//
//        Stage stageCourant = StageHelper.getStage(btnConnecter);
//        BoutonChargement.demarrer(btnConnecter, "Connexion en cours...");
//
//        Task<AuthService.ResultatConnexion> tacheConnexion = new Task<>() {
//            @Override
//            protected AuthService.ResultatConnexion call() {
//                EcoleDAO ecoleDAO = new EcoleDAO();
//                var ecoleOpt = ecoleDAO.findPremiereEcoleActive();
//
//                if (ecoleOpt.isEmpty()) {
//                    return new AuthService.ResultatConnexion(false, "Aucune école active trouvée dans la base de données");
//                }
//
//                Ecole ecole = ecoleOpt.get();
//                System.out.println("🏫 École chargée : " + ecole.getNomEcole() + " (ID: " + ecole.getIdEcole() + ")");
//
//                SessionManager.getInstance().setEcoleCourante(ecole);
//
//                String ipAdresse = obtenirAdresseIpLocale();
//                String nomApp = obtenirInfoAppareil();
//                String userAgentCustom = "KlassUp-DesktopClient/1.0 (" + System.getProperty("os.name") + ")";
//
//                AuthService authService = new AuthService();
//
//                // Si le code2FA est vide (première tentative), authService vérifiera si l'utilisateur requiert le 2FA
//                return authService.seConnecter(stageCourant, login, motDePasse, code2FA, ecole.getIdEcole(), ipAdresse, userAgentCustom, nomApp);
//            }
//        };
//
//        tacheConnexion.setOnSucceeded(event -> {
//            AuthService.ResultatConnexion resultat = tacheConnexion.getValue();
//
//            if (resultat != null && resultat.isSucces()) {
//                SessionManager sessionManager = SessionManager.getInstance();
//
//                if (sessionManager.estConnecte()) {
//                    Utilisateur user = sessionManager.getUtilisateurCourant();
//
//                    System.out.println("✅ Connecté : " + user.getNomComplet());
//                    System.out.println("   Username : " + user.getUsername());
//                    System.out.println("   École : " + sessionManager.getEcoleCourante().getNomEcole());
//
//                    List<Role> roles = new UtilisateurRoleDAO().findRolesByUtilisateur(user.getIdUtilisateur());
//                    if (!roles.isEmpty()) {
//                        String nomsRoles = roles.stream()
//                                .map(Role::getNomRole)
//                                .reduce((a, b) -> a + ", " + b)
//                                .orElse("Aucun");
//                        System.out.println("   Rôles : " + nomsRoles);
//                    }
//
//                    naviguerVersDashboard();
//
//                } else {
//                    BoutonChargement.arreter(btnConnecter);
//                    afficherErreur("Erreur : session non démarrée");
//                }
//
//            } else if (resultat != null && resultat.isRequiert2FA()) {
//                // Interception du besoin de validation 2FA
//                BoutonChargement.arreter(btnConnecter);
//
//                if (container2FA != null) {
//                    container2FA.setVisible(true);
//                    container2FA.setManaged(true);
//                    if (champCode2FA != null) {
//                        champCode2FA.requestFocus();
//                    }
//                }
//
//                afficherErreur("Authentification à deux facteurs requise. Veuillez entrer le code TOTP.");
//
//            } else {
//                BoutonChargement.arreter(btnConnecter);
//                String msg = (resultat != null) ? resultat.getMessage() : "Échec de connexion inconnu";
//                afficherErreur(msg);
//            }
//        });
//
//        tacheConnexion.setOnFailed(event -> {
//            BoutonChargement.arreter(btnConnecter);
//            Throwable exception = tacheConnexion.getException();
//            if (exception != null) {
//                exception.printStackTrace();
//            }
//            afficherErreur("Erreur technique lors de la connexion.");
//        });
//
//        AppExecutor.get().submit(tacheConnexion);
//    }
//    private void naviguerVersDashboard() {
//        try {
//            Stage stage = (Stage) btnConnecter.getScene().getWindow();
//            Parent nouvelleRacine = chargerVue("/org/erpklassup/erpklassup/hello-view.fxml").load();
//
//            stage.setResizable(true);
//            stage.centerOnScreen();
//            stage.setTitle("KlassUp");
//            stage.getScene().setRoot(nouvelleRacine);
//
//        } catch (IOException ex) {
//            BoutonChargement.arreter(btnConnecter);
//            afficherErreur("Impossible de charger le tableau de bord.");
//            System.err.println("Erreur navigation : " + ex.getMessage());
//        }
//    }
//
//    private void chargerStatistiquesGlobales() {
//        try (Connection conn = Database.getConnexion()) {
//
//            String sqlEleves = "SELECT COUNT(*) FROM Eleve";
//            try (PreparedStatement stmt = conn.prepareStatement(sqlEleves);
//                 ResultSet rs = stmt.executeQuery()) {
//                if (rs.next()) {
//                    nbEleves.setText(String.format("%,d", rs.getInt(1)));
//                }
//            }
//
//            String sqlEnseignants = "SELECT COUNT(*) FROM Enseignant";
//            try (PreparedStatement stmt = conn.prepareStatement(sqlEnseignants);
//                 ResultSet rs = stmt.executeQuery()) {
//                if (rs.next()) {
//                    nbEnseignants.setText(String.valueOf(rs.getInt(1)));
//                }
//            }
//
//            String sqlClasses = "SELECT COUNT(*) FROM Classe";
//            try (PreparedStatement stmt = conn.prepareStatement(sqlClasses);
//                 ResultSet rs = stmt.executeQuery()) {
//                if (rs.next()) {
//                    nbClasses.setText(String.valueOf(rs.getInt(1)));
//                }
//            }
//
//        } catch (Exception e) {
//            System.err.println("Erreur lors du chargement des compteurs du Dashboard : " + e.getMessage());
//        }
//    }
//
//    private void afficherErreur(String message) {
//        labelErreur.setText(message);
//        labelErreur.setVisible(true);
//        labelErreur.setManaged(true);
//    }
//
//    public static FXMLLoader chargerVue(String cheminAbsolu) {
//        var url = LoginController.class.getResource(cheminAbsolu);
//        if (url == null) {
//            throw new IllegalStateException("FXML introuvable : " + cheminAbsolu
//                    + " — vérifie qu'il est bien dans src/main/resources et dans le classpath.");
//        }
//        return new FXMLLoader(url);
//    }
//
//    public String obtenirAdresseIpLocale() {
//        try {
//            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
//            while (interfaces != null && interfaces.hasMoreElements()) {
//                NetworkInterface iface = interfaces.nextElement();
//
//                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
//                    continue;
//                }
//
//                Enumeration<InetAddress> addresses = iface.getInetAddresses();
//                while (addresses.hasMoreElements()) {
//                    InetAddress addr = addresses.nextElement();
//
//                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
//                        return addr.getHostAddress();
//                    }
//                }
//            }
//
//            InetAddress localHost = InetAddress.getLocalHost();
//            if (!localHost.isLoopbackAddress()) {
//                return localHost.getHostAddress();
//            }
//        } catch (Exception e) {
//            System.err.println("Impossible de déterminer l'adresse IP locale : " + e.getMessage());
//        }
//
//        return "127.0.0.1";
//    }
//
//    public String obtenirInfoAppareil() {
//        String hostName = "Machine";
//        try {
//            hostName = InetAddress.getLocalHost().getHostName();
//        } catch (Exception ignored) {}
//
//        String osName = System.getProperty("os.name", "Inconnu");
//        String osArch = System.getProperty("os.arch", "");
//
//        return String.format("%s (%s %s)", hostName, osName, osArch).trim();
//    }
//}
//
////package org.erpklassup.erpklassup.controllers.actionPage;
////
////import javafx.concurrent.Task;
////import javafx.fxml.FXML;
////import javafx.fxml.FXMLLoader;
////import javafx.scene.Parent;
////import javafx.scene.Scene;
////import javafx.scene.control.Button;
////import javafx.scene.control.Label;
////import javafx.scene.control.PasswordField;
////import javafx.scene.control.TextField;
////import javafx.scene.layout.HBox;
////import javafx.scene.shape.SVGPath;
////import javafx.stage.Stage;
////import javafx.stage.StageStyle;
////import org.erpklassup.erpklassup.BoutonChargement;
////import org.erpklassup.erpklassup.HelloApplication;
////import org.erpklassup.erpklassup.WindowsTitleBar;
////import org.erpklassup.erpklassup.dao.EcoleDAO;
////import org.erpklassup.erpklassup.dao.UtilisateurDAO;
////import org.erpklassup.erpklassup.dao.UtilisateurRoleDAO;
////import org.erpklassup.erpklassup.models.Ecole;
////import org.erpklassup.erpklassup.models.Role;
////import org.erpklassup.erpklassup.models.Utilisateur;
////import org.erpklassup.erpklassup.service.AppExecutor;
////import org.erpklassup.erpklassup.service.AuthService;
////import org.erpklassup.erpklassup.service.SessionManager;
////import org.erpklassup.erpklassup.Database;
////import org.erpklassup.erpklassup.util.StageHelper;
////import org.erpklassup.erpklassup.util.ToastNotification;
////
////
////import java.io.IOException;
////import java.net.Inet4Address;
////import java.net.InetAddress;
////import java.net.NetworkInterface;
////import java.sql.Connection;
////import java.sql.PreparedStatement;
////import java.sql.ResultSet;
////import java.sql.SQLException;
////import java.util.Enumeration;
////import java.util.List;
////import static okhttp3.internal.Util.userAgent;
////
////
////public class LoginController {
////
////    @FXML private Button btnConnecter;
////    @FXML private HBox loginAppBar;
////
////    @FXML private TextField champEmail;
////    @FXML private PasswordField champMotDePasse;
////    @FXML private TextField champMotDePasseVisible;
////    @FXML private SVGPath iconeOeil;
////    @FXML private Label labelErreur;
////    @FXML private Label nbEleves;
////    @FXML private Label nbEnseignants;
////    @FXML private Label nbClasses;
////
////    private static final String APP_BAR_COLOR = "#16213A";
////
////    private boolean motDePasseVisible = false;
////
////    private static final String OEIL_OUVERT =
////            "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z";
////    private static final String OEIL_FERME =
////            "M17.94 17.94A10.94 10.94 0 0 1 12 20c-7 0-11-8-11-8a18.5 18.5 0 0 1 5.06-5.94 " +
////                    "M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19 " +
////                    "M1 1l22 22";
////
////    @FXML
////    public void initialize() {
////        chargerStatistiquesGlobales();
////    }
////
////    @FXML
////    public void handleClose() {
////        Stage stage = (Stage) loginAppBar.getScene().getWindow();
////        stage.close();
////    }
////
////    @FXML
////    public void toggleMotDePasse() {
////        motDePasseVisible = !motDePasseVisible;
////
////        if (motDePasseVisible) {
////            champMotDePasseVisible.setText(champMotDePasse.getText());
////            champMotDePasse.setVisible(false);
////            champMotDePasse.setManaged(false);
////            champMotDePasseVisible.setVisible(true);
////            champMotDePasseVisible.setManaged(true);
////            iconeOeil.setContent(OEIL_FERME);
////        } else {
////            champMotDePasse.setText(champMotDePasseVisible.getText());
////            champMotDePasseVisible.setVisible(false);
////            champMotDePasseVisible.setManaged(false);
////            champMotDePasse.setVisible(true);
////            champMotDePasse.setManaged(true);
////            iconeOeil.setContent(OEIL_OUVERT);
////        }
////    }
////
////    @FXML
//////    public void handleLogin() {
//////        String login = champEmail.getText().trim();
//////        String motDePasse = motDePasseVisible ? champMotDePasseVisible.getText() : champMotDePasse.getText();
//////        if (login.isEmpty() || motDePasse.isEmpty()) {
//////            afficherErreur("Veuillez remplir tous les champs.");
//////            return;
//////        }
//////        String ecole = "ECO_001";
//////        BoutonChargement.demarrer(btnConnecter, "Connexion en cours...");
//////        Task<AuthService.ResultatConnexion> tacheConnexion = new Task<>() {
//////            @Override
//////            protected AuthService.ResultatConnexion call() {
//////                return new AuthService().seConnecter(login, motDePasse, ecole);
//////            }
//////        };
//////        tacheConnexion.setOnSucceeded(e -> {
//////            AuthService.ResultatConnexion resultat = tacheConnexion.getValue();
//////            if (resultat.isSucces()) {
//////                try {
//////                    Stage stage = (Stage) btnConnecter.getScene().getWindow();
//////                    Parent nouvelleRacine = chargerVue("/org/erpklassup/erpklassup/hello-view.fxml").load();
//////                    stage.setResizable(true);
//////                    stage.setWidth(1180);
//////                    stage.centerOnScreen();
//////                    stage.setTitle("KlassUp");
//////                    stage.getScene().setRoot(nouvelleRacine);
//////                } catch (IOException ex) {
//////                    BoutonChargement.arreter(btnConnecter);
//////                    afficherErreur("Impossible de charger le tableau de bord.");
//////                }
//////            } else {
//////                BoutonChargement.arreter(btnConnecter);
//////                afficherErreur(resultat.getMessage());
//////            }
//////        });
//////        tacheConnexion.setOnFailed(e -> {
//////            BoutonChargement.arreter(btnConnecter);
//////            tacheConnexion.getException().printStackTrace();
//////            afficherErreur("Erreur technique lors de la connexion.");
//////        });
//////        AppExecutor.get().submit(tacheConnexion);
//////    }
////
//////    public void handleLogin() {
//////        String login = champEmail.getText().trim();
//////        String motDePasse = motDePasseVisible ? champMotDePasseVisible.getText() : champMotDePasse.getText();
//////
//////        if (login.isEmpty() || motDePasse.isEmpty()) {
//////            afficherErreur("Veuillez remplir tous les champs.");
//////            return;
//////        }
//////
//////        String ecole = "ECO_001";
//////        BoutonChargement.demarrer(btnConnecter, "Connexion en cours...");
//////
//////        Task<AuthService.ResultatConnexion> tacheConnexion = new Task<>() {
//////            @Override
//////            protected AuthService.ResultatConnexion call() {
//////                return new AuthService().seConnecter(login, motDePasse, ecole);
//////            }
//////        };
//////
//////        tacheConnexion.setOnSucceeded(event -> {
//////            AuthService.ResultatConnexion resultat = tacheConnexion.getValue();
//////
//////            if (resultat.isSucces()) {
//////                SessionManager sessionManager = SessionManager.getInstance();
//////
//////                if (sessionManager.estConnecte()) {
//////                    Utilisateur user = sessionManager.getUtilisateurCourant();
//////
//////                    // ✅ LOG SANS getIdRole
//////                    System.out.println("✅ Connecté en tant que : " + user.getNomComplet());
//////                    System.out.println("   Username : " + user.getUsername());
//////
//////                    // ✅ Récupérer les rôles via UtilisateurRoleDAO
//////                    List<Role> roles = new UtilisateurRoleDAO()
//////                            .findRolesByUtilisateur(user.getIdUtilisateur());
//////                    if (!roles.isEmpty()) {
//////                        String nomsRoles = roles.stream()
//////                                .map(Role::getNomRole)
//////                                .reduce((a, b) -> a + ", " + b)
//////                                .orElse("Aucun");
//////                        System.out.println("   Rôles : " + nomsRoles);
//////                    }
//////
//////                    // ✅ Toast de bienvenue
//////                    ToastNotification.succes(
//////                            StageHelper.getStage(btnConnecter),
//////                            "Bienvenue " + user.getNomComplet() + " !"
//////                    );
//////
//////                    naviguerVersDashboard();
//////
//////                } else {
//////                    BoutonChargement.arreter(btnConnecter);
//////                    afficherErreur("Erreur : session non démarrée");
//////                }
//////
//////            } else {
//////                BoutonChargement.arreter(btnConnecter);
//////                afficherErreur(resultat.getMessage());
//////            }
//////        });
//////
//////        tacheConnexion.setOnFailed(event -> {
//////            BoutonChargement.arreter(btnConnecter);
//////            System.err.println("Erreur de connexion : " + tacheConnexion.getException().getMessage());
//////            afficherErreur("Erreur technique lors de la connexion.");
//////        });
//////
//////        AppExecutor.get().submit(tacheConnexion);
//////    }
////
////    public void handleLogin() {
////        String login = champEmail.getText().trim();
////        String motDePasse = motDePasseVisible ? champMotDePasseVisible.getText() : champMotDePasse.getText();
////
////        if (login.isEmpty() || motDePasse.isEmpty()) {
////            afficherErreur("Veuillez remplir tous les champs.");
////            return;
////        }
////
////        // Récupération de la fenêtre JavaFX courante pour les ToastNotifications
////        Stage stageCourant = StageHelper.getStage(btnConnecter);
////
////        BoutonChargement.demarrer(btnConnecter, "Connexion en cours...");
////
////        // ✅ TÂCHE : Charger l'école depuis la BD + Se connecter
////        Task<AuthService.ResultatConnexion> tacheConnexion = new Task<>() {
////            @Override
////            protected AuthService.ResultatConnexion call() {
////                // 1. Charger l'école (une seule école en BD)
////                EcoleDAO ecoleDAO = new EcoleDAO();
////                var ecoleOpt = ecoleDAO.findPremiereEcoleActive();
////
////                if (ecoleOpt.isEmpty()) {
////                    return new AuthService.ResultatConnexion(false, "Aucune école active trouvée dans la base de données");
////                }
////
////                Ecole ecole = ecoleOpt.get();
////                System.out.println("🏫 École chargée : " + ecole.getNomEcole() + " (ID: " + ecole.getIdEcole() + ")");
////
////                // 2. Stocker l'école dans SessionManager
////                SessionManager.getInstance().setEcoleCourante(ecole);
////
////                // 3. Récupérer les informations système / réseau (ex: IP locale et Client App)
////                String ipAdresse = obtenirAdresseIpLocale(); // Peut être remplacé par une utilitaire réseau local
////                String nomApp  = obtenirInfoAppareil();
////
////                // 4. Se connecter avec AuthService mis à jour (prend le stage en paramètre)
////                AuthService authService = new AuthService();
////                return authService.seConnecter(stageCourant, login, motDePasse, ecole.getIdEcole(), ipAdresse, userAgent, nomApp);
////            }
////        };
////
////        tacheConnexion.setOnSucceeded(event -> {
////            AuthService.ResultatConnexion resultat = tacheConnexion.getValue();
////
////            if (resultat.isSucces()) {
////                SessionManager sessionManager = SessionManager.getInstance();
////
////                if (sessionManager.estConnecte()) {
////                    Utilisateur user = sessionManager.getUtilisateurCourant();
////
////                    System.out.println("✅ Connecté : " + user.getNomComplet());
////                    System.out.println("   Username : " + user.getUsername());
////                    System.out.println("   École : " + sessionManager.getEcoleCourante().getNomEcole());
////
////                    // Log des rôles pour le débogage
////                    List<Role> roles = new UtilisateurRoleDAO().findRolesByUtilisateur(user.getIdUtilisateur());
////                    if (!roles.isEmpty()) {
////                        String nomsRoles = roles.stream()
////                                .map(Role::getNomRole)
////                                .reduce((a, b) -> a + ", " + b)
////                                .orElse("Aucun");
////                        System.out.println("   Rôles : " + nomsRoles);
////                    }
////
////                    // Le Toast de succès est automatiquement affiché par AuthService.seConnecter()
////                    naviguerVersDashboard();
////
////                } else {
////                    BoutonChargement.arreter(btnConnecter);
////                    afficherErreur("Erreur : session non démarrée");
////                }
////
////            } else {
////                BoutonChargement.arreter(btnConnecter);
////                // Les erreurs d'authentification ou verrous sont gérées et affichées par les Toasts dans AuthService
////                afficherErreur(resultat.getMessage());
////            }
////        });
////
////        tacheConnexion.setOnFailed(event -> {
////            BoutonChargement.arreter(btnConnecter);
////            Throwable exception = tacheConnexion.getException();
////            System.err.println("Erreur de connexion : " + (exception != null ? exception.getMessage() : "Inconnue"));
////            afficherErreur("Erreur technique lors de la connexion.");
////        });
////
////        AppExecutor.get().submit(tacheConnexion);
////    }
////
////    private void naviguerVersDashboard() {
////        try {
////            Stage stage = (Stage) btnConnecter.getScene().getWindow();
////            Parent nouvelleRacine = chargerVue("/org/erpklassup/erpklassup/hello-view.fxml").load();
////
////            stage.setResizable(true);
////            stage.centerOnScreen();
////            stage.setTitle("KlassUp");
////            stage.getScene().setRoot(nouvelleRacine);
////
////        } catch (IOException ex) {
////            BoutonChargement.arreter(btnConnecter);
////            afficherErreur("Impossible de charger le tableau de bord.");
////            System.err.println("Erreur navigation : " + ex.getMessage());
////        }
////    }
////
////    private void chargerStatistiquesGlobales() {
////        try (Connection conn = Database.getConnexion()) {
////
////            // 1. Compter les élèves
////            String sqlEleves = "SELECT COUNT(*) FROM Eleve";
////            try (PreparedStatement stmt = conn.prepareStatement(sqlEleves);
////                 ResultSet rs = stmt.executeQuery()) {
////                if (rs.next()) {
////                    nbEleves.setText(String.format("%,d", rs.getInt(1)));
////                }
////            }
////
////            // 2. Compter les enseignants
////            String sqlEnseignants = "SELECT COUNT(*) FROM Enseignant";
////            try (PreparedStatement stmt = conn.prepareStatement(sqlEnseignants);
////                 ResultSet rs = stmt.executeQuery()) {
////                if (rs.next()) {
////                    nbEnseignants.setText(String.valueOf(rs.getInt(1)));
////                }
////            }
////
////            // 3. Compter les classes
////            String sqlClasses = "SELECT COUNT(*) FROM Classe";
////            try (PreparedStatement stmt = conn.prepareStatement(sqlClasses);
////                 ResultSet rs = stmt.executeQuery()) {
////                if (rs.next()) {
////                    nbClasses.setText(String.valueOf(rs.getInt(1)));
////                }
////            }
////
////        } catch (Exception e) {
////            System.err.println("Erreur lors du chargement des compteurs du Dashboard : " + e.getMessage());
////            e.printStackTrace();
////        }
////    }
////    private void afficherErreur(String message) {
////        labelErreur.setText(message);
////        labelErreur.setVisible(true);
////        labelErreur.setManaged(true);
////    }
////
////    public static FXMLLoader chargerVue(String cheminAbsolu) {
////        var url = LoginController.class.getResource(cheminAbsolu);
////        if (url == null) {
////            throw new IllegalStateException("FXML introuvable : " + cheminAbsolu
////                    + " — vérifie qu'il est bien dans src/main/resources et dans le classpath.");
////        }
////        return new FXMLLoader(url);
////    }
////
////    public String obtenirAdresseIpLocale() {
////        try {
////            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
////            while (interfaces != null && interfaces.hasMoreElements()) {
////                NetworkInterface iface = interfaces.nextElement();
////
////                // Ignorer les interfaces inactives, de bouclage (loopback) ou virtuelles (ex: Docker, VPN, VirtualBox)
////                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
////                    continue;
////                }
////
////                Enumeration<InetAddress> addresses = iface.getInetAddresses();
////                while (addresses.hasMoreElements()) {
////                    InetAddress addr = addresses.nextElement();
////
////                    // On filtre pour ne garder que les adresses IPv4 de site/réseau local
////                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
////                        return addr.getHostAddress();
////                    }
////                }
////            }
////
////            // Tentative de fallback simple si le parcours d'interfaces n'a pas abouti
////            InetAddress localHost = InetAddress.getLocalHost();
////            if (!localHost.isLoopbackAddress()) {
////                return localHost.getHostAddress();
////            }
////        } catch (Exception e) {
////            System.err.println("Impossible de déterminer l'adresse IP locale : " + e.getMessage());
////        }
////
////        return "127.0.0.1";
////    }
////
////    /**
////     * Génère une chaîne descriptive complète de la machine (Nom d'hôte + OS + Architecture).
////     */
////    public String obtenirInfoAppareil() {
////        String hostName = "Machine";
////        try {
////            hostName = InetAddress.getLocalHost().getHostName();
////        } catch (Exception ignored) {}
////
////        String osName = System.getProperty("os.name", "Inconnu");
////        String osArch = System.getProperty("os.arch", "");
////
////        return String.format("%s (%s %s)", hostName, osName, osArch).trim();
////    }
////}