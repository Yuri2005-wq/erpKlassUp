package org.erpklassup.erpklassup.controllers.actionPage;

import eu.hansolo.tilesfx.Command;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.erpklassup.erpklassup.dto.*;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.UtilisateurService;
import org.erpklassup.erpklassup.util.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class UtilisateursView implements Initializable, VueDisposable {

    @FXML private TextField searchField;
    @FXML private ComboBox<RoleOption> filterRole;
    @FXML private ComboBox<StatutCompte> filterStatut;
    @FXML private Button btnActionsGroupees;
    @FXML private Button btnAjouterUtilisateur;
    private final AuditService auditService = new AuditService();

    @FXML private TableView<UtilisateurLigne> tableUtilisateurs;
    @FXML private TableColumn<UtilisateurLigne, Boolean> colSelect;
    @FXML private CheckBox checkAllUsers;
    @FXML private TableColumn<UtilisateurLigne, UtilisateurLigne> colUtilisateur;
    @FXML private TableColumn<UtilisateurLigne, String> colRole;
    @FXML private TableColumn<UtilisateurLigne, UtilisateurLigne> colStatut;
    @FXML private TableColumn<UtilisateurLigne, UtilisateurLigne> colSecurite;
    @FXML private TableColumn<UtilisateurLigne, UtilisateurLigne> colDerniereConnexion;
    @FXML private TableColumn<UtilisateurLigne, UtilisateurLigne> colActions;
    @FXML private Button btnClearSearch;

    @FXML private Label statTotalLabel;
    @FXML private Label statConnectesLabel;
    @FXML private Label statActifsLabel;
    @FXML private Label statVerrouillesLabel;
    @FXML private Label statSuspendusLabel;
    @FXML private Pagination paginationAudit;

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int TAILLE_PAGE = FiltreUtilisateur.TAILLE_PAGE_DEFAUT;

    private final UtilisateurService service = new UtilisateurService();
    private final Set<String> selectionnes = new HashSet<>();
    private final PauseTransition debounceRecherche = new PauseTransition(Duration.millis(350));
    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;

    // ✅ ÉCOUTEUR i18n
    private final Runnable ecouteurI18n = this::rafraichirTextes;

    private String idEcoleCourante;
    private GestionnairePagination<UtilisateurLigne> gestionnairePagination;

    // ✅ Raccourci i18n
    private I18nManager i18n() {
        return I18nManager.getInstance();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();

        gestionnairePagination = new GestionnairePagination<>(tableUtilisateurs, paginationAudit, TAILLE_PAGE);

        configurerTable();
        configurerFiltres();
        configurerRecherche();
        configurerBoutonsHautNiveau();

        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
        appliquerControlesAcces();

        // ✅ S'abonner aux changements de langue
        i18n().ecouterChangement(ecouteurI18n);

        if (idEcoleCourante == null) {
            tableUtilisateurs.setPlaceholder(new Label(i18n().t("users.value.no_school")));
            return;
        }

        chargerRoles();
        lancerRecherche();
    }

    @Override
    public void disposer() {
        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
        i18n().arreterEcoute(ecouteurI18n);   // ✅
    }

    /** ✅ Rafraîchit les textes dynamiques après changement de langue. */
    private void rafraichirTextes() {
        if (tableUtilisateurs != null) {
            tableUtilisateurs.refresh();
        }

        // Si la table est vide → mettre à jour le placeholder
        if (tableUtilisateurs != null && tableUtilisateurs.getItems().isEmpty()) {
            tableUtilisateurs.setPlaceholder(new Label(i18n().t("users.value.no_user")));
        }
    }

    private void appliquerControlesAcces() {
        ControleAcces.appliquerAction(btnAjouterUtilisateur, "user.creer");
        ControleAcces.appliquerAction(btnActionsGroupees, "user.activer");
        tableUtilisateurs.refresh();
    }

    private void configurerTable() {
        tableUtilisateurs.setEditable(true);

        colSelect.setCellValueFactory(data -> {
            UtilisateurLigne ligne = data.getValue();
            SimpleBooleanProperty prop = new SimpleBooleanProperty(selectionnes.contains(ligne.idUtilisateur()));
            prop.addListener((obs, ancien, nouveau) -> {
                if (Boolean.TRUE.equals(nouveau)) {
                    selectionnes.add(ligne.idUtilisateur());
                } else {
                    selectionnes.remove(ligne.idUtilisateur());
                }
            });
            return prop;
        });
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colSelect.setEditable(true);

        checkAllUsers.setOnAction(e -> {
            boolean coche = checkAllUsers.isSelected();
            tableUtilisateurs.getItems().forEach(ligne -> {
                if (coche) {
                    selectionnes.add(ligne.idUtilisateur());
                } else {
                    selectionnes.remove(ligne.idUtilisateur());
                }
            });
            tableUtilisateurs.refresh();
        });

        colUtilisateur.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colUtilisateur.setCellFactory(col -> new TableCell<>() {
            private final Label nomLabel = new Label();
            private final Label emailLabel = new Label();
            private final VBox texte = new VBox(2, nomLabel, emailLabel);
            private final HBox conteneur = new HBox(10);
            {
                nomLabel.getStyleClass().add("cell-title");
                emailLabel.getStyleClass().add("cell-subtitle");
            }

            @Override protected void updateItem(UtilisateurLigne ligne, boolean vide) {
                super.updateItem(ligne, vide);
                if (vide || ligne == null) { setGraphic(null); return; }
                conteneur.getChildren().setAll(
                        AvatarUtil.creer(ligne.photoPath(), ligne.nom(), ligne.prenom(), ligne.idUtilisateur()),
                        texte
                );
                conteneur.setAlignment(Pos.CENTER_LEFT);
                nomLabel.setText(ligne.nomComplet());
                emailLabel.setText(ligne.email() != null ? ligne.email() : ligne.username());
                setGraphic(conteneur);
            }
        });

        // ✅ Rôle — avec i18n pour "Aucun rôle"
        colRole.setCellValueFactory(data -> {
            String roles = data.getValue().roles();
            return new SimpleStringProperty(
                    roles == null || roles.isBlank()
                            ? i18n().t("users.value.no_role")
                            : roles);
        });

        // ✅ Statut — avec i18n pour les libellés
        colStatut.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(UtilisateurLigne ligne, boolean vide) {
                super.updateItem(ligne, vide);
                if (vide || ligne == null) { setGraphic(null); return; }

                // ✅ Libellé traduit selon l'énum
                String libelle = switch (ligne.statut()) {
                    case ACTIF -> i18n().t("status.active");
                    case VERROUILLE -> i18n().t("status.locked");
                    case INACTIF -> i18n().t("status.inactive");
                };

                Label badge = new Label(libelle);
                badge.getStyleClass().addAll("badge-statut", switch (ligne.statut()) {
                    case ACTIF -> "badge-succes";
                    case VERROUILLE -> "badge-danger";
                    case INACTIF -> "badge-neutre";
                });
                setGraphic(badge);
            }
        });

        colSecurite.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colSecurite.setCellFactory(col -> new TableCell<>() {
            private final FontIcon icone = new FontIcon();
            @Override protected void updateItem(UtilisateurLigne ligne, boolean vide) {
                super.updateItem(ligne, vide);
                if (vide || ligne == null) { setGraphic(null); return; }
                icone.setIconLiteral(ligne.deuxFacteursActif() ? "fth-shield" : "fth-shield-off");
                icone.getStyleClass().setAll(ligne.deuxFacteursActif() ? "icone-actif" : "icone-inactif");
                setGraphic(icone);
            }
        });

        // ✅ Dernière connexion — avec i18n pour "Jamais connecté"
        colDerniereConnexion.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colDerniereConnexion.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(UtilisateurLigne ligne, boolean vide) {
                super.updateItem(ligne, vide);
                setText(vide || ligne == null ? null
                        : ligne.derniereConnexion() != null
                          ? ligne.derniereConnexion().format(FORMAT_DATE)
                          : i18n().t("users.value.never_connected"));
            }
        });

        colActions.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnVoir = new Button();
            private final Button btnSupprimer = new Button();
            private final HBox conteneur = new HBox(6, btnVoir, btnSupprimer);

            {
                conteneur.setAlignment(Pos.CENTER);

                FontIcon iconVoir = new FontIcon("fth-eye");
                iconVoir.setIconSize(14);
                btnVoir.setGraphic(iconVoir);
                btnVoir.getStyleClass().add("btn-icon-light");
                btnVoir.setTooltip(new Tooltip(i18n().t("users.tooltip.view")));

                FontIcon iconDelete = new FontIcon("fth-trash-2");
                iconDelete.setIconSize(14);
                btnSupprimer.setGraphic(iconDelete);
                btnSupprimer.getStyleClass().addAll("btn-icon-light", "btn-danger-icon");
                btnSupprimer.setTooltip(new Tooltip(i18n().t("users.tooltip.delete")));

                btnVoir.setOnAction(e -> {
                    UtilisateurLigne ligne = getItem();
                    if (ligne != null) ouvrirModalEditionUtilisateur(ligne);
                });

                btnSupprimer.setOnAction(e -> {
                    UtilisateurLigne ligne = getItem();
                    if (ligne != null) supprimerUtilisateur(ligne);
                });
            }

            @Override protected void updateItem(UtilisateurLigne ligne, boolean vide) {
                super.updateItem(ligne, vide);
                if (vide || ligne == null) {
                    setGraphic(null);
                } else {
                    btnVoir.setVisible(ControleAcces.autoriseAction("user.modifier"));
                    btnVoir.setManaged(btnVoir.isVisible());

                    btnSupprimer.setVisible(ControleAcces.autoriseAction("user.supprimer"));
                    btnSupprimer.setManaged(btnSupprimer.isVisible());

                    setGraphic(conteneur);
                }
            }
        });
    }

    private void configurerFiltres() {
        ObservableList<StatutCompte> statuts = FXCollections.observableArrayList();
        statuts.add(null);
        statuts.addAll(StatutCompte.values());

        filterStatut.setItems(statuts);

        filterStatut.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(StatutCompte item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(i18n().t("users.filter.all_status"));
                } else {
                    // ✅ Libellé traduit
                    setText(switch (item) {
                        case ACTIF -> i18n().t("status.active");
                        case VERROUILLE -> i18n().t("status.locked");
                        case INACTIF -> i18n().t("status.inactive");
                    });
                }
            }
        });
        filterStatut.setButtonCell(filterStatut.getCellFactory().call(null));
        filterStatut.getSelectionModel().selectFirst();

        filterStatut.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> lancerRecherche());
        filterRole.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> lancerRecherche());
    }

    private void configurerRecherche() {
        debounceRecherche.setOnFinished(e -> lancerRecherche());

        searchField.textProperty().addListener((obs, ancien, nouveau) -> {
            boolean aDuTexte = nouveau != null && !nouveau.trim().isEmpty();

            if (btnClearSearch != null) {
                btnClearSearch.setVisible(aDuTexte);
            }

            debounceRecherche.playFromStart();
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnAction(e -> {
                searchField.clear();
                searchField.requestFocus();
            });
        }
    }

    private void configurerBoutonsHautNiveau() {
        btnAjouterUtilisateur.setOnAction(e -> ouvrirModalCreationUtilisateur());
        btnActionsGroupees.setOnAction(this::ouvrirActionsGroupees);
    }

    private void chargerRoles() {
        service.chargerRolesAsync(idEcoleCourante,
                roles -> Platform.runLater(() -> {
                    ObservableList<RoleOption> listeRoles = FXCollections.observableArrayList();
                    listeRoles.add(new RoleOption(null, null, i18n().t("users.filter.all_roles")));
                    listeRoles.addAll(roles);

                    filterRole.setCellFactory(lv -> new ListCell<>() {
                        @Override
                        protected void updateItem(RoleOption item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null || item.nomRole() == null
                                    ? i18n().t("users.filter.all_roles")
                                    : item.nomRole());
                        }
                    });
                    filterRole.setButtonCell(filterRole.getCellFactory().call(null));

                    filterRole.setItems(listeRoles);
                    filterRole.getSelectionModel().selectFirst();
                }),
                erreur -> Platform.runLater(() -> afficherErreurDialogue(erreur)));
    }

    private void lancerRecherche() {
        if (idEcoleCourante == null) return;

        RoleOption role = filterRole.getSelectionModel().getSelectedItem();
        StatutCompte statut = filterStatut.getSelectionModel().getSelectedItem();

        FiltreUtilisateur filtre = new FiltreUtilisateur(
                idEcoleCourante,
                (role != null) ? role.idRole() : null,
                statut,
                searchField.getText(),
                1,
                Integer.MAX_VALUE
        );

        tableUtilisateurs.setPlaceholder(new Label(i18n().t("users.value.loading")));

        service.rechercherAsync(filtre,
                resultat -> Platform.runLater(() -> afficherResultat(resultat)),
                erreur -> Platform.runLater(() -> {
                    tableUtilisateurs.setPlaceholder(new Label(i18n().t("users.value.error_loading")));
                    afficherErreurDialogue(erreur);
                }));
    }

    private void afficherResultat(ResultatPagine<UtilisateurLigne> resultat) {
        List<UtilisateurLigne> liste = resultat.elements();

        gestionnairePagination.mettreAJourDonnees(liste);

        tableUtilisateurs.setPlaceholder(new Label(i18n().t("users.value.no_user")));

        mettreAJourStatistiques(liste, resultat.total());
        checkAllUsers.setSelected(false);
        selectionnes.clear();
    }

    private void ouvrirModalCreationUtilisateur() {
        String fxmlPath = "/org/erpklassup/erpklassup/view/create-user-view.fxml";

        UserCreateController ctrl = ModalUtil.ouvrirModal(
                getClass(),
                fxmlPath,
                i18n().t("users.modal.create.title"),
                tableUtilisateurs.getScene().getWindow(),
                695,
                680
        );

        if (ctrl != null) {
            ctrl.initData(this.idEcoleCourante, filterRole.getItems(), this::lancerRecherche);
        }
    }

    private void ouvrirModalEditionUtilisateur(UtilisateurLigne ligne) {
        if (ligne == null) return;

        String fxmlPath = "/org/erpklassup/erpklassup/view/user-detail-admin-page.fxml";

        UserDetailController ctrl = ModalUtil.ouvrirModal(
                getClass(),
                fxmlPath,
                i18n().t("users.modal.edit.title"),
                tableUtilisateurs.getScene().getWindow(),
                806,
                668
        );
        if (ctrl != null) {
            ctrl.initData(this.idEcoleCourante, ligne, this::lancerRecherche);
        }
    }

    private void supprimerUtilisateur(UtilisateurLigne ligne) {
        Boolean confirmation = AlertUtil.afficherConfirmation(
                i18n().t("users.confirm.delete.title"),
                i18n().t("users.confirm.delete.message", ligne.nomComplet()),
                tableUtilisateurs.getScene().getWindow()
        );

        if (confirmation) {
            Command lancerRecherche = this::lancerRecherche;
            service.supprimerAsync(ligne.idUtilisateur(),
                    () -> Platform.runLater(() -> {
                        auditService.tracerActionAsync("HABILITATIONS", "SUPPRESSION_UTILISATEUR",
                                "Utilisateur Supprimé : " + ligne.nomComplet());
                        ToastNotification.succes((Stage) tableUtilisateurs.getScene().getWindow(),
                                i18n().t("users.toast.deleted", ligne.nomComplet()));
                        lancerRecherche();
                    }),
                    erreur -> Platform.runLater(() -> {
                        ToastNotification.erreur((Stage) tableUtilisateurs.getScene().getWindow(),
                                i18n().t("users.toast.error_delete", erreur.getMessage()));
                        System.err.println(erreur);
                    })
            );
        }
    }

    private void ouvrirActionsGroupees(ActionEvent event) {
        if (selectionnes.isEmpty()) {
            AlertUtil.afficherInformation(
                    i18n().t("common.info"),
                    i18n().t("users.bulk.no_selection"));
            return;
        }

        ContextMenu menu = new ContextMenu();

        MenuItem activer = new MenuItem(i18n().t("users.bulk.activate"));
        activer.setOnAction(e -> changerStatutEnMasse(true));

        MenuItem desactiver = new MenuItem(i18n().t("users.bulk.deactivate"));
        desactiver.setOnAction(e -> changerStatutEnMasse(false));

        MenuItem supprimer = new MenuItem(i18n().t("users.bulk.delete"));
        supprimer.setOnAction(e -> {
            boolean confirmation = AlertUtil.afficherConfirmation(
                    i18n().t("users.confirm.bulk_delete.title"),
                    i18n().t("users.confirm.bulk_delete.message", selectionnes.size()),
                    tableUtilisateurs.getScene().getWindow()
            );
            if (confirmation) {
                Command lancerRecherche = this::lancerRecherche;
                service.supprimerEnMasseAsync(new HashSet<>(selectionnes),
                        () -> Platform.runLater(() -> {
                            auditService.tracerActionAsync("HABILITATIONS", "SUPPRESSION_UTILISATEUR",
                                    "Utilisateur(s) " + selectionnes + " supprimé(s)");
                            ToastNotification.succes((Stage) tableUtilisateurs.getScene().getWindow(),
                                    i18n().t("users.toast.deleted_bulk"));
                            lancerRecherche();
                        }),
                        erreur -> Platform.runLater(() ->
                                ToastNotification.erreur((Stage) tableUtilisateurs.getScene().getWindow(),
                                        i18n().t("users.toast.error_delete", erreur.getMessage())))
                );
            }
        });

        menu.getItems().addAll(activer, desactiver, new SeparatorMenuItem(), supprimer);
        menu.show(btnActionsGroupees, Side.BOTTOM, 0, 4);
    }

    private void changerStatutEnMasse(boolean actif) {
        Set<String> copiesIds = new HashSet<>(selectionnes);
        AtomicInteger reste = new AtomicInteger(copiesIds.size());

        for (String id : copiesIds) {
            service.changerStatutActifAsync(id, actif, () -> {
                if (reste.decrementAndGet() == 0) {
                    Platform.runLater(this::lancerRecherche);
                }
            }, erreur -> Platform.runLater(() -> afficherErreurDialogue(erreur)));
        }
    }

    private void afficherErreurDialogue(Throwable erreur) {
        new Alert(Alert.AlertType.ERROR,
                erreur != null && erreur.getMessage() != null
                        ? erreur.getMessage()
                        : i18n().t("common.error")).showAndWait();
    }

    private void mettreAJourStatistiques(List<UtilisateurLigne> listeUtilisateurs, long totalElements) {
        if (listeUtilisateurs == null) {
            mettreAJourEtAnimer(statTotalLabel, "0");
            mettreAJourEtAnimer(statConnectesLabel, "0");
            mettreAJourEtAnimer(statActifsLabel, "0");
            mettreAJourEtAnimer(statVerrouillesLabel, "0");
            mettreAJourEtAnimer(statSuspendusLabel, "0");
            return;
        }

        long actifs = 0;
        long verrouilles = 0;
        long suspendus = 0;

        for (UtilisateurLigne u : listeUtilisateurs) {
            if (u.statut() != null) {
                switch (u.statut()) {
                    case ACTIF -> actifs++;
                    case VERROUILLE -> verrouilles++;
                    case INACTIF -> suspendus++;
                }
            }
        }

        service.compterConnectesAsync(idEcoleCourante,
                nbConnectes -> Platform.runLater(() ->
                        mettreAJourEtAnimer(statConnectesLabel, String.valueOf(nbConnectes))),
                erreur -> Platform.runLater(() -> {
                    System.err.println("❌ Erreur comptage connectés : " + erreur.getMessage());
                    mettreAJourEtAnimer(statConnectesLabel, "0");
                }));

        mettreAJourEtAnimer(statTotalLabel, String.valueOf(totalElements));
        mettreAJourEtAnimer(statActifsLabel, String.valueOf(actifs));
        mettreAJourEtAnimer(statVerrouillesLabel, String.valueOf(verrouilles));
        mettreAJourEtAnimer(statSuspendusLabel, String.valueOf(suspendus));
    }

    private void mettreAJourEtAnimer(Label label, String nouvelleValeur) {
        if (label == null) return;

        boolean premierChargement = label.getText().equals("0") && !nouvelleValeur.equals("0");
        boolean valeurChangee = !label.getText().equals(nouvelleValeur);

        label.setText(nouvelleValeur);

        if (valeurChangee || premierChargement) {
            animerChangementStat(label);
        }
    }

    private void animerChangementStat(Label labelStat) {
        if (labelStat == null || labelStat.getParent() == null) return;

        Node carte = labelStat.getParent().getParent();
        if (carte == null) carte = labelStat.getParent();

        final Node conteneurCarte = carte;
        String styleOrigine = conteneurCarte.getStyle();

        conteneurCarte.setStyle(styleOrigine + "; -fx-background-color: rgba(255, 165, 0, 0.30); -fx-background-radius: 6px;");

        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> conteneurCarte.setStyle(styleOrigine));
        pause.play();
    }
}