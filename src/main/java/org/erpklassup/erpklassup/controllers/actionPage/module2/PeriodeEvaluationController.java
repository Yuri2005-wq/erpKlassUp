package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.erpklassup.erpklassup.models.module2.AnneeScolaire;
import org.erpklassup.erpklassup.models.module2.AnneeScolaire.Statut;
import org.erpklassup.erpklassup.models.module2.PeriodeEvaluation;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.module2.PeriodeEvaluationService;
import org.erpklassup.erpklassup.util.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class PeriodeEvaluationController implements Initializable, VueDisposable {

    /* ═══════════════ FXML ═══════════════ */
    @FXML private StackPane rootStack;
    @FXML private Label lblResume;
    @FXML private Label lblAnneeContexte;
    @FXML private Label lblModeConsultation;
    @FXML private FontIcon icoMode;
    @FXML private Button btnGenererStructure;
    @FXML private Button btnNouvellePeriode;
    @FXML private Button btnEmptyGenerer;
    @FXML private Button btnEmptyCreer;
    @FXML private ScrollPane scrollTimeline;
    @FXML private VBox timelineContainer;
    @FXML private VBox emptyStateBox;

    /* ═══════════════ SERVICES & ÉTAT ═══════════════ */
    private static final Duration PERIODE_REFRESH = Duration.seconds(15);

    private final PeriodeEvaluationService service = new PeriodeEvaluationService();

    private String  idEcoleCourante;
    private String  idAnneeCourante;
    private boolean modeConsultation = false;

    private AutoRefreshService<List<PeriodeEvaluation>> autoRefresh;
    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;
    private final Runnable ecouteurAnnee       = this::surChangementAnneeGlobal;

    /* ═══════════════ INITIALISATION ═══════════════ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();

        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
        SessionManager.getInstance().ecouterChangementAnnee(ecouteurAnnee);

        appliquerControlesAcces();

        if (idEcoleCourante == null) {
            afficherAucuneAnneeActive("Aucune école active — connectez-vous à une école.");
            return;
        }

        synchroniserAvecAnneeGlobaleSession();
        demarrerRafraichissementAuto();
    }

    @Override
    public void disposer() {
        if (autoRefresh != null) { autoRefresh.dispose(); autoRefresh = null; }
        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
        SessionManager.getInstance().arreterEcouteAnnee(ecouteurAnnee);
    }

    /* ═══════════════ ANNÉE ═══════════════ */
    private void synchroniserAvecAnneeGlobaleSession() {
        AnneeScolaire a = SessionManager.getInstance().getAnneeActive();
        if (a != null) appliquerAnnee(a);
        else afficherAucuneAnneeActive("Aucune année scolaire active dans le système.");
    }

    private void surChangementAnneeGlobal() {
        AnneeScolaire a = SessionManager.getInstance().getAnneeActive();
        if (a != null) appliquerAnnee(a);
        else afficherAucuneAnneeActive("Aucune année scolaire active dans le système.");
    }

    private void appliquerAnnee(AnneeScolaire annee) {
        this.idAnneeCourante = annee.getId();

        AnneeScolaire global = SessionManager.getInstance().getAnneeActive();
        boolean courante = global != null && Objects.equals(global.getId(), annee.getId());
        boolean enCours  = annee.getStatut() == Statut.EN_COURS;
        this.modeConsultation = !(courante && enCours);

        lblAnneeContexte.setText("Année : " + (annee.getLibelle() != null ? annee.getLibelle() : "—"));
        if (modeConsultation) {
            icoMode.setIconLiteral("fth-lock");
            icoMode.setStyle("-fx-icon-size: 12px; -fx-icon-color: #E5484D;");
            lblModeConsultation.setText("Consultation seule");
            lblModeConsultation.setStyle("-fx-font-size: 11px; -fx-text-fill: #E5484D; -fx-font-weight: 600;");
        } else {
             icoMode.setIconLiteral("fth-edit-3");
            icoMode.getStyleClass().add("icon-green");
            lblModeConsultation.setText("Édition autorisée");
            lblModeConsultation.setStyle("-fx-font-size: 11px; -fx-text-fill: #15803D; -fx-font-weight: 600;");
        }

        appliquerControlesAcces();
        chargerEtAfficher();
    }

    private void afficherAucuneAnneeActive(String msg) {
        idAnneeCourante = null;
        modeConsultation = true;
        lblAnneeContexte.setText(msg);
        icoMode.setIconLiteral("fth-alert-circle");
        icoMode.setStyle("-fx-icon-size: 12px; -fx-icon-color: #E5484D;");
        lblModeConsultation.setText("Aucune année active");
        lblModeConsultation.setStyle("-fx-font-size: 11px; -fx-text-fill: #E5484D;");
        lblResume.setText("—");
        timelineContainer.getChildren().clear();
        emptyStateBox.setVisible(true);
        emptyStateBox.setManaged(true);
        appliquerControlesAcces();
    }

    /* ═══════════════ CHARGEMENT & TIMELINE ═══════════════ */
    private void chargerEtAfficher() {
        if (idAnneeCourante == null) return;
        service.chargerArborescenceAsync(idAnneeCourante,
                liste -> Platform.runLater(() -> construireTimeline(liste)),
                err -> Platform.runLater(() -> {
                    ToastNotification.erreur(fenetre(), "Erreur : " + err.getMessage());
                    construireTimeline(null);
                }));
    }

    private void construireTimeline(List<PeriodeEvaluation> racines) {
        timelineContainer.getChildren().clear();

        boolean vide = racines == null || racines.isEmpty();
        emptyStateBox.setVisible(vide);
        emptyStateBox.setManaged(vide);

        if (vide) {
            lblResume.setText("Aucune période définie");
            return;
        }

        int nbTrimestres = racines.size();
        int nbSequences = racines.stream()
                .mapToInt(r -> r.getSousPeriodes() == null ? 0 : r.getSousPeriodes().size())
                .sum();
        lblResume.setText(nbTrimestres + " période(s) racine · " + nbSequences + " sous-période(s)");

        for (PeriodeEvaluation trimestre : racines) {
            timelineContainer.getChildren().add(creerPeriodeCard(trimestre));
        }

        timelineContainer.getChildren().add(creerBoutonNouvelleRacine());
    }

    /* ═══════════════ CARTE PÉRIODE ═══════════════ */
    private VBox creerPeriodeCard(PeriodeEvaluation periode) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #E4E4E7; -fx-border-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        /* ---- Header ---- */
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setStyle("-fx-border-color: transparent transparent #F4F4F5 transparent; -fx-border-width: 0 0 1 0;");

        Region accent = new Region();
        accent.setPrefWidth(4);
        accent.setPrefHeight(38);
        accent.setStyle("-fx-background-color: " + couleurAccent(periode) + "; -fx-background-radius: 2;");

        VBox titres = new VBox(2);
        Label nom = new Label(periode.getLibelle());
        nom.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #18181B;");
        Label plage = new Label(formatPlage(periode.getDateDebut(), periode.getDateFin()));
        plage.setStyle("-fx-font-size: 11px; -fx-text-fill: #71717A;");
        titres.getChildren().addAll(nom, plage);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox badge = creerBadgeStatut(periode);

        Button btnEdit = new Button();
        btnEdit.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6;");
        FontIcon icoEdit = new FontIcon("fth-edit-3");
        icoEdit.setIconSize(14);
        icoEdit.setIconColor(javafx.scene.paint.Color.web("#71717A"));
        btnEdit.setGraphic(icoEdit);
        btnEdit.setTooltip(new Tooltip("Modifier"));
        btnEdit.setOnAction(e -> ouvrirModalEdition(periode));

        header.getChildren().addAll(accent, titres, spacer, badge, btnEdit);
        card.getChildren().add(header);

        /* ---- Sous-périodes ---- */
        VBox seqBox = new VBox(0);
        seqBox.setPadding(new Insets(6, 0, 6, 0));

        List<PeriodeEvaluation> sous = periode.getSousPeriodes();
        if (sous != null && !sous.isEmpty()) {
            for (PeriodeEvaluation s : sous) {
                seqBox.getChildren().add(creerLigneSousPeriode(s));
            }
        } else {
            Label vide = new Label("Aucune sous-période définie");
            vide.setStyle("-fx-font-size: 12px; -fx-text-fill: #A1A1AA; -fx-font-style: italic;");
            VBox.setMargin(vide, new Insets(14, 0, 14, 60));
            seqBox.getChildren().add(vide);
        }

        Button btnAjouter = new Button("Ajouter une sous-période");
        FontIcon icoPlus = new FontIcon("fth-plus-circle");
        icoPlus.setIconSize(12);
        btnAjouter.setGraphic(icoPlus);
        btnAjouter.getStyleClass().add("btn-icon-light");
        btnAjouter.setStyle("-fx-border-style: dashed;");
        btnAjouter.setMaxWidth(Double.MAX_VALUE);
        btnAjouter.setOnAction(e -> ouvrirModalCreation(periode.getId()));
        seqBox.getChildren().add(btnAjouter);

        card.getChildren().add(seqBox);
        return card;
    }

    private HBox creerLigneSousPeriode(PeriodeEvaluation sp) {
        HBox ligne = new HBox(14);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setPadding(new Insets(10, 20, 10, 60));
        ligne.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");
        ligne.setOnMouseEntered(e -> ligne.setStyle("-fx-cursor: hand; -fx-background-color: #FAFAFA;"));
        ligne.setOnMouseExited(e  -> ligne.setStyle("-fx-cursor: hand; -fx-background-color: transparent;"));
        ligne.setOnMouseClicked(e -> ouvrirModalEdition(sp));

        Region ligneConnexion = new Region();
        ligneConnexion.setPrefWidth(2);
        ligneConnexion.setPrefHeight(20);
        ligneConnexion.setStyle("-fx-background-color: #E4E4E7;");

        Label nom = new Label(sp.getLibelle());
        nom.setStyle("-fx-font-size: 13px; -fx-text-fill: #3F3F46;");
        nom.setPrefWidth(180);

        Label dates = new Label(formatPlage(sp.getDateDebut(), sp.getDateFin()));
        dates.setStyle("-fx-font-size: 11px; -fx-text-fill: #71717A;");
        dates.setPrefWidth(170);

        HBox poidsBox = new HBox(4);
        poidsBox.setAlignment(Pos.CENTER_LEFT);
        poidsBox.setPrefWidth(80);
        FontIcon icoPoids = new FontIcon("fth-bar-chart-2");
        icoPoids.setIconSize(11);
        icoPoids.setIconColor(javafx.scene.paint.Color.web("#A1A1AA"));
        Label poidsLbl = new Label(sp.getPoids() != null ? sp.getPoids().toPlainString() : "1.00");
        poidsLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #A1A1AA;");
        poidsBox.getChildren().addAll(icoPoids, poidsLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox badge = creerBadgeStatut(sp);

        ligne.getChildren().addAll(ligneConnexion, nom, dates, poidsBox, spacer, badge);
        return ligne;
    }

    /* ═══════════════ BADGE STATUT ═══════════════ */
    private HBox creerBadgeStatut(PeriodeEvaluation p) {
        String texte, bg, fg, iconLit;

        if (p.isEstCloturee()) {
            texte = "Clôturé"; bg = "#FEE2E2"; fg = "#E5484D"; iconLit = "fth-lock";
        } else if (estEnCours(p)) {
            texte = "En cours"; bg = "#DCFCE7"; fg = "#16A34A"; iconLit = "fth-play";
        } else if (estAVenir(p)) {
            texte = "À venir"; bg = "#FEF3E2"; fg = "#F5A524"; iconLit = "fth-clock";
        } else {
            texte = "Ouvert"; bg = "#DCFCE7"; fg = "#15803D"; iconLit = "fth-unlock";
        }

        FontIcon ico = new FontIcon(iconLit);
        ico.setIconSize(11);
        ico.setIconColor(javafx.scene.paint.Color.web(fg));

        Label lbl = new Label(texte);
        lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: " + fg + ";");

        HBox badge = new HBox(5, ico, lbl);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(4, 10, 4, 10));
        badge.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 12;");
        return badge;
    }

    private boolean estEnCours(PeriodeEvaluation p) {
        if (p.getDateDebut() == null || p.getDateFin() == null) return false;
        LocalDate now = LocalDate.now();
        return !now.isBefore(p.getDateDebut()) && !now.isAfter(p.getDateFin());
    }

    private boolean estAVenir(PeriodeEvaluation p) {
        return p.getDateDebut() != null && LocalDate.now().isBefore(p.getDateDebut());
    }

    /* ═══════════════ OUVERTURE MODAL ═══════════════ */
    @FXML
    public void onNouvellePeriodeRacine() {
        if (modeConsultation) {
            ToastNotification.avertissement(fenetre(), "Mode consultation seule.");
            return;
        }
        ouvrirModalCreation(null);
    }

    private void ouvrirModalCreation(String idParent) {
        String fxml = "/org/erpklassup/erpklassup/view/module2/ModalPeriode.fxml";
        PeriodeModalController ctrl = ModalUtil.ouvrirModal(
                getClass(), fxml,
                idParent == null ? "Nouvelle période" : "Nouvelle sous-période",
                fenetre(), 560, 680);

        if (ctrl != null) {
            ctrl.initData(idEcoleCourante, idAnneeCourante, null, idParent,
                    this::chargerEtAfficher);
        }
    }

    private void ouvrirModalEdition(PeriodeEvaluation periode) {
        if (modeConsultation) {
            ToastNotification.avertissement(fenetre(), "Mode consultation seule.");
            return;
        }
        String fxml = "/org/erpklassup/erpklassup/view/module2/ModalPeriode.fxml";
        PeriodeModalController ctrl = ModalUtil.ouvrirModal(
                getClass(), fxml, "Modifier la période",
                fenetre(), 560, 680);

        if (ctrl != null) {
            ctrl.initData(idEcoleCourante, idAnneeCourante, periode, null,
                    this::chargerEtAfficher);
        }
    }

    /* ═══════════════ STRUCTURE STANDARD ═══════════════ */
    @FXML
//    public void onGenererStructureStandard() {
//        if (modeConsultation) return;
//        AnneeScolaire a = SessionManager.getInstance().getAnneeActive();
//        if (a == null || a.getDateDebut() == null || a.getDateFin() == null) {
//            ToastNotification.erreur(fenetre(), "Dates de l'année manquantes.");
//            return;
//        }
//        service.genererStructureStandardAsync(idEcoleCourante, idAnneeCourante,
//                a.getDateDebut(), a.getDateFin(),
//                PeriodeEvaluationService.ModeGeneration.TRIMESTRES_SEQUENCES,
//                () -> Platform.runLater(() -> {
//                    ToastNotification.succes(fenetre(), "Structure générée.");
//                    chargerEtAfficher();
//                }),
//                err -> Platform.runLater(() -> ToastNotification.erreur(fenetre(), err.getMessage())));
//    }
    private void onGenererStructureStandard() {
        if (idAnneeCourante == null) {
            ToastNotification.erreur(fenetre(), "Sélectionnez d'abord une année scolaire.");
            return;
        }

        String fxml = "/org/erpklassup/erpklassup/view/module2/GenerePeriode.fxml";

        GenererPeriodesModalController ctrl = ModalUtil.ouvrirModal(
                getClass(), fxml, "Générer la structure",
                fenetre(), 520, 460);

        if (ctrl != null) {
            ctrl.initData(idEcoleCourante, idAnneeCourante, this::chargerEtAfficher);
        }
    }

    /* ═══════════════ AUTO-REFRESH ═══════════════ */
    private void demarrerRafraichissementAuto() {
        if (autoRefresh != null && autoRefresh.isRunning()) {
            autoRefresh.restartNow();
            return;
        }
        autoRefresh = new AutoRefreshService<List<PeriodeEvaluation>>(PERIODE_REFRESH)
                .loader(() -> (idAnneeCourante != null)
                        ? service.chargerArborescence(idAnneeCourante) : null)
                .onSuccess(liste -> { if (liste != null) construireTimeline(liste); })
                .onFailure(err -> System.err.println("Auto-refresh : " + err.getMessage()))
                .start();
    }

    /* ═══════════════ ACCÈS & UTILITAIRES ═══════════════ */
    private void appliquerControlesAcces() {
        boolean lock = (idAnneeCourante == null) || modeConsultation;
        btnNouvellePeriode.setDisable(lock);
        btnGenererStructure.setDisable(lock);
        if (btnEmptyCreer != null) btnEmptyCreer.setDisable(lock);
        if (btnEmptyGenerer != null) btnEmptyGenerer.setDisable(lock);
    }

    private Button creerBoutonNouvelleRacine() {
        Button b = new Button("Nouvelle période racine");
        FontIcon ico = new FontIcon("fth-plus-circle");
        ico.setIconSize(14);
        b.setGraphic(ico);
        b.getStyleClass().add("button-primary");
        b.setStyle(
                "-fx-border-color: #E4E4E7; -fx-border-style: dashed; "
                + "-fx-background-radius: 10;");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> onNouvellePeriodeRacine());
        return b;
    }

    private String couleurAccent(PeriodeEvaluation p) {
        if (p.isEstCloturee()) return "#A1A1AA";
        if (estEnCours(p))      return "#3B82F6";
        if (estAVenir(p))       return "#F59E0B";
        return "#10B981";
    }

    private String formatPlage(LocalDate d1, LocalDate d2) {
        if (d1 == null || d2 == null) return "Dates non définies";
        return String.format("%1$td/%1$tm/%1$tY → %2$td/%2$tm/%2$tY", d1, d2);
    }

    private Stage fenetre() {
        return (Stage) rootStack.getScene().getWindow();
    }
}



























//package org.erpklassup.erpklassup.controllers.actionPage.module2;
//
//import javafx.application.Platform;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.scene.control.*;
//import javafx.scene.layout.HBox;
//import javafx.scene.shape.Circle;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//import javafx.util.StringConverter;
//
//import org.erpklassup.erpklassup.models.module2.AnneeScolaire;
//import org.erpklassup.erpklassup.models.module2.PeriodeEvaluation;
//import org.erpklassup.erpklassup.service.SessionManager;
//import org.erpklassup.erpklassup.service.module2.AnneeScolaireService;
//import org.erpklassup.erpklassup.service.module2.PeriodeEvaluationService;
//import org.erpklassup.erpklassup.util.AlertUtil;
//import org.erpklassup.erpklassup.util.AutoRefreshService;
//import org.erpklassup.erpklassup.util.ModalUtil;
//import org.erpklassup.erpklassup.util.ToastNotification;
//import org.erpklassup.erpklassup.util.VueDisposable;
//import org.kordamp.ikonli.javafx.FontIcon;
//
//import java.math.BigDecimal;
//import java.net.URL;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.time.format.DateTimeParseException;
//import java.util.List;
//import java.util.Optional;
//import java.util.ResourceBundle;
//import java.util.UUID;
//import java.util.function.Consumer;
//
//import static org.erpklassup.erpklassup.util.ValidationUtil.*;
//
///**
// * Controller de l'écran "Périodes d'évaluation".
// *
// * <p>Autonome : charge l'année scolaire courante de l'école via
// * {@link AnneeScolaireService#chargerCouranteAsync}. Un écran parent peut
// * imposer une année précise via {@link #initData(String, String)}.</p>
// *
// * <p>Rafraîchissement automatique toutes les 15 s pour refléter les
// * modifications faites depuis un autre poste (clôture, création, etc.).</p>
// */
//public class PeriodeEvaluationController implements Initializable, VueDisposable {
//
//    /* ============================================================
//       FXML — CHAMPS
//       ============================================================ */
//    @FXML private Label  lblAnneeContexte;
//    @FXML private Button btnNouvellePeriode;
//    @FXML private Button btnGenererStructure;
//
//    @FXML private Label lblArbreVide;
//    @FXML private TreeView<PeriodeEvaluation> treePeriodes;
//
//    @FXML private Label        lblPeriodeSelectionnee;
//    @FXML private TextField    champLibelle;
//    @FXML private DatePicker   champDateDebut;
//    @FXML private DatePicker   champDateFin;
//    @FXML private TextField    champPoids;
//    @FXML private ToggleButton toggleCloturee;
//    @FXML private Button       btnSupprimer;
//    @FXML private Button       btnEnregistrer;
//
//    /* ============================================================
//       CONSTANTES
//       ============================================================ */
//    private static final Duration PERIODE_REFRESH = Duration.seconds(15);
//    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
//
//    /* ============================================================
//       SERVICES
//       ============================================================ */
//    private final PeriodeEvaluationService service      = new PeriodeEvaluationService();
//    private final AnneeScolaireService     anneeService = new AnneeScolaireService();
//
//    /* ============================================================
//       ÉTAT
//       ============================================================ */
//    private String  idEcoleCourante;
//    private String  idAnneeCourante;
//    private boolean anneeImposeeParAppelant = false;
//    private boolean chargementEnCours       = false;
//
//    private PeriodeEvaluation periodeSelectionnee;
//    private final TreeItem<PeriodeEvaluation> racineTechnique = new TreeItem<>();
//
//    private AutoRefreshService<List<PeriodeEvaluation>> autoRefresh;
//
//    /* ============================================================
//       INITIALISATION
//       ============================================================ */
//    @Override
//    public void initialize(URL url, ResourceBundle rb) {
//        idEcoleCourante = SessionManager.getInstance().getIdEcoleCourante();
//
//        // Configuration arbre
//        treePeriodes.setRoot(racineTechnique);
//        treePeriodes.setShowRoot(false);
//        treePeriodes.setCellFactory(tv -> new CelluleArbre());
//        treePeriodes.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
//            PeriodeEvaluation p = (nouveau != null) ? nouveau.getValue() : null;
//            afficherDansPanneauDetail(p);
//        });
//
//        // Toggle clôture
//        Circle cercleBille = new Circle(8);
//        cercleBille.getStyleClass().add("switch-bille");
//        toggleCloturee.setGraphic(cercleBille);
//
//        // Format des DatePicker
//        appliquerFormatDate(champDateDebut);
//        appliquerFormatDate(champDateFin);
//
//        // Validation live
//        forcerEntier(champPoids);
//        forcerCasseTitre(champLibelle);
//        configurerToggleSwitch(toggleCloturee);
//
//        // État initial
//        viderPanneauDetail();
//        btnSupprimer.setDisable(true);
//
//        // Chargement année + arbre
//        chargerAnneeCouranteParDefaut();
//        // Initialisation à l'état "aucune année active" (sera corrigé si une année est trouvée)
//        btnNouvellePeriode.setDisable(true);
//        btnGenererStructure.setDisable(true);
//
//        // Démarrage du refresh auto
//        demarrerRafraichissementAuto();
//    }
//
//    @Override
//    public void disposer() {
//        if (autoRefresh != null) {
//            autoRefresh.dispose();
//            autoRefresh = null;
//        }
//    }
//
//    /* ============================================================
//       CHARGEMENT ANNÉE
//       ============================================================ */
//    private void chargerAnneeCouranteParDefaut() {
//        if (idEcoleCourante == null) {
//            afficherAucuneAnneeActive("Aucune école active — connectez-vous à une école.");
//            return;
//        }
//
//        anneeService.chargerCouranteAsync(idEcoleCourante,
//                optAnnee -> Platform.runLater(() -> {
//                    if (anneeImposeeParAppelant) return;
//
//                    if (optAnnee.isPresent()) {
//                        AnneeScolaire annee = optAnnee.get();
//                        appliquerAnnee(annee.getId(), annee.getLibelle());
//                    } else {
//                        afficherAucuneAnneeActive(
//                                "Aucune année scolaire active. Définissez-en une dans « Années scolaires ».");
//                    }
//                }),
//                erreur -> Platform.runLater(() -> {
//                    System.err.println("❌ Erreur chargement année courante : " + erreur.getMessage());
//                    erreur.printStackTrace();
//                    afficherAucuneAnneeActive(
//                            "Erreur de chargement de l'année scolaire active.");
//                }));
//    }
//
//
//
//    /**
//     * Point d'entrée pour un parent qui veut imposer une année précise.
//     */
//    public void initData(String idAnnee, String libelleAnnee) {
//        anneeImposeeParAppelant = true;
//        appliquerAnnee(idAnnee, libelleAnnee);
//    }
//
//    private void appliquerAnnee(String idAnnee, String libelleAnnee) {
//        this.idAnneeCourante = idAnnee;
//
//        lblAnneeContexte.setText("Année scolaire : " + (libelleAnnee != null ? libelleAnnee : "-"));
//        lblAnneeContexte.setStyle("-fx-text-fill: #F5A524; -fx-font-weight: 600;");
//
//        // ✅ Réactiver les boutons
//        btnNouvellePeriode.setDisable(false);
//        btnGenererStructure.setDisable(false);
//
//        chargerArborescence();
//    }
//
//    /* ============================================================
//       AUTO-REFRESH
//       ============================================================ */
//    private void demarrerRafraichissementAuto() {
//        autoRefresh = new AutoRefreshService<List<PeriodeEvaluation>>(PERIODE_REFRESH)
//                .loader(this::chargerPeriodesPourRefresh)
//                .onSuccess(this::appliquerResultatRefresh)
//                .onFailure(err -> System.err.println("❌ Auto-refresh périodes : " + err.getMessage()))
//                .start();
//    }
//
//    /**
//     * Charge les périodes pour le refresh auto (thread background).
//     * Retourne null pour ignorer le cycle (pas d'année, action en cours).
//     */
//    private List<PeriodeEvaluation> chargerPeriodesPourRefresh() {
//        if (chargementEnCours) return null;
//
//        // 1. Vérifier si l'année courante a changé depuis le dernier cycle
//        verifierChangementAnneeCourante();
//
//        // 2. Si pas d'année, on ne peut rien charger
//        if (idAnneeCourante == null)return null;
//
//        // 3. Charger les périodes de l'année courante
//        return service.chargerArborescence(idAnneeCourante);
//    }
//
//    /**
//     * Vérifie si l'année courante a changé en base. Si oui, recharge
//     * l'arborescence pour cette nouvelle année.
//     */
//    /**
//     * Vérifie si l'année courante a changé en base.
//     *
//     * <p>Gère TROIS cas :</p>
//     * <ul>
//     *   <li><b>Désactivation</b> : une année était active, plus aucune ne l'est
//     *       → on vide l'UI et affiche un message d'erreur</li>
//     *   <li><b>Activation</b> : aucune année n'était active, une le devient
//     *       → on charge l'arborescence</li>
//     *   <li><b>Changement</b> : une autre année devient active
//     *       → on recharge tout</li>
//     * </ul>
//     */
//
//    /**
//     * Vérifie si l'année courante a changé en base de manière asynchrone.
//     *
//     * <p>Gère TROIS cas :</p>
//     * <ul>
//     *   <li><b>Désactivation</b> : une année était active, plus aucune ne l'est
//     *       → on vide l'UI et affiche un message d'erreur</li>
//     *   <li><b>Activation</b> : aucune année n'était active, une le devient
//     *       → on charge l'arborescence</li>
//     *   <li><b>Changement</b> : une autre année devient active
//     *       → on recharge tout</li>
//     * </ul>
//     */
//    private void verifierChangementAnneeCourante() {
//        if (idEcoleCourante == null) return;
//
//        anneeService.chargerCouranteAsync(idEcoleCourante,
//                opt -> Platform.runLater(() -> {
//                    String nouvelleIdAnnee = opt.map(AnneeScolaire::getId).orElse(null);
//                    String nouveauLibelle  = opt.map(AnneeScolaire::getLibelle).orElse(null);
//
//                    // Détection du changement d'état ou d'année
//                    boolean change = !java.util.Objects.equals(nouvelleIdAnnee, idAnneeCourante);
//                    if (!change) return; // Aucun changement détecté
//
//                    // Cas 1 : DÉSACTIVATION (avant : année active, maintenant : aucune)
//                    if (nouvelleIdAnnee == null) {
//                        System.out.println("🔄 Année active désactivée → UI vidée");
//
//                        this.idAnneeCourante = null;
//                        this.periodeSelectionnee = null;
//                        this.anneeImposeeParAppelant = false;
//
//                        // Mise à jour de l'affichage
//                        lblAnneeContexte.setText(
//                                "Aucune année scolaire active. Définissez-en une dans « Années scolaires ».");
//                        lblAnneeContexte.setStyle("-fx-text-fill: #E5484D; -fx-font-weight: 600;");
//
//                        // Nettoyage de l'arbre et des détails
//                        racineTechnique.getChildren().clear();
//                        afficherArbreVide("Aucune année scolaire active");
//                        viderPanneauDetail();
//
//                        // Désactivation des actions
//                        btnNouvellePeriode.setDisable(true);
//                        btnGenererStructure.setDisable(true);
//                        return;
//                    }
//
//                    // Cas 2 & 3 : ACTIVATION ou CHANGEMENT D'ANNÉE
//                    System.out.println("🔄 Année active changée : " + idAnneeCourante
//                            + " → " + nouvelleIdAnnee + " (" + nouveauLibelle + ")");
//
//                    this.idAnneeCourante = nouvelleIdAnnee;
//                    this.anneeImposeeParAppelant = false;
//
//                    // Mise à jour du label
//                    lblAnneeContexte.setText("Année scolaire : " + nouveauLibelle);
//                    lblAnneeContexte.setStyle("-fx-text-fill: #F5A524; -fx-font-weight: 600;");
//
//                    // Réinitialisation de la sélection
//                    periodeSelectionnee = null;
//                    viderPanneauDetail();
//
//                    // Réactivation des boutons
//                    btnNouvellePeriode.setDisable(false);
//                    btnGenererStructure.setDisable(false);
//
//                    // Rechargement des données pour la nouvelle année
//                    chargerArborescence();
//                }),
//                err -> System.err.println("❌ Erreur vérification année courante : " + err.getMessage())
//        );
//    }
////    private void verifierChangementAnneeCourante() {
////        if (idEcoleCourante == null) return;
////
////        try {
////            Optional<AnneeScolaire> opt = anneeService.getCourante(idEcoleCourante);
////
////            String nouvelleIdAnnee  = opt.map(AnneeScolaire::getId).orElse(null);
////            String nouveauLibelle   = opt.map(AnneeScolaire::getLibelle).orElse(null);
////
////            // ✅ Détection générique : y a-t-il un changement d'état ?
////            boolean change = !java.util.Objects.equals(nouvelleIdAnnee, idAnneeCourante);
////
////            if (!change) return;   // rien à faire, on garde l'état actuel
////
////            // Cas 1 : DÉSACTIVATION (avant : année active, maintenant : aucune)
////            if (nouvelleIdAnnee == null) {
////                System.out.println("🔄 Année active désactivée → UI vidée");
////
////                Platform.runLater(() -> {
////                    this.idAnneeCourante = null;
////                    this.periodeSelectionnee = null;
////                    this.anneeImposeeParAppelant = false;
////
////                    // Mise à jour du label
////                    lblAnneeContexte.setText(
////                            "Aucune année scolaire active. Définissez-en une dans « Années scolaires ».");
////                    lblAnneeContexte.setStyle("-fx-text-fill: #E5484D; -fx-font-weight: 600;");
////
////                    // Vidage de l'arbre
////                    racineTechnique.getChildren().clear();
////                    afficherArbreVide("Aucune année scolaire active");
////
////                    // Vidage du panneau de détail
////                    viderPanneauDetail();
////
////                    // Désactivation des boutons
////                    btnNouvellePeriode.setDisable(true);
////                    btnGenererStructure.setDisable(true);
////                });
////                return;
////            }
////
////            // Cas 2 : ACTIVATION (avant : aucune, maintenant : une)
////            // Cas 3 : CHANGEMENT (avant : A, maintenant : B)
////            System.out.println("🔄 Année active changée : " + idAnneeCourante
////                    + " → " + nouvelleIdAnnee + " (" + nouveauLibelle + ")");
////
////            Platform.runLater(() -> {
////                this.idAnneeCourante = nouvelleIdAnnee;
////                this.anneeImposeeParAppelant = false;
////
////                // Mise à jour du label
////                lblAnneeContexte.setText("Année scolaire : " + nouveauLibelle);
////                lblAnneeContexte.setStyle("-fx-text-fill: #F5A524; -fx-font-weight: 600;");
////
////                // Réinitialisation de la sélection
////                periodeSelectionnee = null;
////                viderPanneauDetail();
////
////                // Réactivation des boutons
////                btnNouvellePeriode.setDisable(false);
////                btnGenererStructure.setDisable(false);
////
////                // Rechargement de l'arbre
////                chargerArborescence();
////            });
////
////        } catch (Exception e) {
////            System.err.println("❌ Erreur vérification année courante : " + e.getMessage());
////        }
////    }
//
////    private void verifierChangementAnneeCourante() {
////        if (idEcoleCourante == null) return;
////
////        try {
////            Optional<AnneeScolaire> opt = anneeService.getCourante(idEcoleCourante);
////
////            String nouvelleIdAnnee = opt.map(AnneeScolaire::getId).orElse(null);
////
////            // Si l'année courante en base est différente de celle affichée
////            if (nouvelleIdAnnee != null && !nouvelleIdAnnee.equals(idAnneeCourante)) {
////                String nouveauLibelle = opt.get().getLibelle();
////                System.out.println("🔄 Année courante changée : " + idAnneeCourante
////                        + " → " + nouvelleIdAnnee + " (" + nouveauLibelle + ")");
////
////                // Mettre à jour côté FX thread
////                Platform.runLater(() -> {
////                    this.idAnneeCourante = nouvelleIdAnnee;
////                    this.anneeImposeeParAppelant = false;  // nouvelle année courante
////                    lblAnneeContexte.setText("Année scolaire : " + nouveauLibelle);
////                    lblAnneeContexte.setStyle("-fx-text-fill: #F5A524; -fx-font-weight: 600;");
////
////                    // Vider la sélection devenue obsolète
////                    periodeSelectionnee = null;
////                    viderPanneauDetail();
////
////                    // Recharger immédiatement l'arbre
////                    chargerArborescence();
////                });
////            }
////        } catch (Exception e) {
////            System.err.println("❌ Erreur vérification année courante : " + e.getMessage());
////        }
////    }
//
//
//
//
//    /**
//     * Affiche l'état "aucune année active" dans toute l'UI.
//     */
//    private void afficherAucuneAnneeActive(String message) {
//        this.idAnneeCourante = null;
//        this.periodeSelectionnee = null;
//
//        lblAnneeContexte.setText(message);
//        lblAnneeContexte.setStyle("-fx-text-fill: #E5484D; -fx-font-weight: 600;");
//
//        racineTechnique.getChildren().clear();
//        afficherArbreVide(message);
//
//        viderPanneauDetail();
//
//        btnNouvellePeriode.setDisable(true);
//        btnGenererStructure.setDisable(true);
//    }
//
//
//
//
//    /**
//     * Applique le résultat du refresh (thread FX). Préserve la sélection.
//     */
//    private void appliquerResultatRefresh(List<PeriodeEvaluation> racines) {
//        if (racines == null) return;
//
//        String idSelectionne = (periodeSelectionnee != null) ? periodeSelectionnee.getId() : null;
//
//        racineTechnique.getChildren().clear();
//        for (PeriodeEvaluation racine : racines) {
//            racineTechnique.getChildren().add(construireNoeud(racine));
//        }
//
//        if (racines.isEmpty()) {
//            afficherArbreVide("Aucune période créée pour cette année - génère la structure standard pour commencer");
//        } else {
//            masquerArbreVide();
//        }
//
//        // Restaurer la sélection si possible
//        if (idSelectionne != null) {
//            TreeItem<PeriodeEvaluation> nouveau = trouverNoeudParId(racineTechnique, idSelectionne);
//            if (nouveau != null) {
//                treePeriodes.getSelectionModel().select(nouveau);
//            } else {
//                viderPanneauDetail();
//            }
//        }
//    }
//
//    private TreeItem<PeriodeEvaluation> trouverNoeudParId(TreeItem<PeriodeEvaluation> racine, String id) {
//        if (racine.getValue() != null && id.equals(racine.getValue().getId())) {
//            return racine;
//        }
//        for (TreeItem<PeriodeEvaluation> enfant : racine.getChildren()) {
//            TreeItem<PeriodeEvaluation> trouve = trouverNoeudParId(enfant, id);
//            if (trouve != null) return trouve;
//        }
//        return null;
//    }
//
//    /* ============================================================
//       CHARGEMENT ARBORESCENCE (manuel)
//       ============================================================ */
//    private void chargerArborescence() {
//        if (idAnneeCourante == null) {
//            afficherArbreVide("Aucune année scolaire sélectionnée");
//            return;
//        }
//
//        service.chargerArborescenceAsync(idAnneeCourante,
//                racines -> Platform.runLater(() -> peuplerArbre(racines)),
//                erreur -> Platform.runLater(() -> {
//                    System.err.println("❌ Erreur chargement arborescence : " + erreur.getMessage());
//                    ToastNotification.erreur(fenetre(), "Impossible de charger les périodes d'évaluation.");
//                    afficherArbreVide("Erreur de chargement");
//                }));
//    }
//
//    private void peuplerArbre(List<PeriodeEvaluation> racines) {
//        racineTechnique.getChildren().clear();
//        for (PeriodeEvaluation racine : racines) {
//            racineTechnique.getChildren().add(construireNoeud(racine));
//        }
//        if (racines.isEmpty()) {
//            afficherArbreVide("Aucune période créée pour cette année — génère la structure standard pour commencer");
//        } else {
//            masquerArbreVide();
//        }
//        viderPanneauDetail();
//    }
//
//    private TreeItem<PeriodeEvaluation> construireNoeud(PeriodeEvaluation periode) {
//        TreeItem<PeriodeEvaluation> noeud = new TreeItem<>(periode);
//        noeud.setExpanded(true);
//        for (PeriodeEvaluation enfant : periode.getSousPeriodes()) {
//            noeud.getChildren().add(construireNoeud(enfant));
//        }
//        return noeud;
//    }
//
//    /* ============================================================
//       ARBRE — ÉTAT VIDE
//       ============================================================ */
//    private void afficherArbreVide(String message) {
//        if (lblArbreVide == null) return;
//        lblArbreVide.setText(message);
//        lblArbreVide.setVisible(true);
//        lblArbreVide.setManaged(true);
//        treePeriodes.setVisible(false);
//        treePeriodes.setManaged(false);
//    }
//
//    private void masquerArbreVide() {
//        if (lblArbreVide == null) return;
//        lblArbreVide.setVisible(false);
//        lblArbreVide.setManaged(false);
//        treePeriodes.setVisible(true);
//        treePeriodes.setManaged(true);
//    }
//
//    /* ============================================================
//       CELLULE ARBRE
//       ============================================================ */
//    private static class CelluleArbre extends TreeCell<PeriodeEvaluation> {
//        private final FontIcon icone = new FontIcon();
//        private final Label    texte = new Label();
//        private final HBox conteneur = new HBox(8, icone, texte);
//
//        @Override
//        protected void updateItem(PeriodeEvaluation periode, boolean vide) {
//            super.updateItem(periode, vide);
//            if (vide || periode == null) {
//                setGraphic(null);
//                setText(null);
//                return;
//            }
//            icone.setIconLiteral(periode.isEstCloturee() ? "fth-lock" : "fth-unlock");
//            texte.setText(periode.getLibelle());
//            setGraphic(conteneur);
//        }
//    }
//
//    /* ============================================================
//       PANNEAU DE DÉTAIL
//       ============================================================ */
//    private void afficherDansPanneauDetail(PeriodeEvaluation periode) {
//        this.periodeSelectionnee = periode;
//
//        if (periode == null) {
//            viderPanneauDetail();
//            return;
//        }
//
//        lblPeriodeSelectionnee.setText(periode.getLibelle());
//        champLibelle.setText(periode.getLibelle());
//        champDateDebut.setValue(periode.getDateDebut());
//        champDateFin.setValue(periode.getDateFin());
//        champPoids.setText(periode.getPoids() != null
//                ? periode.getPoids().toPlainString() : "1.00");
//
//        toggleCloturee.setSelected(periode.isEstCloturee());
//        boolean aDesEnfantsOuverts = periode.aDesSousPeriodes()
//                && periode.getSousPeriodes().stream().anyMatch(sp -> !sp.isEstCloturee());
//        toggleCloturee.setDisable(aDesEnfantsOuverts && !periode.isEstCloturee());
//
//        btnSupprimer.setDisable(periode.aDesSousPeriodes());
//    }
//
//    private void viderPanneauDetail() {
//        periodeSelectionnee = null;
//        lblPeriodeSelectionnee.setText("Sélectionnez une période");
//        champLibelle.clear();
//        champDateDebut.setValue(null);
//        champDateFin.setValue(null);
//        champPoids.setText("1.00");
//        toggleCloturee.setSelected(false);
//        toggleCloturee.setDisable(true);
//        btnSupprimer.setDisable(true);
//    }
//
//    /* ============================================================
//       ACTIONS
//       ============================================================ */
//
//    @FXML
//    private void onNouvellePeriode() {
//        if (idAnneeCourante == null) {
//            ToastNotification.erreur(fenetre(), "Sélectionnez d'abord une année scolaire.");
//            return;
//        }
//
//        TreeItem<PeriodeEvaluation> selection = treePeriodes.getSelectionModel().getSelectedItem();
//        PeriodeEvaluation parent = (selection != null) ? selection.getValue() : null;
//
//        String typeParDefaut = (parent != null)
//                ? PeriodeEvaluation.TYPE_SEQUENCE
//                : PeriodeEvaluation.TYPE_TRIMESTRE;
//
//        viderPanneauDetail();
//        periodeSelectionnee = new PeriodeEvaluation();
//        periodeSelectionnee.setTypePeriode(typeParDefaut);
//        periodeSelectionnee.setIdAnnee(idAnneeCourante);
//        periodeSelectionnee.setIdPeriodeParent(parent != null ? parent.getId() : null);
//        periodeSelectionnee.setNumeroOrdre(parent != null
//                ? parent.getSousPeriodes().size() + 1
//                : racineTechnique.getChildren().size() + 1);
//
//        lblPeriodeSelectionnee.setText(parent != null
//                ? "Nouvelle séquence — sous " + parent.getLibelle()
//                : "Nouvelle période racine");
//        toggleCloturee.setDisable(true);
//        champDateDebut.requestFocus();
//    }
//
//    @FXML
//    private void onEnregistrer() {
//        if (periodeSelectionnee == null) return;
//
//        String libelle = champLibelle.getText();
//        LocalDate debut = champDateDebut.getValue();
//        LocalDate fin = champDateFin.getValue();
//        BigDecimal poids = parserPoids(champPoids.getText());
//
//        // Validation locale
//        if (libelle == null || libelle.isBlank()) {
//            ToastNotification.avertissement(fenetre(), "Le libellé est obligatoire.");
//            champLibelle.requestFocus();
//            return;
//        }
//        if (debut == null) {
//            ToastNotification.avertissement(fenetre(), "La date de début est obligatoire.");
//            champDateDebut.requestFocus();
//            return;
//        }
//        if (fin == null) {
//            ToastNotification.avertissement(fenetre(), "La date de fin est obligatoire.");
//            champDateFin.requestFocus();
//            return;
//        }
//        if (!fin.isAfter(debut)) {
//            ToastNotification.avertissement(fenetre(),
//                    "La date de fin doit être postérieure à la date de début.");
//            champDateFin.requestFocus();
//            return;
//        }
//
//        // Appliquer au modèle
//        periodeSelectionnee.setLibelle(libelle.trim());
//        periodeSelectionnee.setDateDebut(debut);
//        periodeSelectionnee.setDateFin(fin);
//        periodeSelectionnee.setPoids(poids);
//
//        boolean estCreation = periodeSelectionnee.getId() == null
//                || periodeSelectionnee.getId().isBlank();
//
//        if (estCreation) {
//            if (periodeSelectionnee.getCodePeriode() == null
//                    || periodeSelectionnee.getCodePeriode().isBlank()) {
//                periodeSelectionnee.setCodePeriode(genererCodeParDefaut(periodeSelectionnee));
//            }
//            periodeSelectionnee.setId(UUID.randomUUID().toString());
//        }
//
//        // Verrouillage UI
//        chargementEnCours = true;
//        btnEnregistrer.setDisable(true);
//        btnSupprimer.setDisable(true);
//
//        Runnable onSuccess = () -> Platform.runLater(() -> {
//            chargementEnCours = false;
//            btnEnregistrer.setDisable(false);
//            btnSupprimer.setDisable(false);
//            ToastNotification.succes(fenetre(),
//                    estCreation ? "Période créée." : "Période mise à jour.");
//            chargerArborescence();
//        });
//
//        Consumer<Throwable> onError = err -> Platform.runLater(() -> {
//            chargementEnCours = false;
//            btnEnregistrer.setDisable(false);
//            btnSupprimer.setDisable(false);
//            gererErreurEnregistrement(err);
//        });
//
//        if (estCreation) {
//            service.creerAsync(periodeSelectionnee, idEcoleCourante, onSuccess, onError);
//        } else {
//            service.modifierAsync(periodeSelectionnee, onSuccess, onError);
//        }
//    }
//
//    private void gererErreurEnregistrement(Throwable erreur) {
//        System.err.println("❌ Erreur enregistrement période : " + erreur.getMessage());
//        if (erreur != null) erreur.printStackTrace();
//        ToastNotification.erreur(fenetre(), erreur != null && erreur.getMessage() != null
//                ? erreur.getMessage()
//                : "Échec de l'enregistrement de la période.");
//    }
//
//    @FXML
//    private void onSupprimer() {
//        if (periodeSelectionnee == null || periodeSelectionnee.getId() == null) return;
//
//        Boolean confirme = AlertUtil.afficherConfirmation(
//                "Supprimer la période",
//                "Supprimer définitivement \"" + periodeSelectionnee.getLibelle() + "\" ?",
//                fenetre());
//        if (!Boolean.TRUE.equals(confirme)) return;
//
//        chargementEnCours = true;
//        btnSupprimer.setDisable(true);
//
//        service.supprimerAsync(periodeSelectionnee,
//                () -> Platform.runLater(() -> {
//                    chargementEnCours = false;
//                    btnSupprimer.setDisable(false);
//                    ToastNotification.succes(fenetre(), "Période supprimée.");
//                    chargerArborescence();
//                }),
//                erreur -> Platform.runLater(() -> {
//                    chargementEnCours = false;
//                    btnSupprimer.setDisable(false);
//                    System.err.println("❌ Erreur suppression : " + erreur.getMessage());
//                    ToastNotification.erreur(fenetre(), erreur.getMessage() != null
//                            ? erreur.getMessage() : "Échec de la suppression.");
//                }));
//    }
//
//    @FXML
//    private void onEnregistrerCloture() {
//        if (periodeSelectionnee == null || periodeSelectionnee.getId() == null) return;
//
//        boolean veutCloturer = toggleCloturee.isSelected();
//
//        chargementEnCours = true;
//        toggleCloturee.setDisable(true);
//
//        if (veutCloturer) {
//            service.cloturerAsync(periodeSelectionnee,
//                    () -> Platform.runLater(() -> {
//                        chargementEnCours = false;
//                        toggleCloturee.setDisable(false);
//                        ToastNotification.succes(fenetre(), "Période clôturée.");
//                        chargerArborescence();
//                    }),
//                    erreur -> Platform.runLater(() -> {
//                        chargementEnCours = false;
//                        toggleCloturee.setDisable(false);
//                        toggleCloturee.setSelected(false);
//                        System.err.println("❌ Erreur clôture : " + erreur.getMessage());
//                        ToastNotification.erreur(fenetre(), erreur.getMessage());
//                    }));
//        } else {
//            Boolean confirme = AlertUtil.afficherConfirmation(
//                    "Réouvrir la période",
//                    "Réouvrir \"" + periodeSelectionnee.getLibelle()
//                            + "\" permettra à nouveau la saisie des notes. Continuer ?",
//                    fenetre());
//
//            if (!Boolean.TRUE.equals(confirme)) {
//                toggleCloturee.setSelected(true);
//                toggleCloturee.setDisable(false);
//                chargementEnCours = false;
//                return;
//            }
//
//            service.reouvrirAsync(periodeSelectionnee,
//                    () -> Platform.runLater(() -> {
//                        chargementEnCours = false;
//                        toggleCloturee.setDisable(false);
//                        ToastNotification.succes(fenetre(), "Période réouverte.");
//                        chargerArborescence();
//                    }),
//                    erreur -> Platform.runLater(() -> {
//                        chargementEnCours = false;
//                        toggleCloturee.setDisable(false);
//                        toggleCloturee.setSelected(true);
//                        System.err.println("❌ Erreur réouverture : " + erreur.getMessage());
//                        ToastNotification.erreur(fenetre(), erreur.getMessage());
//                    }));
//        }
//    }
//
//    @FXML
//    private void onGenererStructureStandard() {
//        if (idAnneeCourante == null) {
//            ToastNotification.erreur(fenetre(), "Sélectionnez d'abord une année scolaire.");
//            return;
//        }
//
//        String fxml = "/org/erpklassup/erpklassup/view/module2/GenerePeriode.fxml";
//
//        GenererPeriodesModalController ctrl = ModalUtil.ouvrirModal(
//                getClass(), fxml, "Générer la structure",
//                fenetre(), 520, 460);
//
//        if (ctrl != null) {
//            ctrl.initData(idEcoleCourante, idAnneeCourante, this::chargerArborescence);
//        }
//    }
//
//    /* ============================================================
//       UTILITAIRES
//       ============================================================ */
//    private void appliquerFormatDate(DatePicker picker) {
//        picker.setConverter(new StringConverter<LocalDate>() {
//            @Override
//            public String toString(LocalDate date) {
//                return date != null ? FORMAT_DATE.format(date) : "";
//            }
//
//            @Override
//            public LocalDate fromString(String texte) {
//                if (texte == null || texte.isBlank()) return null;
//                try {
//                    return LocalDate.parse(texte.trim(), FORMAT_DATE);
//                } catch (DateTimeParseException e) {
//                    System.err.println("⚠️ Format date invalide : " + texte);
//                    return null;
//                }
//            }
//        });
//        picker.setPromptText("jj/mm/aaaa");
//    }
//
//    private BigDecimal parserPoids(String texte) {
//        try {
//            return new BigDecimal(texte.trim().replace(",", "."));
//        } catch (Exception e) {
//            return BigDecimal.ONE;
//        }
//    }
//
//    private String genererCodeParDefaut(PeriodeEvaluation periode) {
//        String prefixe = switch (periode.getTypePeriode()) {
//            case PeriodeEvaluation.TYPE_TRIMESTRE -> "T";
//            case PeriodeEvaluation.TYPE_SEMESTRE  -> "S";
//            case PeriodeEvaluation.TYPE_SEQUENCE  -> "SEQ";
//            default -> "P";
//        };
//        return prefixe + periode.getNumeroOrdre();
//    }
//
//    private Stage fenetre() {
//        return (Stage) treePeriodes.getScene().getWindow();
//    }
//}