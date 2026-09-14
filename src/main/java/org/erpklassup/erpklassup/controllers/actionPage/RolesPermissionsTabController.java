package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.erpklassup.erpklassup.dto.PermissionOption;
import org.erpklassup.erpklassup.dto.RoleOption;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.RoleService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.*;

import java.net.URL;
import java.util.*;

public class RolesPermissionsTabController implements Initializable, VueDisposable {

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
    private final AuditService auditService = new AuditService();

    private final Map<String, CheckBox> casesParIdPermission = new LinkedHashMap<>();
    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;
    private final Runnable ecouteurI18n = this::rafraichirTextes;

    private RoleOption roleSelectionne;
    private boolean enChargement = false;
    private SessionManager session = SessionManager.getInstance();
    private String idEcoleCourante = session.getIdEcoleCourante();
    private String idUtilisateurConnecte;

    private I18nManager i18n() {
        return I18nManager.getInstance();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        listRoles.setItems(FXCollections.observableArrayList());
        listRoles.setPlaceholder(new Label(i18n().t("roles.loading")));
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

        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
        appliquerControlesAcces();

        // ✅ S'abonner aux changements de langue
        i18n().ecouterChangement(ecouteurI18n);

        chargerPermissionsDisponibles();
        rafraichirContexte();
    }

    @Override
    public void disposer() {
        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
        i18n().arreterEcoute(ecouteurI18n);
    }

    private void rafraichirTextes() {
        // Le placeholder
        if (listRoles.getItems().isEmpty()) {
            listRoles.setPlaceholder(new Label(i18n().t("roles.empty")));
        }
        // Les titres des modules (Accordion)
        if (accordionModules != null) {
            accordionModules.getPanes().forEach(p -> {
                // Le titre vient des permissions BDD → pas traduisible ici
            });
        }
        // Le label de sélection
        if (roleSelectionne == null) {
            labelRoleSelectionne.setText(i18n().t("roles.select_hint"));
            labelRoleDescription.setText(i18n().t("roles.description_hint"));
        }
    }

    private void appliquerControlesAcces() {
        ControleAcces.appliquerAction(btnNouveauRole, "role.creer");
        ControleAcces.appliquerAction(btnRenommerRole, "role.renommer");
        ControleAcces.appliquerAction(btnSupprimerRole, "role.supprimer");
        ControleAcces.appliquerAction(btnEnregistrerPermissions, "role.modifier");
    }

    public void rafraichirContexte() {
        SessionManager session = SessionManager.getInstance();
        this.idEcoleCourante = session.getIdEcoleCourante();
        this.idUtilisateurConnecte = session.getUtilisateurCourant() != null
                ? session.getUtilisateurCourant().getIdUtilisateur() : null;

        if (idEcoleCourante == null) {
            listRoles.setItems(FXCollections.observableArrayList());
            listRoles.setPlaceholder(new Label(i18n().t("roles.no_school")));
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
            labelRoleSelectionne.setText(i18n().t("roles.select_hint"));
            labelRoleDescription.setText(i18n().t("roles.description_hint"));
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
                    listRoles.setPlaceholder(new Label(i18n().t("roles.empty")));
                    if (!roles.isEmpty()) {
                        listRoles.getSelectionModel().selectFirst();
                    }
                }),
                erreur -> Platform.runLater(erreur::printStackTrace));
    }

    private void chargerRole(RoleOption role) {
        activerPanneauDroit(true);
        labelRoleSelectionne.setText(role.nomRole());
        labelRoleDescription.setText(role.description() != null && !role.description().isBlank()
                ? role.description() : i18n().t("roles.description_hint"));

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
        for (PermissionOption p : permissions)
            parCategorie.computeIfAbsent(p.categorie(), k -> new ArrayList<>()).add(p);

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
                caseACocher.selectedProperty().addListener((obs, a, n) -> {
                    if (!enChargement) signalerModification();
                });
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

        if (!accordionModules.getPanes().isEmpty())
            accordionModules.setExpandedPane(accordionModules.getPanes().get(0));
        if (roleSelectionne != null) chargerRole(roleSelectionne);
    }

    private CheckBox creerCaseSelectionnerTout(List<CheckBox> cases) {
        CheckBox toutSelectionner = new CheckBox(i18n().t("roles.select_all"));
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
        labelModificationsNonEnregistrees.setText(i18n().t("roles.unsaved_changes"));
    }

    private void enregistrerPermissions() {
        if (roleSelectionne == null) return;
        Set<String> idsCoches = new HashSet<>();
        casesParIdPermission.forEach((id, c) -> {
            if (c.isSelected()) idsCoches.add(id);
        });

        Stage stage = StageHelper.getStage(btnEnregistrerPermissions);
        btnEnregistrerPermissions.setDisable(true);

        service.enregistrerPermissionsAsync(roleSelectionne.idRole(), idEcoleCourante, idsCoches, idUtilisateurConnecte,
                () -> Platform.runLater(() -> {
                    labelModificationsNonEnregistrees.setText(i18n().t("roles.saved"));
                    btnEnregistrerPermissions.setDisable(false);

                    auditService.tracerActionAsync(
                            "HABILITATIONS",
                            "MODIFICATION_PERMISSIONS",
                            "Mise à jour des permissions du rôle : " + roleSelectionne.nomRole()
                                    + " (" + idsCoches.size() + " autorisations octroyées)"
                    );

                    ToastNotification.succes(stage, i18n().t("roles.permissions_saved"));
                }),
                erreur -> Platform.runLater(() -> {
                    btnEnregistrerPermissions.setDisable(false);
                    ToastNotification.erreur(stage, i18n().t("roles.save_error"));
                }));
    }

    private void creerNouveauRole() {
        String ecoleId = SessionManager.getInstance().getIdEcoleCourante();

        if (ecoleId == null || ecoleId.isBlank()) {
            Stage stage = StageHelper.getStage(btnNouveauRole);
            ToastNotification.avertissement(stage, i18n().t("roles.no_school_short"));
            return;
        }

        String fxmlPath = "/org/erpklassup/erpklassup/view/creer-role-view.fxml";
        RoleModalController controller = ModalUtil.ouvrirModal(
                getClass(),
                fxmlPath,
                i18n().t("roles.modal.create.title"),
                btnNouveauRole.getScene().getWindow(),
                480,
                396.8
        );
        if (controller != null) {
            controller.initCreation(ecoleId, this::chargerRoles);
        }
    }

    private void renommerRoleSelectionne() {
        if (roleSelectionne == null) return;

        String fxmlPath = "/org/erpklassup/erpklassup/view/creer-role-view.fxml";
        RoleModalController controller = ModalUtil.ouvrirModal(
                getClass(),
                fxmlPath,
                i18n().t("roles.modal.edit.title"),
                btnNouveauRole.getScene().getWindow(),
                480,
                396.8
        );
        if (controller != null) {
            controller.initEdition(idEcoleCourante, roleSelectionne, this::chargerRoles);
        }
    }

    private void supprimerRoleSelectionne() {
        if (roleSelectionne == null) return;
        Stage stage = StageHelper.getStage(btnSupprimerRole);

        boolean confirme = AlertUtil.afficherConfirmation(
                i18n().t("roles.confirm_delete.title"),
                i18n().t("roles.confirm_delete.message", roleSelectionne.nomRole()),
                stage
        );
        if (confirme) {
            String nomRoleSupprime = roleSelectionne.nomRole();

            service.supprimerRoleAsync(roleSelectionne.idRole(),
                    () -> Platform.runLater(() -> {
                        roleSelectionne = null;
                        chargerRoles();
                        activerPanneauDroit(false);

                        auditService.tracerActionAsync(
                                "HABILITATIONS",
                                "SUPPRESSION_ROLE",
                                "Suppression du rôle : " + nomRoleSupprime
                        );

                        ToastNotification.succes(stage, i18n().t("roles.deleted", nomRoleSupprime));
                    }),
                    erreur -> Platform.runLater(() -> ToastNotification.erreur(stage, i18n().t("roles.delete_error"))));
        }
    }
}