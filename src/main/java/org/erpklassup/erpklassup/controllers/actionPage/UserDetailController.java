package org.erpklassup.erpklassup.controllers.actionPage;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.erpklassup.erpklassup.dao.UtilisateurPermissionDAO;
import org.erpklassup.erpklassup.dto.AuditLigne;
import org.erpklassup.erpklassup.dto.FiltreAudit;
import org.erpklassup.erpklassup.dto.PermissionOption;
import org.erpklassup.erpklassup.dto.RoleOption;
import org.erpklassup.erpklassup.dto.UtilisateurLigne;
import org.erpklassup.erpklassup.models.Permission;
import org.erpklassup.erpklassup.service.AppExecutor;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.RoleService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.UtilisateurService;
import org.erpklassup.erpklassup.util.ToastNotification;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class UserDetailController implements Initializable {

    // ===== HEADER =====
    @FXML private Label avatarLabel;
    @FXML private Label nomCompletLabel;
    @FXML private Label usernameLabel;
    @FXML private Label badgeStatut;
    @FXML private Label badge2FA;

    // ===== TABPANE =====
    @FXML private TabPane tabPane;

    // ===== ONGLET 1 : INFORMATIONS =====
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private ComboBox<String> typeUserCombo;
    @FXML private Button btnAnnulerInfo;
    @FXML private Button btnEnregistrerInfo;

    // ===== ONGLET 2 : RÔLES =====
    @FXML private Label roleCountLabel;
    @FXML private TextField searchRoleField;
    @FXML private FlowPane rolesFlowPane;
    @FXML private Button btnEnregistrerRoles;

    // ===== ONGLET 3 : PERMISSIONS =====
    @FXML private Label permCountLabel;
    @FXML private TextField searchPermissionField;
    @FXML private Accordion accordionPermissions;
    @FXML private Button btnEnregistrerPermissions;

    // ===== ONGLET 4 : SÉCURITÉ =====
    @FXML private ToggleButton toggleActif;
    @FXML private ToggleButton toggle2FA;
    @FXML private Button btnEnvoyerLien;
    @FXML private Button btnGenererTemp;
    @FXML private CheckBox doitChangerMdpCheckBox;
    @FXML private Button btnEnregistrerSecurite;

    // ===== ONGLET 5 : HISTORIQUE =====
    @FXML private TableView<AuditLigne> auditTable;
    @FXML private TableColumn<AuditLigne, String> colDateAudit;
    @FXML private TableColumn<AuditLigne, String> colActionAudit;
    @FXML private TableColumn<AuditLigne, String> colDetailAudit;
    @FXML private TableColumn<AuditLigne, String> colIpAudit;
    @FXML private Pagination paginationAudit;

    // ===== CONSTANTES ET SERVICES =====
    private static final int ELEMENTS_PAR_PAGE = 10;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final UtilisateurService utilisateurService = new UtilisateurService();
    private final RoleService roleService = new RoleService();
    private final AuditService auditService = new AuditService();
    private final UtilisateurPermissionDAO utilisateurPermissionDAO = new UtilisateurPermissionDAO();

    private String idEcoleCourante;
    private UtilisateurLigne utilisateurCourant;
    private Runnable callbackRefresh;

    private final List<CheckBox> listeCheckBoxRoles = new ArrayList<>();
    private final Map<String, CheckBox> casesParIdPermission = new LinkedHashMap<>();
    private List<AuditLigne> listeCompleteAudit = List.of();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();

        if (typeUserCombo != null) {
            typeUserCombo.setItems(FXCollections.observableArrayList(
                    "ADMIN", "PERSONNEL", "ENSEIGNANT", "ÉLÈVE", "PARENT"));
        }

        configurerRechercheFiltres();
        configurerBoutonsEtActions();
        configurerTableAudit();
        configurerPagination();
    }

    public void initData(String idEcole, UtilisateurLigne ligne, Runnable onRefresh) {
        this.idEcoleCourante = idEcole;
        this.utilisateurCourant = ligne;
        this.callbackRefresh = onRefresh;

        if (utilisateurCourant != null) {
            chargerEntete();
            chargerFormulaireInfos();
            chargerRoles();
            chargerPermissionsOrganisees();
            chargerSecurite();
            chargerHistorique();
        }
    }

    // ==========================================
    // 1. EN-TÊTE & PROFIL
    // ==========================================

    @FXML
    private void handleActiver2FA() {
        System.out.println("🔵 [DEBUG] handleActiver2FA appelé");

        if (utilisateurCourant == null) {
            System.out.println("❌ [DEBUG] utilisateurCourant est null");
            ToastNotification.erreur(getStage(), "Aucun utilisateur sélectionné.");
            return;
        }

        String id = utilisateurCourant.idUtilisateur();
        String username = utilisateurCourant.username();
        System.out.println("🔵 [DEBUG] Forçage 2FA pour : " + username + " (id=" + id + ")");

        utilisateurService.forcerActivation2FAAsync(
                id,
                () -> Platform.runLater(() -> {
                    System.out.println("✅ [DEBUG] onSucces appelé — 2FA forcée pour " + username);

                    toggle2FA.setSelected(true);
                    toggle2FA.setText("2FA Requis");
                    mettreAJourBadge2FA(true);   // Met à jour le badge

                    ToastNotification.succes(getStage(),
                            "L'utilisateur devra configurer sa 2FA lors de sa prochaine connexion.");

                    auditService.tracerActionAsync(
                            "SECURITE",
                            "ACTIVATION_OBLIGATOIRE_2FA",
                            "Activation de l'obligation 2FA pour : " + username
                    );
                    chargerHistorique();
                    notifierChangement();
                }),
                erreur -> Platform.runLater(() -> {
                    System.err.println("❌ [DEBUG] onErreur : " + erreur.getMessage());
                    erreur.printStackTrace();
                    ToastNotification.erreur(getStage(),
                            "Erreur lors de la demande d'activation 2FA : " + erreur.getMessage());
                })
        );
    }

    private void chargerEntete() {
        String nom = utilisateurCourant.nom() != null ? utilisateurCourant.nom() : "";
        String prenom = utilisateurCourant.prenom() != null ? utilisateurCourant.prenom() : "";

        nomCompletLabel.setText(utilisateurCourant.nomComplet());
        usernameLabel.setText("@" + utilisateurCourant.username());

        String initiales = (nom.isEmpty() ? "" : nom.substring(0, 1))
                + (prenom.isEmpty() ? "" : prenom.substring(0, 1));
        avatarLabel.setText(initiales.toUpperCase());

        boolean estActif = utilisateurCourant.statut() != null
                && utilisateurCourant.statut().getLibelle().equalsIgnoreCase("Actif");
        mettreAJourBadgeStatut(estActif);
        mettreAJourBadge2FA(utilisateurCourant.deuxFacteursActif());
    }

    private void mettreAJourBadgeStatut(boolean estActif) {
        badgeStatut.setText(estActif ? "● Actif" : "● Inactif");
        badgeStatut.getStyleClass().removeAll("tag-green", "tag-red");
        badgeStatut.getStyleClass().add(estActif ? "tag-green" : "tag-red");
    }

    private void mettreAJourBadge2FA(boolean estActif2FA) {
        badge2FA.setText(estActif2FA ? "● 2FA Activé" : "● 2FA Désactivé");
        badge2FA.getStyleClass().removeAll("tag-blue", "tag-red");
        badge2FA.getStyleClass().add(estActif2FA ? "tag-blue" : "tag-red");
    }

    private void chargerFormulaireInfos() {
        nomField.setText(utilisateurCourant.nom());
        prenomField.setText(utilisateurCourant.prenom());
        usernameField.setText(utilisateurCourant.username());
        emailField.setText(utilisateurCourant.email());
        if (telephoneField != null) {
            telephoneField.setText(utilisateurCourant.telephone());
        }

        if (typeUserCombo != null) {
            if (utilisateurCourant.typeUtilisateur() != null) {
                typeUserCombo.setValue(utilisateurCourant.typeUtilisateur());
            }
            typeUserCombo.setDisable(true);
        }
        usernameField.setDisable(true);
    }

    // ==========================================
    // 2. GESTION DES RÔLES
    // ==========================================
    private void chargerRoles() {
        rolesFlowPane.getChildren().clear();
        listeCheckBoxRoles.clear();

        utilisateurService.chargerRolesAsync(idEcoleCourante, roles -> Platform.runLater(() -> {
            for (RoleOption role : roles) {
                CheckBox cb = new CheckBox(role.nomRole());
                cb.setUserData(role);

                if (utilisateurCourant.roles() != null
                        && utilisateurCourant.roles().contains(role.nomRole())) {
                    cb.setSelected(true);
                }

                cb.selectedProperty().addListener((obs, old, val) -> mettreAJourCompteurRoles());
                listeCheckBoxRoles.add(cb);
                rolesFlowPane.getChildren().add(cb);
            }
            mettreAJourCompteurRoles();
        }), err -> Platform.runLater(() ->
                ToastNotification.erreur(getStage(), "Erreur lors du chargement des rôles.")));
    }

    private void mettreAJourCompteurRoles() {
        long count = listeCheckBoxRoles.stream().filter(CheckBox::isSelected).count();
        roleCountLabel.setText(count + " sélectionné(s)");
    }

    // ==========================================
    // 3. PERMISSIONS PAR MODULE (ACCORDION)
    // ==========================================
    private void chargerPermissionsOrganisees() {
        roleService.listerPermissionsAsync(
                permissions -> Platform.runLater(() -> {
                    construireAccordionPermissions(permissions);
                    chargerPermissionsPossedees();
                }),
                err -> Platform.runLater(() ->
                        ToastNotification.erreur(getStage(), "Erreur lors de la récupération des permissions."))
        );
    }

    private void construireAccordionPermissions(List<PermissionOption> permissions) {
        Map<String, List<PermissionOption>> parCategorie = new LinkedHashMap<>();
        for (PermissionOption p : permissions) {
            parCategorie.computeIfAbsent(p.categorie(), k -> new ArrayList<>()).add(p);
        }

        accordionPermissions.getPanes().clear();
        casesParIdPermission.clear();

        for (var entry : parCategorie.entrySet()) {
            List<CheckBox> casesDuModule = new ArrayList<>();
            VBox corps = new VBox(8);
            corps.setPadding(new Insets(10, 8, 10, 8));
            corps.getStyleClass().add("permissions-module-body");

            for (PermissionOption perm : entry.getValue()) {
                CheckBox cb = new CheckBox(perm.libelle());
                cb.getStyleClass().add("permission-check");
                cb.setUserData(perm.idPermission());
                cb.selectedProperty().addListener((obs, old, val) ->
                        mettreAJourCompteurPermissions());

                casesDuModule.add(cb);
                casesParIdPermission.put(perm.idPermission(), cb);
            }

            CheckBox toutSelectionner = creerCaseToutSelectionner(casesDuModule);
            corps.getChildren().add(toutSelectionner);
            corps.getChildren().addAll(casesDuModule);

            TitledPane pane = new TitledPane(entry.getKey(), corps);
            pane.getStyleClass().add("permission-titled-pane");
            accordionPermissions.getPanes().add(pane);
        }

        if (!accordionPermissions.getPanes().isEmpty()) {
            accordionPermissions.setExpandedPane(accordionPermissions.getPanes().get(0));
        }
    }

    private CheckBox creerCaseToutSelectionner(List<CheckBox> cases) {
        CheckBox toutSelectionner = new CheckBox("Tout sélectionner dans ce module");
        toutSelectionner.getStyleClass().add("check-select-all");

        toutSelectionner.setOnAction(e -> {
            boolean selectionne = toutSelectionner.isSelected();
            cases.forEach(c -> c.setSelected(selectionne));
        });

        cases.forEach(c -> c.selectedProperty().addListener((obs, old, val) -> {
            long cochees = cases.stream().filter(CheckBox::isSelected).count();
            toutSelectionner.setIndeterminate(cochees > 0 && cochees < cases.size());
            toutSelectionner.setSelected(cochees == cases.size());
        }));
        return toutSelectionner;
    }

    private void chargerPermissionsPossedees() {
        if (utilisateurCourant == null) return;

        String idUtilisateur = utilisateurCourant.idUtilisateur();
        System.out.println("🔍 Chargement permissions pour : " + idUtilisateur);

        Task<List<Permission>> task = new Task<>() {
            @Override
            protected List<Permission> call() {
                return utilisateurPermissionDAO.findByUtilisateur(idUtilisateur);
            }
        };

        task.setOnSucceeded(e -> {
            List<Permission> userPermissions = task.getValue();
            System.out.println("✅ " + userPermissions.size() + " permissions trouvées en BDD");

            Set<String> permsUtilisateur = new HashSet<>();
            for (Permission p : userPermissions) {
                if (p.getIdPermission() != null) permsUtilisateur.add(p.getIdPermission());
                if (p.getCodePermission() != null) permsUtilisateur.add(p.getCodePermission());
                System.out.println("   - " + p.getCodePermission() + " (id: " + p.getIdPermission() + ")");
            }

            Platform.runLater(() -> {
                int count = 0;
                for (Map.Entry<String, CheckBox> entry : casesParIdPermission.entrySet()) {
                    boolean possede = permsUtilisateur.contains(entry.getKey());
                    entry.getValue().setSelected(possede);
                    if (possede) count++;
                }
                System.out.println("🎯 " + count + " CheckBox pré-cochées");
                mettreAJourCompteurPermissions();
            });
        });

        task.setOnFailed(e -> {
            System.err.println("❌ Erreur chargement permissions : "
                    + task.getException().getMessage());
            Platform.runLater(() -> ToastNotification.erreur(getStage(),
                    "Erreur lors du chargement des permissions possédées."));
        });

        AppExecutor.get().submit(task);
    }

    private void mettreAJourCompteurPermissions() {
        long count = casesParIdPermission.values().stream().filter(CheckBox::isSelected).count();
        permCountLabel.setText(count + " accordée(s)");
    }

    // ==========================================
    // 4. SÉCURITÉ & TOGGLE BUTTONS
    // ==========================================
    private void chargerSecurite() {
        boolean estActif = utilisateurCourant.statut() != null
                && utilisateurCourant.statut().getLibelle().equalsIgnoreCase("Actif");
        boolean est2FA = utilisateurCourant.deuxFacteursActif();

        toggleActif.setSelected(estActif);
        toggleActif.setText(estActif ? "Actif" : "Inactif");

        toggle2FA.setSelected(est2FA);
        toggle2FA.setText(est2FA ? "2FA Activé" : "2FA Désactivé");

        toggleActif.selectedProperty().addListener((obs, oldVal, isSelected) -> {
            toggleActif.setText(isSelected ? "Actif" : "Inactif");
            mettreAJourBadgeStatut(isSelected);
        });

        toggle2FA.selectedProperty().addListener((obs, oldVal, isSelected) -> {
            toggle2FA.setText(isSelected ? "2FA Activé" : "2FA Désactivé");
            mettreAJourBadge2FA(isSelected);
        });
    }

    // ==========================================
    // 5. AUDIT / HISTORIQUE AVEC PAGINATION
    // ==========================================
    private void configurerTableAudit() {
        colDateAudit.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().horodatage() != null
                        ? data.getValue().horodatage().format(FORMAT_DATE) : ""));
        colActionAudit.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().action()));
        colDetailAudit.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().details()));
        colIpAudit.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().adresseIp() != null
                        ? data.getValue().adresseIp() : "—"));
    }

    private void configurerPagination() {
        if (paginationAudit != null) {
            paginationAudit.currentPageIndexProperty().addListener((obs, old, newIdx) ->
                    afficherPageAudit(newIdx.intValue()));
        }
    }

    private void chargerHistorique() {
        if (utilisateurCourant == null) return;
        auditTable.setPlaceholder(new Label("Chargement de l'historique..."));

        FiltreAudit filtre = new FiltreAudit(idEcoleCourante,
                utilisateurCourant.username(), null, null, null);
        auditService.rechercherAsync(
                filtre,
                resultats -> Platform.runLater(() -> {
                    this.listeCompleteAudit = resultats;
                    int nombrePages = Math.max(1,
                            (int) Math.ceil((double) listeCompleteAudit.size() / ELEMENTS_PAR_PAGE));

                    if (paginationAudit != null) {
                        paginationAudit.setPageCount(nombrePages);
                        paginationAudit.setCurrentPageIndex(0);
                    }
                    afficherPageAudit(0);

                    if (listeCompleteAudit.isEmpty()) {
                        auditTable.setPlaceholder(new Label("Aucun historique trouvé pour cet utilisateur."));
                    }
                }),
                err -> Platform.runLater(() ->
                        auditTable.setPlaceholder(new Label("Erreur lors du chargement de l'historique.")))
        );
    }

    private void afficherPageAudit(int pageIndex) {
        if (listeCompleteAudit.isEmpty()) {
            auditTable.setItems(FXCollections.observableArrayList());
            return;
        }
        int debut = pageIndex * ELEMENTS_PAR_PAGE;
        int fin = Math.min(debut + ELEMENTS_PAR_PAGE, listeCompleteAudit.size());

        if (debut < listeCompleteAudit.size()) {
            auditTable.setItems(FXCollections.observableArrayList(
                    listeCompleteAudit.subList(debut, fin)));
        }
    }

    // ==========================================
    // RECHERCHE & FILTRES
    // ==========================================
    private void configurerRechercheFiltres() {
        searchRoleField.textProperty().addListener((obs, old, n) -> {
            String filtre = (n == null) ? "" : n.toLowerCase().trim();
            rolesFlowPane.getChildren().clear();
            for (CheckBox cb : listeCheckBoxRoles) {
                if (cb.getText().toLowerCase().contains(filtre)) {
                    rolesFlowPane.getChildren().add(cb);
                }
            }
        });

        searchPermissionField.textProperty().addListener((obs, old, n) -> {
            String filtre = (n == null) ? "" : n.toLowerCase().trim();
            casesParIdPermission.forEach((id, cb) -> {
                cb.setVisible(cb.getText().toLowerCase().contains(filtre));
                cb.setManaged(cb.isVisible());
            });
        });
    }

    // ==========================================
    // ACTIONS BOUTONS & AUDIT
    // ==========================================
    private void configurerBoutonsEtActions() {
        btnEnregistrerInfo.setOnAction(e -> enregistrerInformations());
        btnAnnulerInfo.setOnAction(e -> fermerFenetre());
        btnEnregistrerRoles.setOnAction(e -> enregistrerRoles());
        btnEnregistrerPermissions.setOnAction(e -> enregistrerPermissions());
        btnEnregistrerSecurite.setOnAction(e -> enregistrerSecurite());
        btnGenererTemp.setOnAction(e -> genererMotDePasseTemporaire());
        btnEnvoyerLien.setOnAction(e -> envoyerLienReinitialisation());
    }

    private void enregistrerInformations() {
        String username = utilisateurCourant.username();

        String nom    = (nomField != null && nomField.getText() != null) ? nomField.getText().trim() : "";
        String prenom = (prenomField != null && prenomField.getText() != null) ? prenomField.getText().trim() : "";
        String email  = (emailField != null && emailField.getText() != null) ? emailField.getText().trim() : "";
        String tel    = (telephoneField != null && telephoneField.getText() != null) ? telephoneField.getText().trim() : "";

        utilisateurService.modifierUtilisateurAsync(
                utilisateurCourant.idUtilisateur(),
                nom,
                prenom,
                email,
                tel,
                () -> Platform.runLater(() -> {
                    auditService.tracerActionAsync(
                            "UTILISATEUR",
                            "MODIFICATION_INFOS",
                            "Mise à jour des informations personnelles de l'utilisateur : " + username
                    );
                    ToastNotification.succes(getStage(), "Informations modifiées.");
                    chargerHistorique();
                    notifierChangement();
                }),
                err -> Platform.runLater(() ->
                        ToastNotification.erreur(getStage(), err.getMessage()))
        );
    }

    private void enregistrerRoles() {
        List<String> idsRoles = listeCheckBoxRoles.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> ((RoleOption) cb.getUserData()).idRole())
                .collect(Collectors.toList());

        String username = utilisateurCourant.username();

        utilisateurService.mettreAJourRolesAsync(
                utilisateurCourant.idUtilisateur(),
                idEcoleCourante,
                idsRoles,
                () -> Platform.runLater(() -> {
                    auditService.tracerActionAsync(
                            "HABILITATIONS",
                            "AFFECTATION_ROLES",
                            "Attribution de " + idsRoles.size()
                                    + " rôle(s) à l'utilisateur : " + username
                    );
                    ToastNotification.succes(getStage(), "Rôles enregistrés.");
                    chargerHistorique();
                    notifierChangement();
                }),
                err -> Platform.runLater(() ->
                        ToastNotification.erreur(getStage(), "Erreur : " + err.getMessage()))
        );
    }

    private void enregistrerPermissions() {
        Set<String> idsPermissionsSelects = casesParIdPermission.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        String username = utilisateurCourant.username();

        System.out.println("💾 Enregistrement de " + idsPermissionsSelects.size()
                + " permissions pour : " + username);

        utilisateurService.mettreAJourPermissionsAsync(
                utilisateurCourant.idUtilisateur(),
                idsPermissionsSelects,
                () -> Platform.runLater(() -> {
                    auditService.tracerActionAsync(
                            "HABILITATIONS",
                            "MODIFICATION_PERMISSIONS_EXCEPTION",
                            "Mise à jour des permissions d'exception ("
                                    + idsPermissionsSelects.size()
                                    + " octroyée(s)) pour : " + username
                    );
                    ToastNotification.succes(getStage(),
                            "Permissions d'exception enregistrées ("
                                    + idsPermissionsSelects.size() + ").");
                    chargerPermissionsPossedees();
                    chargerHistorique();
                    notifierChangement();
                }),
                (Throwable err) -> Platform.runLater(() ->
                        ToastNotification.erreur(getStage(),
                                "Erreur lors de l'enregistrement : " + err.getMessage()))
        );
    }

    /**
     * ✅ CORRIGÉ : cette méthode ne touche PAS au flag deuxFacteursActive.
     * Elle met à jour le statut actif/inactif, le flag doitChangerMotDePasse,
     * et le téléphone. Le forçage de la 2FA est géré par handleActiver2FA().
     */
    private void enregistrerSecurite() {
        boolean nouveauStatut = toggleActif.isSelected();
        boolean doitChangerMdp = doitChangerMdpCheckBox != null
                && doitChangerMdpCheckBox.isSelected();
        String telephone = (telephoneField != null && telephoneField.getText() != null)
                ? telephoneField.getText().trim()
                : "";
        String username = utilisateurCourant.username();

        // 1. Changement de statut (actif / inactif)
        utilisateurService.changerStatutActifAsync(
                utilisateurCourant.idUtilisateur(),
                nouveauStatut,
                () -> Platform.runLater(() -> {
                    auditService.tracerActionAsync(
                            "SECURITE",
                            "CHANGEMENT_STATUT_COMPTE",
                            "Compte de " + username + " passé à : "
                                    + (nouveauStatut ? "ACTIF" : "INACTIF")
                    );
                    ToastNotification.succes(getStage(), "Statut de sécurité mis à jour.");
                    chargerHistorique();
                    notifierChangement();
                }),
                err -> Platform.runLater(() ->
                        ToastNotification.erreur(getStage(),
                                "Erreur de modification du statut."))
        );

        // 2. ✅ Mise à jour sécurité : on passe la valeur ACTUELLE de deuxFacteursActif()
        //    (jamais celle du toggle) pour ne pas activer accidentellement la 2FA
        utilisateurService.mettreAJourSecuriteAsync(
                utilisateurCourant.idUtilisateur(),
                utilisateurCourant.deuxFacteursActif(),
                doitChangerMdp,
                telephone,
                () -> Platform.runLater(() -> {
                    auditService.tracerActionAsync(
                            "SECURITE",
                            "MODIFICATION_PARAMETRES_SECURITE",
                            "Mise à jour sécurité de : " + username
                    );
                    ToastNotification.succes(getStage(),
                            "Sécurité et téléphone mis à jour avec succès.");
                    chargerHistorique();
                    notifierChangement();
                }),
                err -> Platform.runLater(() ->
                        ToastNotification.erreur(getStage(), err.getMessage()))
        );
    }

    private void genererMotDePasseTemporaire() {
        String tempMdp = "Temp@" + (int)(Math.random() * 9000 + 1000);
        String username = utilisateurCourant.username();

        utilisateurService.reinitialiserMotDePasseAsync(
                utilisateurCourant.idUtilisateur(),
                tempMdp,
                () -> Platform.runLater(() -> {
                    auditService.tracerActionAsync(
                            "SECURITE",
                            "REINITIALISATION_MDP",
                            "Génération d'un mot de passe temporaire pour : " + username
                    );
                    ToastNotification.info(getStage(), "Mot de passe temporaire : " + tempMdp);
                    chargerHistorique();
                }),
                err -> Platform.runLater(() -> ToastNotification.erreur(getStage(), "Erreur."))
        );
    }

    private void envoyerLienReinitialisation() {
        ToastNotification.info(getStage(),
                "Lien de réinitialisation envoyé à : " + emailField.getText());
    }

    private void notifierChangement() {
        if (callbackRefresh != null) callbackRefresh.run();
    }

    private void fermerFenetre() {
        Stage stage = getStage();
        if (stage != null) stage.close();
    }

    private Stage getStage() {
        return (Stage) tabPane.getScene().getWindow();
    }
}
//package org.erpklassup.erpklassup.controllers.actionPage;
//
//import javafx.application.Platform;
//import javafx.beans.property.SimpleStringProperty;
//import javafx.collections.FXCollections;
//import javafx.concurrent.Task;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.geometry.Insets;
//import javafx.scene.control.*;
//import javafx.scene.layout.FlowPane;
//import javafx.scene.layout.VBox;
//import javafx.stage.Stage;
//
//import org.erpklassup.erpklassup.dao.UtilisateurPermissionDAO;
//import org.erpklassup.erpklassup.dto.AuditLigne;
//import org.erpklassup.erpklassup.dto.FiltreAudit;
//import org.erpklassup.erpklassup.dto.PermissionOption;
//import org.erpklassup.erpklassup.dto.RoleOption;
//import org.erpklassup.erpklassup.dto.UtilisateurLigne;
//import org.erpklassup.erpklassup.models.Permission;
//import org.erpklassup.erpklassup.service.AppExecutor;
//import org.erpklassup.erpklassup.service.AuditService;
//import org.erpklassup.erpklassup.service.RoleService;
//import org.erpklassup.erpklassup.service.SessionManager;
//import org.erpklassup.erpklassup.service.UtilisateurService;
//import org.erpklassup.erpklassup.util.ToastNotification;
//
//import java.net.URL;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@SuppressWarnings({"unused", "SpellCheckingInspection"})
//public class UserDetailController implements Initializable {
//
//    // ===== HEADER =====
//    @FXML private Label avatarLabel;
//    @FXML private Label nomCompletLabel;
//    @FXML private Label usernameLabel;
//    @FXML private Label badgeStatut;
//    @FXML private Label badge2FA;
//
//    // ===== TABPANE =====
//    @FXML private TabPane tabPane;
//
//    // ===== ONGLET 1 : INFORMATIONS =====
//    @FXML private TextField nomField;
//    @FXML private TextField prenomField;
//    @FXML private TextField usernameField;
//    @FXML private TextField emailField;
//    @FXML private TextField telephoneField;
//    @FXML private ComboBox<String> typeUserCombo;
//    @FXML private Button btnAnnulerInfo;
//    @FXML private Button btnEnregistrerInfo;
//
//    // ===== ONGLET 2 : RÔLES =====
//    @FXML private Label roleCountLabel;
//    @FXML private TextField searchRoleField;
//    @FXML private FlowPane rolesFlowPane;
//    @FXML private Button btnEnregistrerRoles;
//
//    // ===== ONGLET 3 : PERMISSIONS =====
//    @FXML private Label permCountLabel;
//    @FXML private TextField searchPermissionField;
//    @FXML private Accordion accordionPermissions;
//    @FXML private Button btnEnregistrerPermissions;
//
//    // ===== ONGLET 4 : SÉCURITÉ =====
//    @FXML private ToggleButton toggleActif;
//    @FXML private ToggleButton toggle2FA;
//    @FXML private Button btnEnvoyerLien;
//    @FXML private Button btnGenererTemp;
//    @FXML private CheckBox doitChangerMdpCheckBox;
//    @FXML private Button btnEnregistrerSecurite;
//
//    // ===== ONGLET 5 : HISTORIQUE =====
//    @FXML private TableView<AuditLigne> auditTable;
//    @FXML private TableColumn<AuditLigne, String> colDateAudit;
//    @FXML private TableColumn<AuditLigne, String> colActionAudit;
//    @FXML private TableColumn<AuditLigne, String> colDetailAudit;
//    @FXML private TableColumn<AuditLigne, String> colIpAudit;
//    @FXML private Pagination paginationAudit;
//
//    // ===== CONSTANTES ET SERVICES =====
//    private static final int ELEMENTS_PAR_PAGE = 10;
//    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
//
//    private final UtilisateurService utilisateurService = new UtilisateurService();
//    private final RoleService roleService = new RoleService();
//    private final AuditService auditService = new AuditService();
//    private final UtilisateurPermissionDAO utilisateurPermissionDAO = new UtilisateurPermissionDAO();
//
//    private String idEcoleCourante;
//    private UtilisateurLigne utilisateurCourant;
//    private Runnable callbackRefresh;
//
//    private final List<CheckBox> listeCheckBoxRoles = new ArrayList<>();
//    private final Map<String, CheckBox> casesParIdPermission = new LinkedHashMap<>();
//    private List<AuditLigne> listeCompleteAudit = List.of();
//
//    @Override
//    public void initialize(URL url, ResourceBundle rb) {
//        this.idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();
//
//        if (typeUserCombo != null) {
//            typeUserCombo.setItems(FXCollections.observableArrayList(
//                    "ADMIN", "PERSONNEL", "ENSEIGNANT", "ÉLÈVE", "PARENT"));
//        }
//
//        configurerRechercheFiltres();
//        configurerBoutonsEtActions();
//        configurerTableAudit();
//        configurerPagination();
//    }
//
//    public void initData(String idEcole, UtilisateurLigne ligne, Runnable onRefresh) {
//        this.idEcoleCourante = idEcole;
//        this.utilisateurCourant = ligne;
//        this.callbackRefresh = onRefresh;
//
//        if (utilisateurCourant != null) {
//            chargerEntete();
//            chargerFormulaireInfos();
//            chargerRoles();
//            chargerPermissionsOrganisees();
//            chargerSecurite();
//            chargerHistorique();
//        }
//    }
//
//    // ==========================================
//    // 1. EN-TÊTE & PROFIL
//    // ==========================================
//
//    @FXML
//    private void handleActiver2FA() {
//        if (utilisateurCourant == null) return;
//
//        utilisateurService.forcerActivation2FAAsync(
//                utilisateurCourant.idUtilisateur(),
//                () -> Platform.runLater(() -> {
//                    toggle2FA.setSelected(true);
//                    toggle2FA.setText("2FA Requis");
//
//                    ToastNotification.succes(
//                            getStage(),
//                            "L'utilisateur devra configurer sa 2FA lors de sa prochaine connexion."
//                    );
//
//                    auditService.tracerActionAsync(
//                            "SECURITE",
//                            "ACTIVATION_OBLIGATOIRE_2FA",
//                            "Activation de l'obligation 2FA pour : " + utilisateurCourant.username()
//                    );
//                    chargerHistorique();
//                    notifierChangement();
//                }),
//                erreur -> Platform.runLater(() ->
//                        ToastNotification.erreur(getStage(),
//                                "Erreur lors de la demande d'activation 2FA : " + erreur.getMessage())
//                )
//        );
//    }
//
//    private void chargerEntete() {
//        String nom = utilisateurCourant.nom() != null ? utilisateurCourant.nom() : "";
//        String prenom = utilisateurCourant.prenom() != null ? utilisateurCourant.prenom() : "";
//
//        nomCompletLabel.setText(utilisateurCourant.nomComplet());
//        usernameLabel.setText("@" + utilisateurCourant.username());
//
//        String initiales = (nom.isEmpty() ? "" : nom.substring(0, 1))
//                + (prenom.isEmpty() ? "" : prenom.substring(0, 1));
//        avatarLabel.setText(initiales.toUpperCase());
//
//        boolean estActif = utilisateurCourant.statut() != null
//                && utilisateurCourant.statut().getLibelle().equalsIgnoreCase("Actif");
//        mettreAJourBadgeStatut(estActif);
//        mettreAJourBadge2FA(utilisateurCourant.deuxFacteursActif());
//    }
//
//    private void mettreAJourBadgeStatut(boolean estActif) {
//        badgeStatut.setText(estActif ? "● Actif" : "● Inactif");
//        badgeStatut.getStyleClass().removeAll("tag-green", "tag-red");
//        badgeStatut.getStyleClass().add(estActif ? "tag-green" : "tag-red");
//    }
//
//    private void mettreAJourBadge2FA(boolean estActif2FA) {
//        badge2FA.setText(estActif2FA ? "● 2FA Activé" : "● 2FA Désactivé");
//        badge2FA.getStyleClass().removeAll("tag-blue", "tag-red");
//        badge2FA.getStyleClass().add(estActif2FA ? "tag-blue" : "tag-red");
//    }
//
//    private void chargerFormulaireInfos() {
//        nomField.setText(utilisateurCourant.nom());
//        prenomField.setText(utilisateurCourant.prenom());
//        usernameField.setText(utilisateurCourant.username());
//        emailField.setText(utilisateurCourant.email());
//        if (telephoneField != null) {
//            telephoneField.setText(utilisateurCourant.telephone());
//        }
//
//        if (typeUserCombo != null) {
//            if (utilisateurCourant.typeUtilisateur() != null) {
//                typeUserCombo.setValue(utilisateurCourant.typeUtilisateur());
//            }
//            typeUserCombo.setDisable(true);
//        }
//        usernameField.setDisable(true);
//    }
//
//    // ==========================================
//    // 2. GESTION DES RÔLES
//    // ==========================================
//    private void chargerRoles() {
//        rolesFlowPane.getChildren().clear();
//        listeCheckBoxRoles.clear();
//
//        utilisateurService.chargerRolesAsync(idEcoleCourante, roles -> Platform.runLater(() -> {
//            for (RoleOption role : roles) {
//                CheckBox cb = new CheckBox(role.nomRole());
//                cb.setUserData(role);
//
//                if (utilisateurCourant.roles() != null
//                        && utilisateurCourant.roles().contains(role.nomRole())) {
//                    cb.setSelected(true);
//                }
//
//                cb.selectedProperty().addListener((obs, old, val) -> mettreAJourCompteurRoles());
//                listeCheckBoxRoles.add(cb);
//                rolesFlowPane.getChildren().add(cb);
//            }
//            mettreAJourCompteurRoles();
//        }), err -> Platform.runLater(() ->
//                ToastNotification.erreur(getStage(), "Erreur lors du chargement des rôles.")));
//    }
//
//    private void mettreAJourCompteurRoles() {
//        long count = listeCheckBoxRoles.stream().filter(CheckBox::isSelected).count();
//        roleCountLabel.setText(count + " sélectionné(s)");
//    }
//
//    // ==========================================
//    // 3. PERMISSIONS PAR MODULE (ACCORDION)
//    // ==========================================
//    private void chargerPermissionsOrganisees() {
//        roleService.listerPermissionsAsync(
//                permissions -> Platform.runLater(() -> {
//                    construireAccordionPermissions(permissions);
//                    // ✅ Chargement asynchrone des permissions possédées
//                    chargerPermissionsPossedees();
//                }),
//                err -> Platform.runLater(() ->
//                        ToastNotification.erreur(getStage(), "Erreur lors de la récupération des permissions."))
//        );
//    }
//
//    private void construireAccordionPermissions(List<PermissionOption> permissions) {
//        Map<String, List<PermissionOption>> parCategorie = new LinkedHashMap<>();
//        for (PermissionOption p : permissions) {
//            parCategorie.computeIfAbsent(p.categorie(), k -> new ArrayList<>()).add(p);
//        }
//
//        accordionPermissions.getPanes().clear();
//        casesParIdPermission.clear();
//
//        for (var entry : parCategorie.entrySet()) {
//            List<CheckBox> casesDuModule = new ArrayList<>();
//            VBox corps = new VBox(8);
//            corps.setPadding(new Insets(10, 8, 10, 8));
//            corps.getStyleClass().add("permissions-module-body");
//
//            for (PermissionOption perm : entry.getValue()) {
//                CheckBox cb = new CheckBox(perm.libelle());
//                cb.getStyleClass().add("permission-check");
//                cb.setUserData(perm.idPermission());
//                cb.selectedProperty().addListener((obs, old, val) ->
//                        mettreAJourCompteurPermissions());
//
//                casesDuModule.add(cb);
//                // ✅ On utilise l'ID comme clé
//                casesParIdPermission.put(perm.idPermission(), cb);
//            }
//
//            CheckBox toutSelectionner = creerCaseToutSelectionner(casesDuModule);
//            corps.getChildren().add(toutSelectionner);
//            corps.getChildren().addAll(casesDuModule);
//
//            TitledPane pane = new TitledPane(entry.getKey(), corps);
//            pane.getStyleClass().add("permission-titled-pane");
//            accordionPermissions.getPanes().add(pane);
//        }
//
//        if (!accordionPermissions.getPanes().isEmpty()) {
//            accordionPermissions.setExpandedPane(accordionPermissions.getPanes().get(0));
//        }
//    }
//
//    private CheckBox creerCaseToutSelectionner(List<CheckBox> cases) {
//        CheckBox toutSelectionner = new CheckBox("Tout sélectionner dans ce module");
//        toutSelectionner.getStyleClass().add("check-select-all");
//
//        toutSelectionner.setOnAction(e -> {
//            boolean selectionne = toutSelectionner.isSelected();
//            cases.forEach(c -> c.setSelected(selectionne));
//        });
//
//        cases.forEach(c -> c.selectedProperty().addListener((obs, old, val) -> {
//            long cochees = cases.stream().filter(CheckBox::isSelected).count();
//            toutSelectionner.setIndeterminate(cochees > 0 && cochees < cases.size());
//            toutSelectionner.setSelected(cochees == cases.size());
//        }));
//        return toutSelectionner;
//    }
//
//    /**
//     * ✅ CORRECTION BUG 1 : Chargement asynchrone des permissions possédées
//     * Ne bloque plus le thread JavaFX et pré-coche correctement
//     */
//    private void chargerPermissionsPossedees() {
//        if (utilisateurCourant == null) return;
//
//        String idUtilisateur = utilisateurCourant.idUtilisateur();
//        System.out.println("🔍 Chargement permissions pour : " + idUtilisateur);
//
//        Task<List<Permission>> task = new Task<>() {
//            @Override
//            protected List<Permission> call() {
//                // Requête BDD dans un thread séparé (ne bloque pas l'UI)
//                return utilisateurPermissionDAO.findByUtilisateur(idUtilisateur);
//            }
//        };
//
//        task.setOnSucceeded(e -> {
//            List<Permission> userPermissions = task.getValue();
//            System.out.println("✅ " + userPermissions.size() + " permissions trouvées en BDD");
//
//            // Construire un Set des IDs et codes de permissions possédées
//            Set<String> permsUtilisateur = new HashSet<>();
//            for (Permission p : userPermissions) {
//                if (p.getIdPermission() != null) permsUtilisateur.add(p.getIdPermission());
//                if (p.getCodePermission() != null) permsUtilisateur.add(p.getCodePermission());
//                System.out.println("   - " + p.getCodePermission() + " (id: " + p.getIdPermission() + ")");
//            }
//
//            Platform.runLater(() -> {
//                // ✅ Pré-cocher les CheckBox correspondantes
//                int count = 0;
//                for (Map.Entry<String, CheckBox> entry : casesParIdPermission.entrySet()) {
//                    boolean possede = permsUtilisateur.contains(entry.getKey());
//                    entry.getValue().setSelected(possede);
//                    if (possede) count++;
//                }
//                System.out.println("🎯 " + count + " CheckBox pré-cochées");
//                mettreAJourCompteurPermissions();
//            });
//        });
//
//        task.setOnFailed(e -> {
//            System.err.println("❌ Erreur chargement permissions : "
//                    + task.getException().getMessage());
//            Platform.runLater(() -> ToastNotification.erreur(getStage(),
//                    "Erreur lors du chargement des permissions possédées."));
//        });
//
//        AppExecutor.get().submit(task);
//    }
//
//    private void mettreAJourCompteurPermissions() {
//        long count = casesParIdPermission.values().stream().filter(CheckBox::isSelected).count();
//        permCountLabel.setText(count + " accordée(s)");
//    }
//
//    // ==========================================
//    // 4. SÉCURITÉ & TOGGLE BUTTONS
//    // ==========================================
//    private void chargerSecurite() {
//        boolean estActif = utilisateurCourant.statut() != null
//                && utilisateurCourant.statut().getLibelle().equalsIgnoreCase("Actif");
//        boolean est2FA = utilisateurCourant.deuxFacteursActif();
//
//        toggleActif.setSelected(estActif);
//        toggleActif.setText(estActif ? "Actif" : "Inactif");
//
//        toggle2FA.setSelected(est2FA);
//        toggle2FA.setText(est2FA ? "2FA Activé" : "2FA Désactivé");
//
//        toggleActif.selectedProperty().addListener((obs, oldVal, isSelected) -> {
//            toggleActif.setText(isSelected ? "Actif" : "Inactif");
//            mettreAJourBadgeStatut(isSelected);
//        });
//
//        toggle2FA.selectedProperty().addListener((obs, oldVal, isSelected) -> {
//            toggle2FA.setText(isSelected ? "2FA Activé" : "2FA Désactivé");
//            mettreAJourBadge2FA(isSelected);
//        });
//    }
//
//    // ==========================================
//    // 5. AUDIT / HISTORIQUE AVEC PAGINATION
//    // ==========================================
//    private void configurerTableAudit() {
//        colDateAudit.setCellValueFactory(data ->
//                new SimpleStringProperty(data.getValue().horodatage() != null
//                        ? data.getValue().horodatage().format(FORMAT_DATE) : ""));
//        colActionAudit.setCellValueFactory(data ->
//                new SimpleStringProperty(data.getValue().action()));
//        colDetailAudit.setCellValueFactory(data ->
//                new SimpleStringProperty(data.getValue().details()));
//        colIpAudit.setCellValueFactory(data ->
//                new SimpleStringProperty(data.getValue().adresseIp() != null
//                        ? data.getValue().adresseIp() : "—"));
//    }
//
//    private void configurerPagination() {
//        if (paginationAudit != null) {
//            paginationAudit.currentPageIndexProperty().addListener((obs, old, newIdx) ->
//                    afficherPageAudit(newIdx.intValue()));
//        }
//    }
//
//    private void chargerHistorique() {
//        if (utilisateurCourant == null) return;
//        auditTable.setPlaceholder(new Label("Chargement de l'historique..."));
//
//        FiltreAudit filtre = new FiltreAudit(idEcoleCourante,
//                utilisateurCourant.username(), null, null, null);
//        auditService.rechercherAsync(
//                filtre,
//                resultats -> Platform.runLater(() -> {
//                    this.listeCompleteAudit = resultats;
//                    int nombrePages = Math.max(1,
//                            (int) Math.ceil((double) listeCompleteAudit.size() / ELEMENTS_PAR_PAGE));
//
//                    if (paginationAudit != null) {
//                        paginationAudit.setPageCount(nombrePages);
//                        paginationAudit.setCurrentPageIndex(0);
//                    }
//                    afficherPageAudit(0);
//
//                    if (listeCompleteAudit.isEmpty()) {
//                        auditTable.setPlaceholder(new Label("Aucun historique trouvé pour cet utilisateur."));
//                    }
//                }),
//                err -> Platform.runLater(() ->
//                        auditTable.setPlaceholder(new Label("Erreur lors du chargement de l'historique.")))
//        );
//    }
//
//    private void afficherPageAudit(int pageIndex) {
//        if (listeCompleteAudit.isEmpty()) {
//            auditTable.setItems(FXCollections.observableArrayList());
//            return;
//        }
//        int debut = pageIndex * ELEMENTS_PAR_PAGE;
//        int fin = Math.min(debut + ELEMENTS_PAR_PAGE, listeCompleteAudit.size());
//
//        if (debut < listeCompleteAudit.size()) {
//            auditTable.setItems(FXCollections.observableArrayList(
//                    listeCompleteAudit.subList(debut, fin)));
//        }
//    }
//
//    // ==========================================
//    // RECHERCHE & FILTRES
//    // ==========================================
//    private void configurerRechercheFiltres() {
//        searchRoleField.textProperty().addListener((obs, old, n) -> {
//            String filtre = (n == null) ? "" : n.toLowerCase().trim();
//            rolesFlowPane.getChildren().clear();
//            for (CheckBox cb : listeCheckBoxRoles) {
//                if (cb.getText().toLowerCase().contains(filtre)) {
//                    rolesFlowPane.getChildren().add(cb);
//                }
//            }
//        });
//
//        searchPermissionField.textProperty().addListener((obs, old, n) -> {
//            String filtre = (n == null) ? "" : n.toLowerCase().trim();
//            casesParIdPermission.forEach((id, cb) -> {
//                cb.setVisible(cb.getText().toLowerCase().contains(filtre));
//                cb.setManaged(cb.isVisible());
//            });
//        });
//    }
//
//    // ==========================================
//    // ACTIONS BOUTONS & AUDIT
//    // ==========================================
//    private void configurerBoutonsEtActions() {
//        btnEnregistrerInfo.setOnAction(e -> enregistrerInformations());
//        btnAnnulerInfo.setOnAction(e -> fermerFenetre());
//        btnEnregistrerRoles.setOnAction(e -> enregistrerRoles());
//        btnEnregistrerPermissions.setOnAction(e -> enregistrerPermissions());
//        btnEnregistrerSecurite.setOnAction(e -> enregistrerSecurite());
//        btnGenererTemp.setOnAction(e -> genererMotDePasseTemporaire());
//        btnEnvoyerLien.setOnAction(e -> envoyerLienReinitialisation());
//    }
//
//
//    private void enregistrerInformations() {
//        String username = utilisateurCourant.username();
//
//        // ✅ Déclarations AVANT l'appel, avec protection null
//        String nom    = (nomField != null && nomField.getText() != null) ? nomField.getText().trim() : "";
//        String prenom = (prenomField != null && prenomField.getText() != null) ? prenomField.getText().trim() : "";
//        String email  = (emailField != null && emailField.getText() != null) ? emailField.getText().trim() : "";
//        String tel    = (telephoneField != null && telephoneField.getText() != null) ? telephoneField.getText().trim() : "";
//
//        utilisateurService.modifierUtilisateurAsync(
//                utilisateurCourant.idUtilisateur(),
//                nom,
//                prenom,
//                email,
//                tel,
//                () -> Platform.runLater(() -> {
//                    auditService.tracerActionAsync(
//                            "UTILISATEUR",
//                            "MODIFICATION_INFOS",
//                            "Mise à jour des informations personnelles de l'utilisateur : " + username
//                    );
//                    ToastNotification.succes(getStage(), "Informations modifiées.");
//                    chargerHistorique();
//                    notifierChangement();
//                }),
//                err -> Platform.runLater(() ->
//                        ToastNotification.erreur(getStage(), err.getMessage()))
//        );
//    }
//
//
//
//
//
//
//
//    private void enregistrerRoles() {
//        List<String> idsRoles = listeCheckBoxRoles.stream()
//                .filter(CheckBox::isSelected)
//                .map(cb -> ((RoleOption) cb.getUserData()).idRole())
//                .collect(Collectors.toList());
//
//        String username = utilisateurCourant.username();
//
//        utilisateurService.mettreAJourRolesAsync(
//                utilisateurCourant.idUtilisateur(),
//                idEcoleCourante,
//                idsRoles,
//                () -> Platform.runLater(() -> {
//                    auditService.tracerActionAsync(
//                            "HABILITATIONS",
//                            "AFFECTATION_ROLES",
//                            "Attribution de " + idsRoles.size()
//                                    + " rôle(s) à l'utilisateur : " + username
//                    );
//                    ToastNotification.succes(getStage(), "Rôles enregistrés.");
//                    chargerHistorique();
//                    notifierChangement();
//                }),
//                err -> Platform.runLater(() ->
//                        ToastNotification.erreur(getStage(), "Erreur : " + err.getMessage()))
//        );
//    }
//
//    /**
//     * ✅ CORRECTION BUG 2 : Enregistrement RÉEL des permissions en BDD
//     */
//    private void enregistrerPermissions() {
//        Set<String> idsPermissionsSelects = casesParIdPermission.entrySet().stream()
//                .filter(entry -> entry.getValue().isSelected())
//                .map(Map.Entry::getKey)
//                .collect(Collectors.toSet());
//
//        String username = utilisateurCourant.username();
//
//        System.out.println("💾 Enregistrement de " + idsPermissionsSelects.size()
//                + " permissions pour : " + username);
//
//        utilisateurService.mettreAJourPermissionsAsync(
//                utilisateurCourant.idUtilisateur(),
//                idsPermissionsSelects,
//                () -> Platform.runLater(() -> {
//                    auditService.tracerActionAsync(
//                            "HABILITATIONS",
//                            "MODIFICATION_PERMISSIONS_EXCEPTION",
//                            "Mise à jour des permissions d'exception ("
//                                    + idsPermissionsSelects.size()
//                                    + " octroyée(s)) pour : " + username
//                    );
//                    ToastNotification.succes(getStage(),
//                            "Permissions d'exception enregistrées ("
//                                    + idsPermissionsSelects.size() + ").");
//                    chargerPermissionsPossedees();
//                    chargerHistorique();
//                    notifierChangement();
//                }),
//                (Throwable err) -> Platform.runLater(() ->      // ✅ Type explicite
//                        ToastNotification.erreur(getStage(),
//                                "Erreur lors de l'enregistrement : " + err.getMessage()))
//        );
//    }
//
////    private void enregistrerSecurite() {
////        boolean nouveauStatut = toggleActif.isSelected();
////        String username = utilisateurCourant.username();
////        boolean doitConfigurer2FA = toggle2FA.isSelected();
////        boolean doitChangerMdp = doitChangerMdpCheckBox != null
////                && doitChangerMdpCheckBox.isSelected();
////        String telephone = (telephoneField != null && telephoneField.getText() != null)
////                ? telephoneField.getText().trim()
////                : "";
////        utilisateurService.changerStatutActifAsync(
////                utilisateurCourant.idUtilisateur(),
////                nouveauStatut,
////                () -> Platform.runLater(() -> {
////                    auditService.tracerActionAsync(
////                            "SECURITE",
////                            "CHANGEMENT_STATUT_COMPTE",
////                            "Compte de " + username + " passé à : "
////                                    + (nouveauStatut ? "ACTIF" : "INACTIF")
////                    );
////                    ToastNotification.succes(getStage(), "Statut de sécurité mis à jour.");
////                    chargerHistorique();
////                    notifierChangement();
////                }),
////                err -> Platform.runLater(() ->
////                        ToastNotification.erreur(getStage(),
////                                "Erreur de modification du statut."))
////        );
////
////        utilisateurService.mettreAJourSecuriteAsync(
////                utilisateurCourant.idUtilisateur(),
////                doitConfigurer2FA,
////                doitChangerMdp,
////                telephone,
////                () -> Platform.runLater(() -> {
////                    auditService.tracerActionAsync(
////                            "SECURITE",
////                            "MODIFICATION_PARAMETRES_SECURITE",
////                            "Mise à jour sécurité de : " + username
////                    );
////                    ToastNotification.succes(getStage(),
////                            "Sécurité et téléphone mis à jour avec succès.");
////                    chargerHistorique();
////                    notifierChangement();
////                }),
////                err -> Platform.runLater(() ->
////                        ToastNotification.erreur(getStage(), err.getMessage()))
////        );
////    }
//private void enregistrerSecurite() {
//    boolean nouveauStatut = toggleActif.isSelected();
//    boolean doitChangerMdp = doitChangerMdpCheckBox != null
//            && doitChangerMdpCheckBox.isSelected();
//    String telephone = (telephoneField != null && telephoneField.getText() != null)
//            ? telephoneField.getText().trim()
//            : "";
//    String username = utilisateurCourant.username();
//
//    // 1. Changement de statut (actif / inactif)
//    utilisateurService.changerStatutActifAsync(
//            utilisateurCourant.idUtilisateur(),
//            nouveauStatut,
//            () -> Platform.runLater(() -> {
//                auditService.tracerActionAsync("SECURITE", "CHANGEMENT_STATUT_COMPTE",
//                        "Compte de " + username + " passé à : "
//                                + (nouveauStatut ? "ACTIF" : "INACTIF"));
//                ToastNotification.succes(getStage(), "Statut mis à jour.");
//                chargerHistorique();
//                notifierChangement();
//            }),
//            err -> Platform.runLater(() ->
//                    ToastNotification.erreur(getStage(), "Erreur statut."))
//    );
//
//    // 2. ✅ Mise à jour SEULEMENT du flag "doitChangerMotDePasse" et du téléphone
//    //    (PAS de deuxFacteursActive ici !)
//    utilisateurService.mettreAJourSecuriteAsync(
//            utilisateurCourant.idUtilisateur(),
//            utilisateurCourant.deuxFacteursActif(),   // ← on garde la valeur actuelle
//            doitChangerMdp,
//            telephone,
//            () -> Platform.runLater(() -> {
//                ToastNotification.succes(getStage(), "Paramètres mis à jour.");
//                chargerHistorique();
//                notifierChangement();
//            }),
//            err -> Platform.runLater(() ->
//                    ToastNotification.erreur(getStage(), err.getMessage()))
//    );
//}
//
//    private void genererMotDePasseTemporaire() {
//        String tempMdp = "Temp@" + (int)(Math.random() * 9000 + 1000);
//        String username = utilisateurCourant.username();
//
//        utilisateurService.reinitialiserMotDePasseAsync(
//                utilisateurCourant.idUtilisateur(),
//                tempMdp,
//                () -> Platform.runLater(() -> {
//                    auditService.tracerActionAsync(
//                            "SECURITE",
//                            "REINITIALISATION_MDP",
//                            "Génération d'un mot de passe temporaire pour : " + username
//                    );
//                    ToastNotification.info(getStage(), "Mot de passe temporaire : " + tempMdp);
//                    chargerHistorique();
//                }),
//                err -> Platform.runLater(() -> ToastNotification.erreur(getStage(), "Erreur."))
//        );
//    }
//
//    private void envoyerLienReinitialisation() {
//        ToastNotification.info(getStage(),
//                "Lien de réinitialisation envoyé à : " + emailField.getText());
//    }
//
//    private void notifierChangement() {
//        if (callbackRefresh != null) callbackRefresh.run();
//    }
//
//    private void fermerFenetre() {
//        Stage stage = getStage();
//        if (stage != null) stage.close();
//    }
//
//    private Stage getStage() {
//        return (Stage) tabPane.getScene().getWindow();
//    }
//}
////package org.erpklassup.erpklassup.controllers.actionPage;
////
////import javafx.application.Platform;
////import javafx.beans.property.SimpleStringProperty;
////import javafx.collections.FXCollections;
////import javafx.fxml.FXML;
////import javafx.fxml.Initializable;
////import javafx.geometry.Insets;
////import javafx.scene.control.*;
////import javafx.scene.layout.FlowPane;
////import javafx.scene.layout.VBox;
////import javafx.stage.Stage;
////
////import org.erpklassup.erpklassup.dao.UtilisateurPermissionDAO;
////import org.erpklassup.erpklassup.dto.AuditLigne;
////import org.erpklassup.erpklassup.dto.FiltreAudit;
////import org.erpklassup.erpklassup.dto.PermissionOption;
////import org.erpklassup.erpklassup.dto.RoleOption;
////import org.erpklassup.erpklassup.dto.UtilisateurLigne;
////import org.erpklassup.erpklassup.models.Permission;
////import org.erpklassup.erpklassup.service.AuditService;
////import org.erpklassup.erpklassup.service.RoleService;
////import org.erpklassup.erpklassup.service.SessionManager;
////import org.erpklassup.erpklassup.service.UtilisateurService;
////import org.erpklassup.erpklassup.util.ToastNotification;
////import org.erpklassup.erpklassup.util.TotpUtil;
////
////import java.net.URL;
////import java.time.format.DateTimeFormatter;
////import java.util.*;
////import java.util.stream.Collectors;
////
////@SuppressWarnings({"unused", "SpellCheckingInspection"})
////public class UserDetailController implements Initializable {
////
////    // ===== HEADER =====
////    @FXML private Label avatarLabel;
////    @FXML private Label nomCompletLabel;
////    @FXML private Label usernameLabel;
////    @FXML private Label badgeStatut;
////    @FXML private Label badge2FA;
////
////    // ===== TABPANE =====
////    @FXML private TabPane tabPane;
////
////    // ===== ONGLET 1 : INFORMATIONS =====
////    @FXML private TextField nomField;
////    @FXML private TextField prenomField;
////    @FXML private TextField usernameField;
////    @FXML private TextField emailField;
////    @FXML private TextField telephoneField;
////    @FXML private ComboBox<String> typeUserCombo;
////    @FXML private Button btnAnnulerInfo;
////    @FXML private Button btnEnregistrerInfo;
////
////    // ===== ONGLET 2 : RÔLES =====
////    @FXML private Label roleCountLabel;
////    @FXML private TextField searchRoleField;
////    @FXML private FlowPane rolesFlowPane;
////    @FXML private Button btnEnregistrerRoles;
////
////    // ===== ONGLET 3 : PERMISSIONS =====
////    @FXML private Label permCountLabel;
////    @FXML private TextField searchPermissionField;
////    @FXML private Accordion accordionPermissions;
////    @FXML private Button btnEnregistrerPermissions;
////
////    // ===== ONGLET 4 : SÉCURITÉ =====
////    @FXML private ToggleButton toggleActif;
////    @FXML private ToggleButton toggle2FA;
////    @FXML private Button btnEnvoyerLien;
////    @FXML private Button btnGenererTemp;
////    @FXML private CheckBox doitChangerMdpCheckBox;
////    @FXML private Button btnEnregistrerSecurite;
////
////    // ===== ONGLET 5 : HISTORIQUE =====
////    @FXML private TableView<AuditLigne> auditTable;
////    @FXML private TableColumn<AuditLigne, String> colDateAudit;
////    @FXML private TableColumn<AuditLigne, String> colActionAudit;
////    @FXML private TableColumn<AuditLigne, String> colDetailAudit;
////    @FXML private TableColumn<AuditLigne, String> colIpAudit;
////    @FXML private Pagination paginationAudit;
////
////    // ===== CONSTANTES ET SERVICES =====
////    private static final int ELEMENTS_PAR_PAGE = 10;
////    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
////
////    private final UtilisateurService utilisateurService = new UtilisateurService();
////    private final RoleService roleService = new RoleService();
////    private final AuditService auditService = new AuditService();
////    private final UtilisateurPermissionDAO utilisateurPermissionDAO = new UtilisateurPermissionDAO();
////
////    private String idEcoleCourante;
////    private UtilisateurLigne utilisateurCourant;
////    private Runnable callbackRefresh;
////
////    private final List<CheckBox> listeCheckBoxRoles = new ArrayList<>();
////    private final Map<String, CheckBox> casesParIdPermission = new LinkedHashMap<>();
////    private List<AuditLigne> listeCompleteAudit = List.of();
////
////    @Override
////    public void initialize(URL url, ResourceBundle rb) {
////        this.idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();
////
////        if (typeUserCombo != null) {
////            typeUserCombo.setItems(FXCollections.observableArrayList("ADMIN", "PERSONNEL", "ENSEIGNANT", "ÉLÈVE", "PARENT"));
////        }
////
////        configurerRechercheFiltres();
////        configurerBoutonsEtActions();
////        configurerTableAudit();
////        configurerPagination();
////    }
////
////    public void initData(String idEcole, UtilisateurLigne ligne, Runnable onRefresh) {
////        this.idEcoleCourante = idEcole;
////        this.utilisateurCourant = ligne;
////        this.callbackRefresh = onRefresh;
////
////        if (utilisateurCourant != null) {
////            chargerEntete();
////            chargerFormulaireInfos();
////            chargerRoles();
////            chargerPermissionsOrganisees();
////            chargerSecurite();
////            chargerHistorique();
////        }
////    }
////
////    // ==========================================
////    // 1. EN-TÊTE & PROFIL
////    // ==========================================
////
////    @FXML
////    private void handleActiver2FA() {
////        if (utilisateurCourant == null) return;
////
////        // L'administrateur force la configuration du 2FA au prochain login de l'utilisateur
////        utilisateurService.forcerActivation2FAAsync(
////                utilisateurCourant.idUtilisateur(),
////                () -> Platform.runLater(() -> {
////                    // Mise à jour visuelle du bouton toggle
////                    toggle2FA.setSelected(true);
////                    toggle2FA.setText("2FA Requis");
////
////                    ToastNotification.succes(
////                            (Stage) toggle2FA.getScene().getWindow(),
////                            "L'utilisateur devra configurer sa 2FA lors de sa prochaine connexion."
////                    );
////
////                    // Traçabilité et mise à jour de la vue
////                    auditService.tracerActionAsync(
////                            "SECURITE",
////                            "ACTIVATION_OBLIGATOIRE_2FA",
////                            "Activation de l'obligation 2FA pour : " + utilisateurCourant.username()
////                    );
////                    chargerHistorique();
////                    notifierChangement();
////                }),
////                erreur -> Platform.runLater(() ->
////                        ToastNotification.erreur(
////                                (Stage) toggle2FA.getScene().getWindow(),
////                                "Erreur lors de la demande d'activation 2FA : " + erreur.getMessage()
////                        )
////                )
////        );
////    }
////    private void chargerEntete() {
////        String nom = utilisateurCourant.nom() != null ? utilisateurCourant.nom() : "";
////        String prenom = utilisateurCourant.prenom() != null ? utilisateurCourant.prenom() : "";
////
////        nomCompletLabel.setText(utilisateurCourant.nomComplet());
////        usernameLabel.setText("@" + utilisateurCourant.username());
////
////        String initiales = (nom.isEmpty() ? "" : nom.substring(0, 1)) + (prenom.isEmpty() ? "" : prenom.substring(0, 1));
////        avatarLabel.setText(initiales.toUpperCase());
////
////        boolean estActif = utilisateurCourant.statut() != null && utilisateurCourant.statut().getLibelle().equalsIgnoreCase("Actif");
////        mettreAJourBadgeStatut(estActif);
////        mettreAJourBadge2FA(utilisateurCourant.deuxFacteursActif());
////    }
////
////    private void mettreAJourBadgeStatut(boolean estActif) {
////        badgeStatut.setText(estActif ? "● Actif" : "● Inactif");
////        badgeStatut.getStyleClass().removeAll("tag-green", "tag-red");
////        badgeStatut.getStyleClass().add(estActif ? "tag-green" : "tag-red");
////    }
////
////    private void mettreAJourBadge2FA(boolean estActif2FA) {
////        badge2FA.setText(estActif2FA ? "● 2FA Activé" : "● 2FA Désactivé");
////        badge2FA.getStyleClass().removeAll("tag-blue", "tag-red");
////        badge2FA.getStyleClass().add(estActif2FA ? "tag-blue" : "tag-red");
////    }
////
////    private void chargerFormulaireInfos() {
////        nomField.setText(utilisateurCourant.nom());
////        prenomField.setText(utilisateurCourant.prenom());
////        usernameField.setText(utilisateurCourant.username());
////        emailField.setText(utilisateurCourant.email());
////        if (telephoneField != null) {
////            telephoneField.setText(utilisateurCourant.telephone());
////        }
////
////        if (typeUserCombo != null) {
////            if (utilisateurCourant.typeUtilisateur() != null) {
////                typeUserCombo.setValue(utilisateurCourant.typeUtilisateur());
////            }
////            typeUserCombo.setDisable(true); // 🔒 Type utilisateur non modifiable
////        }        usernameField.setDisable(true);
////    }
////
////    // ==========================================
////    // 2. GESTION DES RÔLES
////    // ==========================================
////    private void chargerRoles() {
////        rolesFlowPane.getChildren().clear();
////        listeCheckBoxRoles.clear();
////
////        utilisateurService.chargerRolesAsync(idEcoleCourante, roles -> Platform.runLater(() -> {
//////            rolePrincipalCombo.setItems(FXCollections.observableArrayList(roles));
////
////            for (RoleOption role : roles) {
////                CheckBox cb = new CheckBox(role.nomRole());
////                cb.setUserData(role);
////
////                if (utilisateurCourant.roles() != null && utilisateurCourant.roles().contains(role.nomRole())) {
////                    cb.setSelected(true);
////                }
////
////                cb.selectedProperty().addListener((_, _, _) -> mettreAJourCompteurRoles());
////                listeCheckBoxRoles.add(cb);
////                rolesFlowPane.getChildren().add(cb);
////            }
////            mettreAJourCompteurRoles();
////        }), _ -> Platform.runLater(() -> ToastNotification.erreur(getStage(), "Erreur lors du chargement des rôles.")));
////    }
////
////    private void mettreAJourCompteurRoles() {
////        long count = listeCheckBoxRoles.stream().filter(CheckBox::isSelected).count();
////        roleCountLabel.setText(count + " sélectionné(s)");
////    }
////
////    // ==========================================
////    // 3. PERMISSIONS PAR MODULE (ACCORDION)
////    // ==========================================
////    private void chargerPermissionsOrganisees() {
////        roleService.listerPermissionsAsync(
////                permissions -> Platform.runLater(() -> {
////                    construireAccordionPermissions(permissions);
////                    chargerPermissionsPossedees();
////                }),
////                _ -> Platform.runLater(() -> ToastNotification.erreur(getStage(), "Erreur lors de la récupération des permissions."))
////        );
////    }
////
////    private void construireAccordionPermissions(List<PermissionOption> permissions) {
////        Map<String, List<PermissionOption>> parCategorie = new LinkedHashMap<>();
////        for (PermissionOption p : permissions) {
////            parCategorie.computeIfAbsent(p.categorie(), _ -> new ArrayList<>()).add(p);
////        }
////
////        accordionPermissions.getPanes().clear();
////        casesParIdPermission.clear();
////
////        for (var entry : parCategorie.entrySet()) {
////            List<CheckBox> casesDuModule = new ArrayList<>();
////            VBox corps = new VBox(8);
////            corps.setPadding(new Insets(10, 8, 10, 8));
////            corps.getStyleClass().add("permissions-module-body");
////
////            for (PermissionOption perm : entry.getValue()) {
////                CheckBox cb = new CheckBox(perm.libelle());
////                cb.getStyleClass().add("permission-check");
////                cb.setUserData(perm.idPermission());
////                cb.selectedProperty().addListener((_, _, _) -> mettreAJourCompteurPermissions());
////
////                casesDuModule.add(cb);
////                casesParIdPermission.put(perm.idPermission(), cb);
////            }
////
////            CheckBox toutSelectionner = creerCaseToutSelectionner(casesDuModule);
////            corps.getChildren().add(toutSelectionner);
////            corps.getChildren().addAll(casesDuModule);
////
////            TitledPane pane = new TitledPane(entry.getKey(), corps);
////            pane.getStyleClass().add("permission-titled-pane");
////            accordionPermissions.getPanes().add(pane);
////        }
////
////        if (!accordionPermissions.getPanes().isEmpty()) {
////            accordionPermissions.setExpandedPane(accordionPermissions.getPanes().getFirst());
////        }
////    }
////
////    private CheckBox creerCaseToutSelectionner(List<CheckBox> cases) {
////        CheckBox toutSelectionner = new CheckBox("Tout sélectionner dans ce module");
////        toutSelectionner.getStyleClass().add("check-select-all");
////        toutSelectionner.setOnAction(_ -> {
////            boolean selectionne = toutSelectionner.isSelected();
////            cases.forEach(c -> c.setSelected(selectionne));
////        });
////
////        cases.forEach(c -> c.selectedProperty().addListener((_, _, _) -> {
////            long cochees = cases.stream().filter(CheckBox::isSelected).count();
////            toutSelectionner.setIndeterminate(cochees > 0 && cochees < cases.size());
////            toutSelectionner.setSelected(cochees == cases.size());
////        }));
////        return toutSelectionner;
////    }
////
////    private void chargerPermissionsPossedees() {
////        if (utilisateurCourant == null) return;
////
////        List<Permission> userPermissions = utilisateurPermissionDAO.findByUtilisateur(utilisateurCourant.idUtilisateur());
////        Set<String> permsUtilisateur = new HashSet<>();
////
////        for (Permission p : userPermissions) {
////            if (p.getIdPermission() != null) permsUtilisateur.add(p.getIdPermission());
////            if (p.getCodePermission() != null) permsUtilisateur.add(p.getCodePermission());
////        }
////
////        casesParIdPermission.forEach((idPerm, checkBox) -> {
////            boolean possede = permsUtilisateur.contains(idPerm) || permsUtilisateur.contains(checkBox.getText());
////            checkBox.setSelected(possede);
////        });
////
////        mettreAJourCompteurPermissions();
////    }
////
////    private void mettreAJourCompteurPermissions() {
////        long count = casesParIdPermission.values().stream().filter(CheckBox::isSelected).count();
////        permCountLabel.setText(count + " accordée(s)");
////    }
////
////    // ==========================================
////    // 4. SÉCURITÉ & TOGGLE BUTTONS
////    // ==========================================
////    private void chargerSecurite() {
////        boolean estActif = utilisateurCourant.statut() != null && utilisateurCourant.statut().getLibelle().equalsIgnoreCase("Actif");
////        boolean est2FA = utilisateurCourant.deuxFacteursActif();
////
////        // 1. Initialisation de l'état des boutons
////        toggleActif.setSelected(estActif);
////        toggleActif.setText(estActif ? "Actif" : "Inactif");
////
////        toggle2FA.setSelected(est2FA);
////        toggle2FA.setText(est2FA ? "2FA Activé" : "2FA Désactivé");
////
////        // 2. Écouteurs de changement dynamique d'état
////        toggleActif.selectedProperty().addListener((obs, oldVal, isSelected) -> {
////            toggleActif.setText(isSelected ? "Actif" : "Inactif");
////            mettreAJourBadgeStatut(isSelected);
////        });
////
////        toggle2FA.selectedProperty().addListener((obs, oldVal, isSelected) -> {
////            toggle2FA.setText(isSelected ? "2FA Activé" : "2FA Désactivé");
////            mettreAJourBadge2FA(isSelected);
////        });
////    }
////
////    // ==========================================
////    // 5. AUDIT / HISTORIQUE AVEC PAGINATION
////    // ==========================================
////
////    private void configurerTableAudit() {
////        colDateAudit.setCellValueFactory(data ->
////                new SimpleStringProperty(data.getValue().horodatage() != null ? data.getValue().horodatage().format(FORMAT_DATE) : ""));
////        colActionAudit.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().action()));
////        colDetailAudit.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().details()));
////        colIpAudit.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().adresseIp() != null ? data.getValue().adresseIp() : "—"));
////    }
////
////    private void configurerPagination() {
////        if (paginationAudit != null) {
////            paginationAudit.currentPageIndexProperty().addListener((_, _, newIdx) -> afficherPageAudit(newIdx.intValue()));
////        }
////    }
////
////    private void chargerHistorique() {
////        if (utilisateurCourant == null) return;
////        auditTable.setPlaceholder(new Label("Chargement de l'historique..."));
////
////        FiltreAudit filtre = new FiltreAudit(idEcoleCourante, utilisateurCourant.username(), null, null, null);
////        auditService.rechercherAsync(
////                filtre,
////                resultats -> Platform.runLater(() -> {
////                    this.listeCompleteAudit = resultats;
////                    int nombrePages = Math.max(1, (int) Math.ceil((double) listeCompleteAudit.size() / ELEMENTS_PAR_PAGE));
////
////                    if (paginationAudit != null) {
////                        paginationAudit.setPageCount(nombrePages);
////                        paginationAudit.setCurrentPageIndex(0);
////                    }
////                    afficherPageAudit(0);
////
////                    if (listeCompleteAudit.isEmpty()) {
////                        auditTable.setPlaceholder(new Label("Aucun historique trouvé pour cet utilisateur."));
////                    }
////                }),
////                _ -> Platform.runLater(() -> auditTable.setPlaceholder(new Label("Erreur lors du chargement de l'historique.")))
////        );
////    }
////
////    private void afficherPageAudit(int pageIndex) {
////        if (listeCompleteAudit.isEmpty()) {
////            auditTable.setItems(FXCollections.observableArrayList());
////            return;
////        }
////        int debut = pageIndex * ELEMENTS_PAR_PAGE;
////        int fin = Math.min(debut + ELEMENTS_PAR_PAGE, listeCompleteAudit.size());
////
////        if (debut < listeCompleteAudit.size()) {
////            auditTable.setItems(FXCollections.observableArrayList(listeCompleteAudit.subList(debut, fin)));
////        }
////    }
////
////    // ==========================================
////    // RECHERCHE & FILTRES
////    // ==========================================
////    private void configurerRechercheFiltres() {
////        searchRoleField.textProperty().addListener((_, _, n) -> {
////            String filtre = (n == null) ? "" : n.toLowerCase().trim();
////            rolesFlowPane.getChildren().clear();
////            for (CheckBox cb : listeCheckBoxRoles) {
////                if (cb.getText().toLowerCase().contains(filtre)) {
////                    rolesFlowPane.getChildren().add(cb);
////                }
////            }
////        });
////
////        searchPermissionField.textProperty().addListener((_, _, n) -> {
////            String filtre = (n == null) ? "" : n.toLowerCase().trim();
////            casesParIdPermission.forEach((_, cb) -> {
////                cb.setVisible(cb.getText().toLowerCase().contains(filtre));
////                cb.setManaged(cb.isVisible());
////            });
////        });
////    }
////
////    // ==========================================
////    // ACTIONS BOUTONS & AUDIT
////    // ==========================================
////    private void configurerBoutonsEtActions() {
////        btnEnregistrerInfo.setOnAction(_ -> enregistrerInformations());
////        btnAnnulerInfo.setOnAction(_ -> fermerFenetre());
////        btnEnregistrerRoles.setOnAction(_ -> enregistrerRoles());
////        btnEnregistrerPermissions.setOnAction(_ -> enregistrerPermissions());
////        btnEnregistrerSecurite.setOnAction(_ -> enregistrerSecurite());
////        btnGenererTemp.setOnAction(_ -> genererMotDePasseTemporaire());
////        btnEnvoyerLien.setOnAction(_ -> envoyerLienReinitialisation());
////    }
////
////    private void enregistrerInformations() {
////        String username = utilisateurCourant.username();
////
////        utilisateurService.modifierUtilisateurAsync(
////                utilisateurCourant.idUtilisateur(),
////                nomField.getText().trim(),
////                prenomField.getText().trim(),
////                emailField.getText().trim(),
////                telephoneField.getText().trim(),
////                () -> Platform.runLater(() -> {
////                    auditService.tracerActionAsync(
////                            "UTILISATEUR",
////                            "MODIFICATION_INFOS",
////                            "Mise à jour des informations personnelles de l'utilisateur : " + username
////                    );
////                    ToastNotification.succes(getStage(), "Informations modifiées.");
////                    chargerHistorique();
////                    notifierChangement();
////                }),
////                err -> Platform.runLater(() -> ToastNotification.erreur(getStage(), err.getMessage()))
////        );
////    }
////
////    private void enregistrerRoles() {
////        List<String> idsRoles = listeCheckBoxRoles.stream()
////                .filter(CheckBox::isSelected)
////                .map(cb -> ((RoleOption) cb.getUserData()).idRole())
////                .collect(Collectors.toList());
////
////        String username = utilisateurCourant.username();
////
////        utilisateurService.mettreAJourRolesAsync(
////                utilisateurCourant.idUtilisateur(),
////                idEcoleCourante,
////                idsRoles,
////                () -> Platform.runLater(() -> {
////                    auditService.tracerActionAsync(
////                            "HABILITATIONS",
////                            "AFFECTATION_ROLES",
////                            "Attribution de " + idsRoles.size() + " rôle(s) à l'utilisateur : " + username
////                    );
////                    ToastNotification.succes(getStage(), "Rôles enregistrés.");
////                    chargerHistorique();
////                    notifierChangement();
////                }),
////                err -> Platform.runLater(() -> ToastNotification.erreur(getStage(), "Erreur : " + err.getMessage()))
////        );
////    }
////
////    private void enregistrerPermissions() {
////        Set<String> idsPermissionsSelects = casesParIdPermission.entrySet().stream()
////                .filter(entry -> entry.getValue().isSelected())
////                .map(Map.Entry::getKey)
////                .collect(Collectors.toSet());
////
////        String username = utilisateurCourant.username();
////
////        auditService.tracerActionAsync(
////                "HABILITATIONS",
////                "MODIFICATION_PERMISSIONS_EXCEPTION",
////                "Mise à jour des permissions d'exception (" + idsPermissionsSelects.size() + " octroyée(s)) pour : " + username
////        );
////
////        ToastNotification.succes(getStage(), "Permissions d'exception enregistrées.");
////        chargerHistorique();
////        notifierChangement();
////    }
////
////    private void enregistrerSecurite() {
////        boolean nouveauStatut = toggleActif.isSelected();
////        String username = utilisateurCourant.username();
////        boolean doitConfigurer2FA = toggle2FA.isSelected();
////        boolean doitChangerMdp = doitChangerMdpCheckBox != null && doitChangerMdpCheckBox.isSelected();
////        String telephone = telephoneField != null ? telephoneField.getText().trim() : "";
////
////        // 1. Mise à jour du statut (Actif / Inactif)
////        utilisateurService.changerStatutActifAsync(
////                utilisateurCourant.idUtilisateur(),
////                nouveauStatut,
////                () -> Platform.runLater(() -> {
////                    auditService.tracerActionAsync(
////                            "SECURITE",
////                            "CHANGEMENT_STATUT_COMPTE",
////                            "Compte de " + username + " passé à : " + (nouveauStatut ? "ACTIF" : "INACTIF")
////                    );
////                    ToastNotification.succes(getStage(), "Statut de sécurité mis à jour.");
////                    chargerHistorique();
////                    notifierChangement();
////                }),
////                _ -> Platform.runLater(() -> ToastNotification.erreur(getStage(), "Erreur de modification du statut."))
////        );
////
////        // 2. Mise à jour des exigences de sécurité (Flag 2FA requis, Mdp requis, téléphone)
////        utilisateurService.mettreAJourSecuriteAsync(
////                utilisateurCourant.idUtilisateur(),
////                doitConfigurer2FA,
////                doitChangerMdp,
////                telephone,
////                () -> Platform.runLater(() -> {
////                    auditService.tracerActionAsync(
////                            "SECURITE",
////                            "MODIFICATION_PARAMETRES_SECURITE",
////                            "Mise à jour sécurité de : " + username
////                    );
////                    ToastNotification.succes(getStage(), "Sécurité et téléphone mis à jour avec succès.");
////                    chargerHistorique();
////                    notifierChangement();
////                }),
////                err -> Platform.runLater(() -> ToastNotification.erreur(getStage(), err.getMessage()))
////        );
////    }
////
////    private void genererMotDePasseTemporaire() {
////        String tempMdp = "Temp@" + (int)(Math.random() * 9000 + 1000);
////        String username = utilisateurCourant.username();
////
////        utilisateurService.reinitialiserMotDePasseAsync(
////                utilisateurCourant.idUtilisateur(),
////                tempMdp,
////                () -> Platform.runLater(() -> {
////                    auditService.tracerActionAsync(
////                            "SECURITE",
////                            "REINITIALISATION_MDP",
////                            "Génération d'un mot de passe temporaire pour : " + username
////                    );
////                    ToastNotification.info(getStage(), "Mot de passe temporaire : " + tempMdp);
////                    chargerHistorique();
////                }),
////                _ -> Platform.runLater(() -> ToastNotification.erreur(getStage(), "Erreur."))
////        );
////    }
////
////    private void envoyerLienReinitialisation() {
////        ToastNotification.info(getStage(), "Lien de réinitialisation envoyé à : " + emailField.getText());
////    }
////
////    private void notifierChangement() {
////        if (callbackRefresh != null) callbackRefresh.run();
////    }
////
////    private void fermerFenetre() {
////        Stage stage = getStage();
////        if (stage != null) stage.close();
////    }
////
////    private Stage getStage() {
////        return (Stage) tabPane.getScene().getWindow();
////    }
////}