package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import org.erpklassup.erpklassup.dto.PermissionOption;
import org.erpklassup.erpklassup.dto.RoleOption;
import org.erpklassup.erpklassup.service.RoleService;
import org.erpklassup.erpklassup.service.SessionManager;

import java.net.URL;
import java.util.*;

public class RolesPermissionsTabController implements Initializable {

    @FXML private ListView<RoleOption> listRoles;
    @FXML private Button btnNouveauRole;
    @FXML private Label labelRoleSelectionne;
    @FXML private Label labelRoleDescription;
    @FXML private Button btnRenommerRole;
    @FXML private Button btnSupprimerRole;
    @FXML private Accordion accordionModules;
    @FXML private Label labelModificationsNonEnregistrees;
    @FXML private Button btnAnnulerPermissions;
    @FXML private Button btnEnregistrerPermissions;

    private final RoleService service = new RoleService();
    private final Map<String, CheckBox> casesParIdPermission = new LinkedHashMap<>();

    private RoleOption roleSelectionne;
    private boolean enChargement = false;

    private String idEcoleCourante;
    private String idUtilisateurConnecte;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        listRoles.setItems(FXCollections.observableArrayList());
        listRoles.setPlaceholder(new Label("Chargement..."));
        activerPanneauDroit(false);

        listRoles.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            roleSelectionne = nouveau;
            if (nouveau != null) chargerRole(nouveau); else activerPanneauDroit(false);
        });

        btnNouveauRole.setOnAction(e -> creerNouveauRole());
        btnRenommerRole.setOnAction(e -> renommerRoleSelectionne());
        btnSupprimerRole.setOnAction(e -> supprimerRoleSelectionne());
        btnEnregistrerPermissions.setOnAction(e -> enregistrerPermissions());
        btnAnnulerPermissions.setOnAction(e -> { if (roleSelectionne != null) chargerRole(roleSelectionne); });

        chargerPermissionsDisponibles();
        rafraichirContexte(); // capture le contexte au chargement initial (cas où la vue n'est pas mise en cache)
    }

    /**
     * À appeler à chaque fois que cet onglet redevient visible (depuis ta navigation /
     * ton système de cache de vues), pour reprendre l'idEcole et l'utilisateur courants
     * — indispensable si le contrôleur est instancié UNE SEULE FOIS avant la connexion.
     */
    public void rafraichirContexte() {
        SessionManager session = SessionManager.getInstance();
        this.idEcoleCourante = session.getIdEcoleCourante();
        this.idUtilisateurConnecte = session.getUtilisateurCourant() != null
                ? session.getUtilisateurCourant().getIdUtilisateur() : null;

        if (idEcoleCourante == null) {
            listRoles.setItems(FXCollections.observableArrayList());
            listRoles.setPlaceholder(new Label("Aucune école active — connectez-vous d'abord."));
            activerPanneauDroit(false);
            return;
        }

        chargerRoles();
    }

    private void activerPanneauDroit(boolean actif) {
        accordionModules.setDisable(!actif);
        btnRenommerRole.setDisable(!actif);
        btnSupprimerRole.setDisable(!actif);
        btnEnregistrerPermissions.setDisable(!actif);
        btnAnnulerPermissions.setDisable(!actif);
        if (!actif) {
            labelRoleSelectionne.setText("Sélectionnez un rôle");
            labelRoleDescription.setText("Les droits ci-dessous s'appliquent à tous les utilisateurs de ce rôle.");
        }
    }

    private void chargerPermissionsDisponibles() {
        service.listerPermissionsAsync(
                permissions -> Platform.runLater(() -> construireAccordion(permissions)),
                erreur -> Platform.runLater(erreur::printStackTrace));
    }

    private void chargerRoles() {
        service.listerRolesAsync(idEcoleCourante,
                roles -> Platform.runLater(() -> {
                    listRoles.setItems(FXCollections.observableArrayList(roles));
                    listRoles.setPlaceholder(new Label("Aucun rôle pour cette école — créez-en un."));
                    if (!roles.isEmpty()) {
                        listRoles.getSelectionModel().selectFirst(); // active le panneau automatiquement
                    }
                }),
                erreur -> Platform.runLater(erreur::printStackTrace));
    }

    private void chargerRole(RoleOption role) {
        activerPanneauDroit(true);
        labelRoleSelectionne.setText(role.nomRole());
        labelRoleDescription.setText(role.description() != null && !role.description().isBlank()
                ? role.description() : "Les droits ci-dessous s'appliquent à tous les utilisateurs de ce rôle.");

        service.chargerPermissionsDuRoleAsync(role.idRole(),
                idsActifs -> Platform.runLater(() -> appliquerSelection(idsActifs)),
                erreur -> Platform.runLater(erreur::printStackTrace));
    }

    private void appliquerSelection(Set<String> idsActifs) {
        enChargement = true;
        casesParIdPermission.forEach((id, caseACocher) -> caseACocher.setSelected(idsActifs.contains(id)));
        enChargement = false;
        labelModificationsNonEnregistrees.setText("");
    }

    private void construireAccordion(List<PermissionOption> permissions) {
        Map<String, List<PermissionOption>> parCategorie = new LinkedHashMap<>();
        for (PermissionOption p : permissions) parCategorie.computeIfAbsent(p.categorie(), k -> new ArrayList<>()).add(p);

        accordionModules.getPanes().clear();
        casesParIdPermission.clear();

        for (var entree : parCategorie.entrySet()) {
            List<CheckBox> cases = new ArrayList<>();
            VBox corps = new VBox(6);
            corps.setPadding(new Insets(8, 4, 8, 4));
            corps.getStyleClass().add("permissions-module-body");

            for (PermissionOption permission : entree.getValue()) {
                CheckBox caseACocher = new CheckBox(permission.libelle());
                caseACocher.getStyleClass().add("permission-check");
                caseACocher.selectedProperty().addListener((obs, a, n) -> { if (!enChargement) signalerModification(); });
                cases.add(caseACocher);
                casesParIdPermission.put(permission.idPermission(), caseACocher);
            }

            CheckBox toutSelectionner = creerCaseSelectionnerTout(cases);
            corps.getChildren().add(toutSelectionner);
            corps.getChildren().addAll(cases);

            TitledPane pane = new TitledPane(entree.getKey(), corps);
            pane.getStyleClass().add("permission-titled-pane");
            accordionModules.getPanes().add(pane);
        }

        if (!accordionModules.getPanes().isEmpty()) accordionModules.setExpandedPane(accordionModules.getPanes().get(0));
        if (roleSelectionne != null) chargerRole(roleSelectionne);
    }

    private CheckBox creerCaseSelectionnerTout(List<CheckBox> cases) {
        CheckBox toutSelectionner = new CheckBox("Tout sélectionner");
        toutSelectionner.getStyleClass().add("check-select-all");
        toutSelectionner.setOnAction(e -> {
            boolean coche = toutSelectionner.isSelected();
            cases.forEach(c -> c.setSelected(coche));
        });
        cases.forEach(c -> c.selectedProperty().addListener((obs, a, n) -> {
            long nombreCochees = cases.stream().filter(CheckBox::isSelected).count();
            toutSelectionner.setIndeterminate(nombreCochees > 0 && nombreCochees < cases.size());
            toutSelectionner.setSelected(nombreCochees == cases.size());
        }));
        return toutSelectionner;
    }

    private void signalerModification() {
        labelModificationsNonEnregistrees.setText("Modifications non enregistrées");
    }

    private void enregistrerPermissions() {
        if (roleSelectionne == null) return;
        Set<String> idsCoches = new HashSet<>();
        casesParIdPermission.forEach((id, c) -> { if (c.isSelected()) idsCoches.add(id); });

        btnEnregistrerPermissions.setDisable(true);
        service.enregistrerPermissionsAsync(roleSelectionne.idRole(), idEcoleCourante, idsCoches, idUtilisateurConnecte,
                () -> Platform.runLater(() -> {
                    labelModificationsNonEnregistrees.setText("Enregistré ✓");
                    btnEnregistrerPermissions.setDisable(false);
                }),
                erreur -> Platform.runLater(() -> {
                    btnEnregistrerPermissions.setDisable(false);
                    new Alert(Alert.AlertType.ERROR, "Échec de l'enregistrement des droits.").showAndWait();
                }));
    }

    private void creerNouveauRole() {
        if (idEcoleCourante == null) {
            new Alert(Alert.AlertType.WARNING, "Aucune école active. Reconnectez-vous.").showAndWait();
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nouveau rôle");
        dialog.setHeaderText("Nom du nouveau rôle");
        dialog.setContentText("Nom :");
        dialog.showAndWait().filter(nom -> !nom.isBlank()).ifPresent(nom ->
                service.creerRoleAsync(idEcoleCourante, nom.trim(), null,
                        roleCree -> Platform.runLater(this::chargerRoles),
                        erreur -> Platform.runLater(() -> new Alert(Alert.AlertType.ERROR,
                                "Impossible de créer le rôle (nom déjà utilisé ?)").showAndWait())));
    }

    private void renommerRoleSelectionne() {
        if (roleSelectionne == null) return;
        TextInputDialog dialog = new TextInputDialog(roleSelectionne.nomRole());
        dialog.setTitle("Renommer le rôle");
        dialog.setHeaderText("Nouveau nom du rôle");
        dialog.setContentText("Nom :");
        dialog.showAndWait().filter(nom -> !nom.isBlank()).ifPresent(nom ->
                service.renommerRoleAsync(roleSelectionne.idRole(), nom.trim(), roleSelectionne.description(),
                        () -> Platform.runLater(this::chargerRoles),
                        erreur -> Platform.runLater(erreur::printStackTrace)));
    }

    private void supprimerRoleSelectionne() {
        if (roleSelectionne == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le rôle \"" + roleSelectionne.nomRole() + "\" ? Les utilisateurs concernés perdront cet accès.",
                ButtonType.YES, ButtonType.NO);
        confirmation.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b ->
                service.supprimerRoleAsync(roleSelectionne.idRole(),
                        () -> Platform.runLater(() -> { roleSelectionne = null; chargerRoles(); activerPanneauDroit(false); }),
                        erreur -> Platform.runLater(erreur::printStackTrace)));
    }
}