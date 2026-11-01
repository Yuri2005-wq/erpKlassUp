package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.erpklassup.erpklassup.dto.PermissionOption;
import org.erpklassup.erpklassup.dto.RoleOption;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.RoleService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.util.*;

import java.net.URL;
import java.util.*;

public class RolesPermissionsTabController implements Initializable, VueDisposable {

    /* ============================================================
       CHAMPS FXML
       ============================================================ */
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

    /* ============================================================
       CONSTANTES
       ============================================================ */
    private static final Duration PERIODE_REFRESH_ROLES = Duration.seconds(10);

    /* ============================================================
       SERVICES & ÉTAT
       ============================================================ */
    private final RoleService service = new RoleService();
    private final AuditService auditService = new AuditService();

    private final Map<String, CheckBox> casesParIdPermission = new LinkedHashMap<>();
    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;
    private final Runnable ecouteurI18n = this::rafraichirTextes;

    private RoleOption roleSelectionne;
    private AutoRefreshService<List<RoleOption>> autoRefreshRoles;

    private boolean enChargement = false;
    private SessionManager session = SessionManager.getInstance();
    private String idEcoleCourante = session.getIdEcoleCourante();
    private String idUtilisateurConnecte;

    /* ============================================================
       RACCOURCI i18n
       ============================================================ */
    private I18nManager i18n() {
        return I18nManager.getInstance();
    }

    public Button getBtnNouveauRole() {
        return btnNouveauRole;
    }

    /* ============================================================
       INITIALISATION
       ============================================================ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        listRoles.setItems(FXCollections.observableArrayList());
        listRoles.setPlaceholder(new Label(i18n().t("roles.loading")));
        activerPanneauDroit(false);

        listRoles.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            roleSelectionne = nouveau;
            if (nouveau != null) {
                chargerRole(nouveau);
            } else {
                activerPanneauDroit(false);
            }
        });

        btnNouveauRole.setOnAction(e -> creerNouveauRole());
        btnRenommerRole.setOnAction(e -> renommerRoleSelectionne());
        btnSupprimerRole.setOnAction(e -> supprimerRoleSelectionne());

        btnEnregistrerPermissions.setOnAction(e -> enregistrerPermissions());
        btnAnnulerPermissions.setOnAction(e -> {
            if (roleSelectionne != null) chargerRole(roleSelectionne);
        });

        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
        appliquerControlesAcces();

        // S'abonner aux changements de langue
        i18n().ecouterChangement(ecouteurI18n);

        chargerPermissionsDisponibles();
        rafraichirContexte();
    }

    /* ============================================================
       DISPOSE — Libération propre des ressources
       ============================================================ */
    @Override
    public void disposer() {
        // 1. Arrêter le rafraîchissement automatique (évite les fuites mémoire)
        if (autoRefreshRoles != null) {
            autoRefreshRoles.dispose();
            autoRefreshRoles = null;
        }
        // 2. Se désabonner des événements globaux
        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
        i18n().arreterEcoute(ecouteurI18n);
    }

    /* ============================================================
       i18n
       ============================================================ */
    private void rafraichirTextes() {
        // Le placeholder
        if (listRoles.getItems().isEmpty()) {
            listRoles.setPlaceholder(new Label(i18n().t("roles.empty")));
        }
        // Le label de sélection (si aucun rôle sélectionné)
        if (roleSelectionne == null) {
            labelRoleSelectionne.setText(i18n().t("roles.select_hint"));
            labelRoleDescription.setText(i18n().t("roles.description_hint"));
        }
        // Rafraîchir la liste (les cellules peuvent contenir du texte traduit)
        listRoles.refresh();
    }

    /* ============================================================
       CONTRÔLES D'ACCÈS
       ============================================================ */
    private void appliquerControlesAcces() {
        ControleAcces.appliquerAction(btnNouveauRole, "role.creer");
        ControleAcces.appliquerAction(btnRenommerRole, "role.renommer");
        ControleAcces.appliquerAction(btnSupprimerRole, "role.supprimer");
        ControleAcces.appliquerAction(btnEnregistrerPermissions, "user.creer");
    }

    /* ============================================================
       RAFRAÎCHISSEMENT DU CONTEXTE
       ============================================================ */
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
        demarrerRafraichissementAuto();
    }

    /* ============================================================
       AUTO-REFRESH DE LA LISTE DES RÔLES
       ============================================================ */
    /**
     * Démarre le rafraîchissement automatique de la liste des rôles.
     * Se déclenche toutes les 10 secondes pour détecter les changements
     * effectués depuis un autre poste ou un autre onglet.
     */
    private void demarrerRafraichissementAuto() {
        // Éviter de démarrer plusieurs fois le même service
        if (autoRefreshRoles != null && autoRefreshRoles.isRunning()) {
            autoRefreshRoles.restartNow();
            return;
        }

        autoRefreshRoles = new AutoRefreshService<List<RoleOption>>(PERIODE_REFRESH_ROLES)
                .loader(() -> service.listerRoles(idEcoleCourante))
                .onSuccess(this::appliquerRefreshRoles)
                .onFailure(err -> System.err.println("❌ Auto-refresh rôles : " + err.getMessage()))
                .start();
    }

    /**
     * Applique la liste des rôles rafraîchie en préservant la sélection courante.
     */
//    private void appliquerRefreshRoles(List<RoleOption> nouveauxRoles) {
//        if (nouveauxRoles == null) return;
//
//        String idRoleSelectionneActuel = (roleSelectionne != null) ? roleSelectionne.idRole() : null;
//
//        // Mise à jour de la liste
//        listRoles.setItems(FXCollections.observableArrayList(nouveauxRoles));
//        listRoles.setPlaceholder(new Label(i18n().t("roles.empty")));
//
//        // Restaurer la sélection si le rôle existe toujours
//        if (idRoleSelectionneActuel != null) {
//            nouveauxRoles.stream()
//                    .filter(r -> r.idRole().equals(idRoleSelectionneActuel))
//                    .findFirst()
//                    .ifPresentOrElse(
//                            r -> listRoles.getSelectionModel().select(r),
//                            () -> {
//                                // Le rôle a été supprimé ailleurs : on désélectionne
//                                roleSelectionne = null;
//                                activerPanneauDroit(false);
//                            }
//                    );
//        } else if (!nouveauxRoles.isEmpty()) {
//            listRoles.getSelectionModel().selectFirst();
//        }
//    }

    // APRÈS
    private void appliquerRefreshRoles(List<RoleOption> nouveauxRoles) {
        if (nouveauxRoles == null) return;

        String idRoleSelectionneActuel = (roleSelectionne != null) ? roleSelectionne.idRole() : null;

        // Fusion ciblée : les rôles inchangés (equals() vrai) ne sont pas touchés,
        // donc la ligne sélectionnée n'est ni remplacée ni ré-émise → pas de
        // rechargement intempestif du panneau droit.
        FusionListeUtil.fusionner(listRoles.getItems(), nouveauxRoles, RoleOption::idRole);
        listRoles.setPlaceholder(new Label(i18n().t("roles.empty")));

        if (idRoleSelectionneActuel != null) {
            boolean toujoursPresent = nouveauxRoles.stream()
                    .anyMatch(r -> r.idRole().equals(idRoleSelectionneActuel));

            if (!toujoursPresent) {
                // Le rôle sélectionné a été supprimé ailleurs entre-temps
                roleSelectionne = null;
                activerPanneauDroit(false);
            }
            // sinon : rien à faire, la fusion a préservé la sélection telle quelle
        } else if (!nouveauxRoles.isEmpty() && listRoles.getSelectionModel().isEmpty()) {
            listRoles.getSelectionModel().selectFirst();
        }
    }


    /* ============================================================
       PANNEAU DROIT — Activation/Désactivation
       ============================================================ */
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

    /* ============================================================
       CHARGEMENT DES PERMISSIONS DISPONIBLES (une seule fois)
       ============================================================ */
    private void chargerPermissionsDisponibles() {
        service.listerPermissionsAsync(
                permissions -> Platform.runLater(() -> construireAccordion(permissions)),
                erreur -> Platform.runLater(erreur::printStackTrace));
    }

    /* ============================================================
       CHARGEMENT DES RÔLES
       ============================================================ */
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

    /* ============================================================
       CHARGEMENT DES PERMISSIONS D'UN RÔLE
       ============================================================ */
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

    /* ============================================================
       CONSTRUCTION DE L'ACCORDION DES PERMISSIONS
       ============================================================ */
    private void construireAccordion(List<PermissionOption> permissions) {
        Map<String, List<PermissionOption>> parCategorie = new LinkedHashMap<>();
        for (PermissionOption p : permissions) {
            parCategorie.computeIfAbsent(p.categorie(), k -> new ArrayList<>()).add(p);
        }

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

        if (!accordionModules.getPanes().isEmpty()) {
            accordionModules.setExpandedPane(accordionModules.getPanes().get(0));
        }
        if (roleSelectionne != null) {
            chargerRole(roleSelectionne);
        }
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

    /* ============================================================
       ENREGISTREMENT DES PERMISSIONS
       ============================================================ */
    private void enregistrerPermissions() {
        if (roleSelectionne == null) return;

        Set<String> idsCoches = new HashSet<>();
        casesParIdPermission.forEach((id, c) -> {
            if (c.isSelected()) idsCoches.add(id);
        });

        Stage stage = StageHelper.getStage(btnEnregistrerPermissions);
        btnEnregistrerPermissions.setDisable(true);

        service.enregistrerPermissionsAsync(
                roleSelectionne.idRole(),
                idEcoleCourante,
                idsCoches,
                idUtilisateurConnecte,
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

    /* ============================================================
       CRÉATION D'UN NOUVEAU RÔLE
       ============================================================ */
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
            controller.initCreation(ecoleId, () -> {
                chargerRoles();
                if (autoRefreshRoles != null) autoRefreshRoles.restartNow();
            });
        }
    }

    /* ============================================================
       RENOMMAGE D'UN RÔLE
       ============================================================ */
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
            controller.initEdition(idEcoleCourante, roleSelectionne, () -> {
                chargerRoles();
                if (autoRefreshRoles != null) autoRefreshRoles.restartNow();
            });
        }
    }

    /* ============================================================
       SUPPRESSION D'UN RÔLE
       ============================================================ */
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

                        // Forcer le refresh immédiat pour resynchroniser la liste
                        if (autoRefreshRoles != null) autoRefreshRoles.restartNow();

                        auditService.tracerActionAsync(
                                "HABILITATIONS",
                                "SUPPRESSION_ROLE",
                                "Suppression du rôle : " + nomRoleSupprime
                        );

                        ToastNotification.succes(stage, i18n().t("roles.deleted", nomRoleSupprime));
                    }),
                    erreur -> Platform.runLater(() -> {
                        ToastNotification.erreur(stage, i18n().t("roles.delete_error"));
                        System.err.println("❌ Suppression rôle : " + erreur.getMessage());
                    }));
        }
    }
}