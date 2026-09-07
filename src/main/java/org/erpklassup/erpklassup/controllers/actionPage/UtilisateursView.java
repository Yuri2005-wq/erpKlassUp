package org.erpklassup.erpklassup.controllers.actionPage;

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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.erpklassup.erpklassup.dto.*;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.UtilisateurService;
import org.erpklassup.erpklassup.util.AvatarUtil;
import org.erpklassup.erpklassup.util.VueDisposable;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDateTime;
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

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int TAILLE_PAGE = FiltreUtilisateur.TAILLE_PAGE_DEFAUT;

    private final UtilisateurService service = new UtilisateurService();
    private final Set<String> selectionnes = new HashSet<>();
    private final PauseTransition debounceRecherche = new PauseTransition(Duration.millis(350));
    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;

    private String idEcoleCourante;
    private int pageCourante = 1;
    private int nombreTotalDePages = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();

        configurerTable();
        configurerFiltres();
        configurerRecherche();
        configurerBoutonsHautNiveau();

        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
        appliquerControlesAcces();

        if (idEcoleCourante == null) {
            tableUtilisateurs.setPlaceholder(new Label("Aucune école active — connectez-vous d'abord."));
            return;
        }

        chargerRoles();
        lancerRecherche();
    }

    @Override
    public void disposer() {
        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
    }

    private void appliquerControlesAcces() {
        org.erpklassup.erpklassup.util.ControleAcces.appliquerAction(btnAjouterUtilisateur, "user.creer");
        org.erpklassup.erpklassup.util.ControleAcces.appliquerAction(btnActionsGroupees, "user.modifier");
    }

    // ---------- Configuration ----------

    private void configurerTable() {
        tableUtilisateurs.setEditable(true);
        tableUtilisateurs.setItems(FXCollections.observableArrayList());

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

        colRole.setCellValueFactory(data -> {
            String roles = data.getValue().roles();
            return new SimpleStringProperty(roles == null || roles.isBlank() ? "Aucun rôle" : roles);
        });

        colStatut.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(UtilisateurLigne ligne, boolean vide) {
                super.updateItem(ligne, vide);
                if (vide || ligne == null) { setGraphic(null); return; }
                Label badge = new Label(ligne.statut().getLibelle());
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

        colDerniereConnexion.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colDerniereConnexion.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(UtilisateurLigne ligne, boolean vide) {
                super.updateItem(ligne, vide);
                setText(vide || ligne == null ? null
                        : ligne.derniereConnexion() != null ? ligne.derniereConnexion().format(FORMAT_DATE) : "Jamais connecté");
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
                btnVoir.setTooltip(new Tooltip("Voir / Modifier l'utilisateur"));

                FontIcon iconDelete = new FontIcon("fth-trash-2");
                iconDelete.setIconSize(14);
                btnSupprimer.setGraphic(iconDelete);
                btnSupprimer.getStyleClass().addAll("btn-icon-light", "btn-danger-icon");
                btnSupprimer.setTooltip(new Tooltip("Supprimer l'utilisateur"));

                btnVoir.setOnAction(e -> {
                    UtilisateurLigne ligne = getItem();
                    if (ligne != null) ouvrirFormulaire(ligne);
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
                    btnVoir.setVisible(org.erpklassup.erpklassup.util.ControleAcces.autoriseAction("user.voir"));
                    btnVoir.setManaged(btnVoir.isVisible());

                    btnSupprimer.setVisible(org.erpklassup.erpklassup.util.ControleAcces.autoriseAction("user.supprimer"));
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
                setText(empty || item == null ? "Tous les Statuts" : item.getLibelle());
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
        btnAjouterUtilisateur.setOnAction(e -> ouvrirFormulaire(null));
        btnActionsGroupees.setOnAction(this::ouvrirActionsGroupees);
    }

    private void chargerRoles() {
        service.chargerRolesAsync(idEcoleCourante,
                roles -> Platform.runLater(() -> {
                    ObservableList<RoleOption> listeRoles = FXCollections.observableArrayList();
                    listeRoles.add(new RoleOption(null, null, "Tous les Rôles"));
                    listeRoles.addAll(roles);

                    filterRole.setCellFactory(lv -> new ListCell<>() {
                        @Override
                        protected void updateItem(RoleOption item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null || item.nomRole() == null ? "Tous les Rôles" : item.nomRole());
                        }
                    });
                    filterRole.setButtonCell(filterRole.getCellFactory().call(null));

                    filterRole.setItems(listeRoles);
                    filterRole.getSelectionModel().selectFirst();
                }),
                erreur -> Platform.runLater(() -> afficherErreurDialogue(erreur)));
    }

    // ---------- Recherche / listing ----------

    private void lancerRecherche() {
        pageCourante = 1;
        executerRecherche();
    }

    private void executerRecherche() {
        if (idEcoleCourante == null) return;

        RoleOption role = filterRole.getSelectionModel().getSelectedItem();
        StatutCompte statut = filterStatut.getSelectionModel().getSelectedItem();

        FiltreUtilisateur filtre = new FiltreUtilisateur(
                idEcoleCourante,
                (role != null) ? role.idRole() : null,
                statut,
                searchField.getText(),
                pageCourante,
                TAILLE_PAGE
        );

        tableUtilisateurs.setPlaceholder(new Label("Chargement..."));

        service.rechercherAsync(filtre,
                resultat -> Platform.runLater(() -> afficherResultat(resultat)),
                erreur -> Platform.runLater(() -> {
                    tableUtilisateurs.setPlaceholder(new Label("Erreur de chargement des utilisateurs"));
                    afficherErreurDialogue(erreur);
                }));
    }

    private void afficherResultat(ResultatPagine<UtilisateurLigne> resultat) {
        tableUtilisateurs.setItems(FXCollections.observableArrayList(resultat.elements()));
        tableUtilisateurs.setPlaceholder(new Label("Aucun utilisateur trouvé"));

        nombreTotalDePages = resultat.nombreDePages();

        mettreAJourStatistiques(resultat.elements(), resultat.total());
        checkAllUsers.setSelected(false);
        selectionnes.clear();
    }

    // ---------- Formulaire ajout / modification ----------

    private void ouvrirFormulaire(UtilisateurLigne ligneExistante) {
        boolean modification = ligneExistante != null;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(modification ? "Détails de l'utilisateur" : "Ajouter un utilisateur");
        ButtonType boutonValider = new ButtonType(modification ? "Enregistrer" : "Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(boutonValider, ButtonType.CANCEL);

        TextField champUsername = new TextField();
        TextField champNom = new TextField();
        TextField champPrenom = new TextField();
        TextField champEmail = new TextField();
        PasswordField champMotDePasse = new PasswordField();
        ComboBox<RoleOption> champRole = new ComboBox<>(filterRole.getItems());
        champMotDePasse.setPromptText(modification ? "Laisser vide pour ne pas changer" : "");

        if (modification) {
            champUsername.setText(ligneExistante.username());
            champUsername.setDisable(true);
            champNom.setText(ligneExistante.nom());
            champPrenom.setText(ligneExistante.prenom());
            champEmail.setText(ligneExistante.email());
        }

        GridPane grille = new GridPane();
        grille.setHgap(10); grille.setVgap(10); grille.setPadding(new Insets(16));
        grille.addRow(0, new Label("Nom d'utilisateur"), champUsername);
        grille.addRow(1, new Label("Nom"), champNom);
        grille.addRow(2, new Label("Prénom"), champPrenom);
        grille.addRow(3, new Label("Email"), champEmail);
        if (!modification) grille.addRow(4, new Label("Mot de passe"), champMotDePasse);
        grille.addRow(5, new Label("Rôle"), champRole);
        dialog.getDialogPane().setContent(grille);

        Node boutonNode = dialog.getDialogPane().lookupButton(boutonValider);
        boutonNode.addEventFilter(ActionEvent.ACTION, event -> {
            boolean invalide = champNom.getText().isBlank()
                    || (!modification && (champUsername.getText().isBlank() || champMotDePasse.getText().isBlank()));
            if (invalide) {
                new Alert(Alert.AlertType.WARNING, "Champs obligatoires manquants.").showAndWait();
                event.consume();
            }
        });

        dialog.showAndWait().filter(b -> b == boutonValider).ifPresent(b -> {
            if (modification) {
                service.modifierUtilisateurAsync(ligneExistante.idUtilisateur(),
                        champNom.getText().trim(), champPrenom.getText().trim(), champEmail.getText().trim(),
                        () -> Platform.runLater(this::executerRecherche),
                        erreur -> Platform.runLater(() -> afficherErreurDialogue(erreur)));
            } else {
                service.creerUtilisateurAsync(idEcoleCourante, champUsername.getText().trim(), champMotDePasse.getText(),
                        champNom.getText().trim(), champPrenom.getText().trim(), champEmail.getText().trim(),
                        champRole.getValue() != null ? champRole.getValue().idRole() : null,
                        () -> Platform.runLater(this::executerRecherche),
                        erreur -> Platform.runLater(() -> afficherErreurDialogue(erreur)));
            }
        });
    }

    // ---------- Actions ligne ----------

    private void supprimerUtilisateur(UtilisateurLigne ligne) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer définitivement " + ligne.nomComplet() + " ?", ButtonType.YES, ButtonType.NO);
        confirmation.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b ->
                service.supprimerAsync(ligne.idUtilisateur(),
                        () -> Platform.runLater(this::executerRecherche),
                        erreur -> Platform.runLater(() -> afficherErreurDialogue(erreur))));
    }

    // ---------- Actions par lot ----------

    private void ouvrirActionsGroupees(ActionEvent event) {
        if (selectionnes.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Sélectionnez au moins un utilisateur.").showAndWait();
            return;
        }

        ContextMenu menu = new ContextMenu();

        MenuItem activer = new MenuItem("Activer la sélection");
        activer.setOnAction(e -> changerStatutEnMasse(true));

        MenuItem desactiver = new MenuItem("Désactiver la sélection");
        desactiver.setOnAction(e -> changerStatutEnMasse(false));

        MenuItem supprimer = new MenuItem("Supprimer la sélection");
        supprimer.setOnAction(e -> {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Supprimer les " + selectionnes.size() + " utilisateur(s) sélectionné(s) ?", ButtonType.YES, ButtonType.NO);
            confirmation.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b ->
                    service.supprimerEnMasseAsync(new HashSet<>(selectionnes),
                            () -> Platform.runLater(this::executerRecherche),
                            erreur -> Platform.runLater(() -> afficherErreurDialogue(erreur))));
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
                    Platform.runLater(this::executerRecherche);
                }
            }, erreur -> Platform.runLater(() -> afficherErreurDialogue(erreur)));
        }
    }

    private void afficherErreurDialogue(Throwable erreur) {
        new Alert(Alert.AlertType.ERROR, erreur != null && erreur.getMessage() != null ? erreur.getMessage() : "Une erreur est survenue.").showAndWait();
    }

    // ---------- Gestion dynamique des statistiques ----------

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
        long connectes = 0;

        for (UtilisateurLigne u : listeUtilisateurs) {
            if (u.statut() != null) {
                switch (u.statut()) {
                    case ACTIF -> actifs++;
                    case VERROUILLE -> verrouilles++;
                    case INACTIF -> suspendus++;
                }
            }

            if (u.derniereConnexion() != null &&
                    u.derniereConnexion().isAfter(LocalDateTime.now().minusMinutes(15))) {
                connectes++;
            }
        }

        mettreAJourEtAnimer(statTotalLabel, String.valueOf(totalElements));
        mettreAJourEtAnimer(statActifsLabel, String.valueOf(actifs));
        mettreAJourEtAnimer(statVerrouillesLabel, String.valueOf(verrouilles));
        mettreAJourEtAnimer(statSuspendusLabel, String.valueOf(suspendus));
        mettreAJourEtAnimer(statConnectesLabel, String.valueOf(connectes));
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

        // Remontée vers la VBox conteneur de la carte
        Node carte = labelStat.getParent().getParent();
        if (carte == null) carte = labelStat.getParent();

        final Node conteneurCarte = carte;
        String styleOrigine = conteneurCarte.getStyle();

        // Application du style temporaire orange
        conteneurCarte.setStyle(styleOrigine + "; -fx-background-color: rgba(255, 165, 0, 0.30); -fx-background-radius: 6px;");

        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> conteneurCarte.setStyle(styleOrigine));
        pause.play();
    }
}