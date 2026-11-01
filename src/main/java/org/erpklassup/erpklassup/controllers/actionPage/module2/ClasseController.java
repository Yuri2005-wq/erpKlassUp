package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import org.erpklassup.erpklassup.models.module2.AnneeScolaire;
import org.erpklassup.erpklassup.models.module2.AnneeScolaire.Statut;
import org.erpklassup.erpklassup.models.module2.Classe;
import org.erpklassup.erpklassup.models.module2.Filiere;
import org.erpklassup.erpklassup.models.module2.Niveau;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.module2.AcademiqueService;
import org.erpklassup.erpklassup.service.module2.AnneeScolaireService;
import org.erpklassup.erpklassup.service.module2.ClasseService;
import org.erpklassup.erpklassup.util.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class ClasseController implements Initializable, VueDisposable {

    /* ============================================================
       FXML
       ============================================================ */
    @FXML private Label lblAnneeContexte;
    @FXML private TextField fieldRecherche;

    @FXML private ComboBox<AnneeScolaire>   filtreAnnee;
    @FXML private ComboBox<Niveau>          filtreNiveau;
    @FXML private ComboBox<Filiere>         filtreFiliere;
    @FXML private ComboBox<Classe.Vacation> filtreVacation;

    @FXML private Button btnCreationMultiple;
    @FXML private Button btnNouvelleClasse;

    @FXML private TableView<Classe> tableClasses;
    @FXML private TableColumn<Classe, String> colClasseNom;
    @FXML private TableColumn<Classe, String> colClasseNiveau;
    @FXML private TableColumn<Classe, String> colClasseFiliere;
    @FXML private TableColumn<Classe, Classe> colClasseVacation;
    @FXML private TableColumn<Classe, String> colClasseSalle;
    @FXML private TableColumn<Classe, String> colClasseTitulaire;
    @FXML private TableColumn<Classe, Classe> colClasseEffectif;
    @FXML private TableColumn<Classe, Classe> colClasseActions;

    @FXML private Label lblFooterClasses;
    @FXML private Pagination paginationClasses;

    /* ============================================================
       CONSTANTES
       ============================================================ */
    private static final Duration PERIODE_REFRESH    = Duration.seconds(10);
    private static final Duration DEBOUNCE_RECHERCHE = Duration.millis(350);
    private static final int      TAILLE_PAGE        = 20;

    /* ============================================================
       SERVICES
       ============================================================ */
    private final ClasseService        service           = new ClasseService();
    private final AnneeScolaireService anneeService      = new AnneeScolaireService();
    private final AcademiqueService    academiqueService = new AcademiqueService();

    /* ============================================================
       ÉTAT
       ============================================================ */
    private String  idEcoleCourante;
    private String  idAnneeCourante;
    private boolean anneeImposeeParAppelant = false;
    private boolean chargementEnCours       = false;

    /** Mode consultation activé si l'année est différente de l'année courante ou si statut != EN_COURS */
    private boolean modeConsultation = false;

    private GestionnairePagination<Classe> gestionnairePagination;
    private AutoRefreshService<List<Classe>> autoRefresh;
    private final PauseTransition debounceRecherche = new PauseTransition(DEBOUNCE_RECHERCHE);

    // Écouteurs enregistrés pour la session
    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;
    private final Runnable ecouteurAnnee       = this::surChangementAnneeGlobal;

    /* ============================================================
       INITIALISATION
       ============================================================ */
    @Override
    @SuppressWarnings("deprecation")
    public void initialize(URL url, ResourceBundle rb) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();

        gestionnairePagination = new GestionnairePagination<>(
                tableClasses, paginationClasses, TAILLE_PAGE);

        configurerTable();
        configurerFiltres();
        configurerRecherche();
        configurerBoutons();

        // Écoute des événements du SessionManager
        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
        SessionManager.getInstance().ecouterChangementAnnee(ecouteurAnnee);

        appliquerControlesAcces();

        if (idEcoleCourante == null) {
            afficherAucuneAnneeActive("Aucune école active - connectez-vous à une école.");
            return;
        }

        // Politique de redimensionnement standard des colonnes
        tableClasses.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Chargement des référentiels puis synchronisation avec l'année globale
        chargerReferentiels(this::synchroniserAvecAnneeGlobaleSession);
        demarrerRafraichissementAuto();
    }

    @Override
    public void disposer() {
        if (autoRefresh != null) {
            autoRefresh.dispose();
            autoRefresh = null;
        }
        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
        SessionManager.getInstance().arreterEcouteAnnee(ecouteurAnnee);
    }

    /* ============================================================
       SYNCHRONISATION ET ÉCOUTE DE L'ANNÉE SCOLAIRE (SESSION)
       ============================================================ */
    private void synchroniserAvecAnneeGlobaleSession() {
        if (anneeImposeeParAppelant) return;

        AnneeScolaire anneeActive = SessionManager.getInstance().getAnneeActive();
        if (anneeActive != null) {
            appliquerAnnee(anneeActive, true);
        } else {
            afficherAucuneAnneeActive("Aucune année scolaire active dans le système.");
        }
    }

    private void surChangementAnneeGlobal() {
        AnneeScolaire nouvelleAnnee = SessionManager.getInstance().getAnneeActive();
        if (nouvelleAnnee != null) {
            appliquerAnnee(nouvelleAnnee, true);
        } else {
            afficherAucuneAnneeActive("Aucune année scolaire active dans le système.");
        }
    }

    /* ============================================================
       REFERENTIELS
       ============================================================ */
    private void chargerReferentiels(Runnable onTermine) {
        /* ---------- 1. ANNEES SCOLAIRES ---------- */
        anneeService.listerAnneesAsync(idEcoleCourante,
                annees -> Platform.runLater(() -> {
                    ObservableList<AnneeScolaire> liste = FXCollections.observableArrayList();
                    if (annees != null) liste.addAll(annees);

                    StringConverter<AnneeScolaire> conv = converterAnnee();
                    filtreAnnee.setConverter(conv);
                    filtreAnnee.setButtonCell(boutonCellule(conv));
                    filtreAnnee.setItems(liste);

                    if (onTermine != null) onTermine.run();
                }),
                err -> Platform.runLater(() -> {
                    System.err.println("❌ [Années] : " + (err != null ? err.getMessage() : "?"));
                    if (onTermine != null) onTermine.run();
                }));

        /* ---------- 2. NIVEAUX ---------- */
        academiqueService.listerNiveauxParEcoleSansJetonAsync(idEcoleCourante,
                niveaux -> Platform.runLater(() -> {
                    ObservableList<Niveau> liste = FXCollections.observableArrayList();
                    liste.add(null);
                    if (niveaux != null) liste.addAll(niveaux);

                    StringConverter<Niveau> conv = converterNiveau();
                    filtreNiveau.setConverter(conv);
                    filtreNiveau.setButtonCell(boutonCellule(conv));
                    filtreNiveau.setItems(liste);
                    filtreNiveau.getSelectionModel().selectFirst();
                }),
                err -> Platform.runLater(() ->
                        System.err.println("❌ [Niveaux] : " + (err != null ? err.getMessage() : "?"))));

        /* ---------- 3. FILIERES ---------- */
        academiqueService.listerFilieresParEcoleSansJetonAsync(idEcoleCourante,
                filieres -> Platform.runLater(() -> {
                    ObservableList<Filiere> liste = FXCollections.observableArrayList();
                    liste.add(null);
                    if (filieres != null) liste.addAll(filieres);

                    StringConverter<Filiere> conv = converterFiliere();
                    filtreFiliere.setConverter(conv);
                    filtreFiliere.setButtonCell(boutonCellule(conv));
                    filtreFiliere.setItems(liste);
                    filtreFiliere.getSelectionModel().selectFirst();
                }),
                err -> Platform.runLater(() ->
                        System.err.println("❌ [Filières] : " + (err != null ? err.getMessage() : "?"))));
    }

    /* ============================================================
       APPLIQUER UNE ANNÉE (avec vérification si année courante)
       ============================================================ */
    private void appliquerAnnee(AnneeScolaire anneeSelectionnee, boolean syncCombo) {
        if (anneeSelectionnee == null) {
            afficherAucuneAnneeActive("Aucune année scolaire sélectionnée.");
            return;
        }

        String idAnnee = anneeSelectionnee.getId();
        boolean anneeChangee = !Objects.equals(this.idAnneeCourante, idAnnee);
        this.idAnneeCourante = idAnnee;

        // Vérification : L'année est-elle l'année courante globale ?
        AnneeScolaire anneeGlobal = SessionManager.getInstance().getAnneeActive();
        boolean estAnneeCouranteGlobale = (anneeGlobal != null && Objects.equals(anneeGlobal.getId(), idAnnee));
        boolean estStatutEnCours = (anneeSelectionnee.getStatut() == Statut.EN_COURS);

        // Mode consultation si l'année choisie n'est pas l'année active globale ou si elle n'est pas EN_COURS
        this.modeConsultation = !(estAnneeCouranteGlobale && estStatutEnCours);

        String baseLabel = "Année scolaire : " + (anneeSelectionnee.getLibelle() != null ? anneeSelectionnee.getLibelle() : "—");

        if (!this.modeConsultation) {
            lblAnneeContexte.setText(baseLabel);
            lblAnneeContexte.setStyle("-fx-text-fill: #F5A524; -fx-font-weight: 600;");
        } else {
            String motif = !estAnneeCouranteGlobale ? "Année non courante" : anneeSelectionnee.getStatut().getLibelle();
            lblAnneeContexte.setText(baseLabel + " — Consultation (" + motif + ")");
            lblAnneeContexte.setStyle("-fx-text-fill: #E5484D; -fx-font-weight: 600;");
        }

        if (syncCombo && filtreAnnee.getItems() != null) {
            for (AnneeScolaire a : filtreAnnee.getItems()) {
                if (a != null && a.getId().equals(idAnnee)) {
                    filtreAnnee.getSelectionModel().select(a);
                    break;
                }
            }
        }

        appliquerControlesAcces();

        if (anneeChangee || !chargementEnCours) {
            lancerRecherche();
        }
    }

    /* ============================================================
       AUCUNE ANNÉE ACTIVE
       ============================================================ */
    private void afficherAucuneAnneeActive(String message) {
        this.idAnneeCourante = null;
        this.modeConsultation = true;

        lblAnneeContexte.setText(message);
        lblAnneeContexte.setStyle("-fx-text-fill: #E5484D; -fx-font-weight: 600;");

        tableClasses.setPlaceholder(new Label(message));
        tableClasses.getItems().clear();
        lblFooterClasses.setText("0 classe");

        appliquerControlesAcces();
    }

    /* ============================================================
       AUTO-REFRESH
       ============================================================ */
    private void demarrerRafraichissementAuto() {
        if (autoRefresh != null && autoRefresh.isRunning()) {
            autoRefresh.restartNow();
            return;
        }

        autoRefresh = new AutoRefreshService<List<Classe>>(PERIODE_REFRESH)
                .loader(this::chargerClassesPourRefresh)
                .onSuccess(this::appliquerResultatRefresh)
                .onFailure(err -> System.err.println("Auto-refresh classes : " + err.getMessage()))
                .start();
    }

    private List<Classe> chargerClassesPourRefresh() {
        if (chargementEnCours || idAnneeCourante == null) return null;

        final String idNiveau = getSelectedNiveauId();
        final String terme    = getSearchText();

        List<Classe> resultats = service.rechercher(idEcoleCourante, idAnneeCourante, idNiveau, terme);
        return filtrerListeLocalement(resultats);
    }

    private void appliquerResultatRefresh(List<Classe> classes) {
        if (classes == null || idAnneeCourante == null) return;

        gestionnairePagination.mettreAJourDonnees(classes);
        tableClasses.setPlaceholder(new Label("Aucune classe enregistrée pour cette année"));
        mettreAJourFooter(classes.size());
        tableClasses.refresh();
    }

    /* ============================================================
       TABLE
       ============================================================ */
    private void configurerTable() {
        colClasseNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNomClasse()));

        colClasseNiveau.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getNomNiveau() != null
                                ? d.getValue().getNomNiveau()
                                : d.getValue().getIdNiveau()));

        colClasseFiliere.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getCodeFiliere() != null
                                ? d.getValue().getCodeFiliere() : "—"));

        colClasseVacation.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colClasseVacation.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Classe classe, boolean vide) {
                super.updateItem(classe, vide);
                if (vide || classe == null || classe.getVacation() == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(classe.getVacation().getLibelle());
                badge.getStyleClass().addAll("event-tag", styleClassFor(classe.getVacation()));
                setGraphic(badge);
            }
        });

        colClasseSalle.setCellValueFactory(d -> {
            Classe c = d.getValue();
            String affichage = (c.getNomSalle() != null && !c.getNomSalle().isBlank()) ? c.getNomSalle()
                    : (c.getIdSalle() != null && !c.getIdSalle().isBlank()) ? c.getIdSalle() : "-";
            return new SimpleStringProperty(affichage);
        });

        colClasseTitulaire.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getIdEnseignantTitulaire() != null
                                ? d.getValue().getIdEnseignantTitulaire() : "—"));

        colClasseEffectif.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colClasseEffectif.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Classe classe, boolean vide) {
                super.updateItem(classe, vide);
                if (vide || classe == null) {
                    setText(null);
                    return;
                }
                setText(classe.getEffectifActuel() + " / " + classe.getCapacite());
            }
        });

        colClasseActions.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colClasseActions.setCellFactory(c -> new TableCell<>() {
            private final Button btnVoir      = new Button();
            private final Button btnEdit      = new Button();
            private final Button btnSupprimer = new Button();
            private final HBox conteneur      = new HBox(6, btnVoir, btnEdit, btnSupprimer);

            {
                conteneur.setAlignment(Pos.CENTER);

                FontIcon iconVoir = new FontIcon("fth-eye");
                iconVoir.setIconSize(14);
                btnVoir.setGraphic(iconVoir);
                btnVoir.getStyleClass().add("btn-icon-light");
                btnVoir.setTooltip(new Tooltip("Voir"));

                FontIcon iconEdit = new FontIcon("fth-edit-2");
                iconEdit.setIconSize(14);
                btnEdit.setGraphic(iconEdit);
                btnEdit.getStyleClass().add("btn-icon-light");
                btnEdit.setTooltip(new Tooltip("Modifier"));

                FontIcon iconDelete = new FontIcon("fth-trash-2");
                iconDelete.setIconSize(14);
                btnSupprimer.setGraphic(iconDelete);
                btnSupprimer.getStyleClass().addAll("btn-icon-light", "btn-danger-icon");
                btnSupprimer.setTooltip(new Tooltip("Supprimer"));

                btnVoir.setOnAction(ev -> {
                    Classe classe = getItem();
                    if (classe != null) ouvrirModalEdition(classe);
                });
                btnEdit.setOnAction(ev -> {
                    Classe classe = getItem();
                    if (classe != null) ouvrirModalEdition(classe);
                });
                btnSupprimer.setOnAction(ev -> {
                    Classe classe = getItem();
                    if (classe != null) supprimerClasse(classe);
                });
            }

            @Override
            protected void updateItem(Classe classe, boolean vide) {
                super.updateItem(classe, vide);
                if (vide || classe == null) {
                    setGraphic(null);
                    return;
                }

                if (modeConsultation) {
                    btnVoir.setVisible(true);
                    btnVoir.setManaged(true);
                    btnEdit.setVisible(false);
                    btnEdit.setManaged(false);
                    btnSupprimer.setVisible(false);
                    btnSupprimer.setManaged(false);
                } else {
                    btnVoir.setVisible(true);
                    btnVoir.setManaged(true);
                    btnEdit.setVisible(true);
                    btnEdit.setManaged(true);
                    btnSupprimer.setVisible(true);
                    btnSupprimer.setManaged(true);
                }

                setGraphic(conteneur);
            }
        });
    }

    /* ============================================================
       FILTRES
       ============================================================ */
    private void configurerFiltres() {
        ObservableList<Classe.Vacation> vacations = FXCollections.observableArrayList();
        vacations.add(null);
        vacations.addAll(Classe.Vacation.values());

        StringConverter<Classe.Vacation> convVac = new StringConverter<>() {
            @Override public String toString(Classe.Vacation v) {
                return v == null ? "Toutes les vacations" : v.getLibelle();
            }
            @Override public Classe.Vacation fromString(String s) { return null; }
        };
        filtreVacation.setConverter(convVac);
        filtreVacation.setButtonCell(boutonCellule(convVac));
        filtreVacation.setItems(vacations);
        filtreVacation.getSelectionModel().selectFirst();

        filtreAnnee.getSelectionModel().selectedItemProperty()
                .addListener((obs, anc, nouv) -> {
                    if (nouv == null) return;
                    anneeImposeeParAppelant = true;
                    appliquerAnnee(nouv, false);
                });

        filtreNiveau.getSelectionModel().selectedItemProperty().addListener((obs, anc, nouv) -> lancerRecherche());
        filtreFiliere.getSelectionModel().selectedItemProperty().addListener((obs, anc, nouv) -> lancerRecherche());
        filtreVacation.getSelectionModel().selectedItemProperty().addListener((obs, anc, nouv) -> lancerRecherche());
    }

    /* ============================================================
       RECHERCHE ET FILTRAGE
       ============================================================ */
    private void configurerRecherche() {
        debounceRecherche.setOnFinished(ev -> lancerRecherche());
        fieldRecherche.textProperty().addListener((obs, anc, nouv) -> debounceRecherche.playFromStart());
    }

    private void lancerRecherche() {
        if (idEcoleCourante == null) return;

        if (idAnneeCourante == null) {
            afficherAucuneAnneeActive("Aucune année scolaire active.");
            return;
        }

        String idNiveau = getSelectedNiveauId();
        String terme    = getSearchText();

        tableClasses.setPlaceholder(new Label("Chargement…"));
        chargementEnCours = true;

        service.rechercherAsync(idEcoleCourante, idAnneeCourante,
                idNiveau, terme,
                classes -> Platform.runLater(() -> {
                    chargementEnCours = false;
                    List<Classe> listeFiltruee = filtrerListeLocalement(classes);

                    gestionnairePagination.mettreAJourDonnees(listeFiltruee);
                    tableClasses.setPlaceholder(new Label("Aucune classe enregistrée pour cette année"));
                    mettreAJourFooter(listeFiltruee.size());

                    if (autoRefresh != null && autoRefresh.isRunning()) {
                        autoRefresh.restartNow();
                    }
                }),
                erreur -> Platform.runLater(() -> {
                    chargementEnCours = false;
                    tableClasses.setPlaceholder(new Label("Erreur de chargement"));
                    afficherErreurDialogue(erreur);
                }));
    }

    /**
     * Filtrage local robuste selon la Filière (par ID ou Code) et la Vacation.
     */
    private List<Classe> filtrerListeLocalement(List<Classe> liste) {
        if (liste == null || liste.isEmpty()) return List.of();

        Filiere filiereSel = filtreFiliere.getSelectionModel().getSelectedItem();
        Classe.Vacation vacationSel = filtreVacation.getSelectionModel().getSelectedItem();

        return liste.stream()
                .filter(c -> {
                    // Filtrage Filière (Vérification double : ID ou Code)
                    if (filiereSel != null) {
                        boolean matchId = filiereSel.getId() != null && Objects.equals(filiereSel.getId(), c.getIdFiliere());
                        boolean matchCode = filiereSel.getCodeFiliere() != null && Objects.equals(filiereSel.getCodeFiliere(), c.getCodeFiliere());
                        if (!matchId && !matchCode) {
                            return false;
                        }
                    }
                    // Filtrage Vacation
                    if (vacationSel != null) {
                        if (c.getVacation() != vacationSel) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();
    }

    private String getSelectedNiveauId() {
        Niveau n = filtreNiveau.getSelectionModel().getSelectedItem();
        return n != null ? n.getId() : null;
    }

    private String getSearchText() {
        String t = fieldRecherche.getText();
        return (t == null || t.isBlank()) ? null : t.trim();
    }

    private void mettreAJourFooter(int total) {
        lblFooterClasses.setText(total + " classe" + (total > 1 ? "s" : ""));
    }

    /* ============================================================
       BOUTONS + CONTROLES D'ACCES
       ============================================================ */
    private void configurerBoutons() {
        btnNouvelleClasse.setOnAction(ev -> onNouvelleClasse());
        btnCreationMultiple.setOnAction(ev -> onCreationMultiple());
    }

    private void appliquerControlesAcces() {
        boolean anneeActive = (idAnneeCourante != null);
        boolean verrouiller = !anneeActive || modeConsultation;

        btnNouvelleClasse.setDisable(verrouiller);
        btnCreationMultiple.setDisable(verrouiller);

        if (tableClasses != null) tableClasses.refresh();
    }

    /* ============================================================
       ACTIONS CRUD
       ============================================================ */
    @FXML
    private void onNouvelleClasse() {
        if (idAnneeCourante == null) {
            ToastNotification.erreur(fenetre(), "Aucune année scolaire active.");
            return;
        }
        if (modeConsultation) {
            ToastNotification.avertissement(fenetre(), "Cette année est en mode consultation seule. Impossible de créer une classe.");
            return;
        }
        ouvrirModalCreation();
    }

    private void onCreationMultiple() {
        if (idAnneeCourante == null) {
            ToastNotification.erreur(fenetre(), "Aucune année scolaire active.");
            return;
        }
        if (modeConsultation) {
            ToastNotification.avertissement(fenetre(), "Cette année est en mode consultation seule. Impossible de créer des classes.");
            return;
        }

        String fxml = "/org/erpklassup/erpklassup/view/module2/GenereClasse.fxml";
        CreationMultipleClassesController ctrl = ModalUtil.ouvrirModal(
                getClass(), fxml, "Création multiple de classes",
                fenetre(), 820, 720);

        if (ctrl != null) {
            ctrl.initData(idEcoleCourante, idAnneeCourante, this::lancerRecherche);
        }
    }

    private void ouvrirModalCreation() {
        String fxml = "/org/erpklassup/erpklassup/view/module2/ClasseModal.fxml";
        ClasseModalController ctrl = ModalUtil.ouvrirModal(
                getClass(), fxml, "Nouvelle classe",
                fenetre(), 540, 650);

        if (ctrl != null) {
            ctrl.initData(idEcoleCourante, idAnneeCourante, null, this::lancerRecherche);
        }
    }

    private void ouvrirModalEdition(Classe classe) {
        if (modeConsultation) {
            ToastNotification.avertissement(fenetre(), "Cette année est en consultation seule.");
            return;
        }

        String fxml = "/org/erpklassup/erpklassup/view/module2/ClasseModal.fxml";
        ClasseModalController ctrl = ModalUtil.ouvrirModal(
                getClass(), fxml, "Modifier la classe",
                fenetre(), 540, 650);

        if (ctrl != null) {
            ctrl.initData(idEcoleCourante, idAnneeCourante, classe, this::lancerRecherche);
        }
    }

    private void supprimerClasse(Classe classe) {
        if (modeConsultation) {
            ToastNotification.avertissement(fenetre(), "Cette année est en consultation seule. Impossible de supprimer.");
            return;
        }

        Boolean confirmation = AlertUtil.afficherConfirmation(
                "Supprimer la classe",
                "Voulez-vous vraiment supprimer \"" + classe.getNomClasse() + "\" ?",
                fenetre());

        if (Boolean.TRUE.equals(confirmation)) {
            service.supprimerAsync(classe.getId(),
                    () -> Platform.runLater(() -> {
                        ToastNotification.succes(fenetre(), "Classe \"" + classe.getNomClasse() + "\" supprimée.");
                        lancerRecherche();
                    }),
                    erreur -> Platform.runLater(() ->
                            ToastNotification.erreur(fenetre(),
                                    erreur.getMessage() != null ? erreur.getMessage() : "Échec de la suppression.")));
        }
    }

    /* ============================================================
       ERREURS ET UTILITAIRES
       ============================================================ */
    private void afficherErreurDialogue(Throwable erreur) {
        new Alert(Alert.AlertType.ERROR,
                erreur != null && erreur.getMessage() != null
                        ? erreur.getMessage()
                        : "Une erreur est survenue.").showAndWait();
    }

    private Stage fenetre() {
        return (Stage) tableClasses.getScene().getWindow();
    }

    private <T> ListCell<T> boutonCellule(StringConverter<T> converter) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : converter.toString(item));
            }
        };
    }

    private StringConverter<AnneeScolaire> converterAnnee() {
        return new StringConverter<>() {
            @Override public String toString(AnneeScolaire a) {
                if (a == null) return "—";
                String libelle = a.getLibelle();
                if (a.getStatut() != null && a.getStatut() != Statut.EN_COURS) {
                    return libelle + " (" + a.getStatut().getLibelle() + ")";
                }
                return libelle;
            }
            @Override public AnneeScolaire fromString(String s) { return null; }
        };
    }

    private StringConverter<Niveau> converterNiveau() {
        return new StringConverter<>() {
            @Override
            public String toString(Niveau n) {
                if (n == null) return "Tous les niveaux";
                return n.getNomNiveau() != null ? n.getNomNiveau() : "Niveau sans nom";
            }
            @Override public Niveau fromString(String s) { return null; }
        };
    }

    private StringConverter<Filiere> converterFiliere() {
        return new StringConverter<>() {
            @Override public String toString(Filiere f) {
                return f == null ? "Toutes les filières" : f.getCodeFiliere();
            }
            @Override public Filiere fromString(String s) { return null; }
        };
    }

    private String styleClassFor(Classe.Vacation v) {
        return switch (v) {
            case MATIN -> "badge-succes";
            case APRES_MIDI -> "badge-info";
            case SOIR -> "badge-warning";
            case JOURNEE_CONTINUE -> "badge-neutre";
            case WEEKEND -> "badge-danger";
        };
    }
}