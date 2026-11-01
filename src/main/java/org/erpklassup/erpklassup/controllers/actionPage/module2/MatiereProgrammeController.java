package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.erpklassup.erpklassup.dto.LigneAffectationNiveau;
import org.erpklassup.erpklassup.dto.ResultatAffectationsNiveau;
import org.erpklassup.erpklassup.models.module2.Matiere;
import org.erpklassup.erpklassup.models.module2.MatiereNiveau;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.module2.MatiereNiveauService;
import org.erpklassup.erpklassup.service.module2.MatiereService;
import org.erpklassup.erpklassup.util.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class MatiereProgrammeController implements Initializable, VueDisposable {

    /* ============================================================
       FXML
       ============================================================ */
    @FXML private TextField          fieldRecherche;
    @FXML private ListView<Matiere>  listMatieres;
    @FXML private Button             btnNouvelleMatiere;

    @FXML private Label    lblMatiereSelectionnee;
    @FXML private CheckBox chkAppliquerTous;
    @FXML private Accordion accordionNiveaux;
    @FXML private Button   btnEnregistrerAffectations;

    @FXML private Button btnSupprimerMatiere;
    @FXML private Button btnRenommerMatiere;

    /* ============================================================
       Constantes
       ============================================================ */
    private static final Duration PERIODE_REFRESH_MATIERES = Duration.seconds(10);
    private static final Duration DEBOUNCE_RECHERCHE       = Duration.millis(350);

    /* ============================================================
       Services
       ============================================================ */
    private final MatiereService       matiereService       = new MatiereService();
    private final MatiereNiveauService matiereNiveauService = new MatiereNiveauService();
    private final AuditService         auditService         = new AuditService();

    /* ============================================================
       État
       ============================================================ */
    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;
    private final PauseTransition debounceRecherche = new PauseTransition(DEBOUNCE_RECHERCHE);

    private final Map<String, CheckBox>  casesParCleNiveau = new LinkedHashMap<>();
    private final Map<String, TextField> champsCoefficient = new LinkedHashMap<>();

    private Matiere matiereSelectionnee;
    private boolean enChargement = false;

    private AutoRefreshService<List<Matiere>> autoRefreshMatieres;

    private final SessionManager session = SessionManager.getInstance();
    private String idEcoleCourante  = session.getIdEcoleCourante();
    private String idUtilisateurConnecte;

    /* ============================================================
       i18n
       ============================================================ */
    private I18nManager i18n() {
        return I18nManager.getInstance();
    }

    /* ============================================================
       Initialisation
       ============================================================ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idUtilisateurConnecte = (session.getUtilisateurCourant() != null)
                ? session.getUtilisateurCourant().getIdUtilisateur() : null;

        listMatieres.setItems(FXCollections.observableArrayList());
        listMatieres.setPlaceholder(new Label("Chargement des matières..."));
        listMatieres.setCellFactory(lv -> new CelluleMatiere());

        listMatieres.getSelectionModel().selectedItemProperty().addListener((obs, anc, nouv) -> {
            matiereSelectionnee = nouv;
            if (nouv != null) chargerAffectationsMatiere(nouv);
            else activerPanneauDroit(false);
        });

        btnNouvelleMatiere.setOnAction(ev -> ouvrirModalCreation());
        btnEnregistrerAffectations.setOnAction(ev -> enregistrerAffectations());
        chkAppliquerTous.setOnAction(ev -> appliquerATousLesNiveaux());

        debounceRecherche.setOnFinished(ev -> lancerRecherche());
        fieldRecherche.textProperty().addListener((obs, anc, nouv) ->
                debounceRecherche.playFromStart());

        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
        appliquerControlesAcces();

        configurationBtn();
        rafraichirContexte();
    }

    private void configurationBtn() {
        btnRenommerMatiere.setTooltip(new Tooltip("Modifier la matière"));
        btnSupprimerMatiere.setTooltip(new Tooltip("Supprimer la matière"));

        btnSupprimerMatiere.setOnAction(event -> supprimerMatiere(matiereSelectionnee));
        btnRenommerMatiere.setOnAction(event -> ouvrirModalEdition(matiereSelectionnee));
    }

    /* ============================================================
       Dispose
       ============================================================ */
    @Override
    public void disposer() {
        if (autoRefreshMatieres != null) {
            autoRefreshMatieres.dispose();
            autoRefreshMatieres = null;
        }
        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
    }

    /* ============================================================
       Contrôles d'accès — commentés pour le moment
       ============================================================ */
    private void appliquerControlesAcces() {
        // ControleAcces.appliquerAction(btnNouvelleMatiere, "matiere.creer");
        // ControleAcces.appliquerAction(btnEnregistrerAffectations, "matiere.affecter");
        // listMatieres.refresh();
    }

    /* ============================================================
       Contexte
       ============================================================ */
    public void rafraichirContexte() {
        SessionManager s = SessionManager.getInstance();
        this.idEcoleCourante = s.getIdEcoleCourante();
        this.idUtilisateurConnecte = (s.getUtilisateurCourant() != null)
                ? s.getUtilisateurCourant().getIdUtilisateur() : null;

        if (idEcoleCourante == null) {
            listMatieres.setItems(FXCollections.observableArrayList());
            listMatieres.setPlaceholder(new Label("Aucune école active"));
            activerPanneauDroit(false);
            return;
        }

        chargerMatieres();
        demarrerRafraichissementAuto();
    }

    /* ============================================================
       Auto-refresh
       ============================================================ */
    private void demarrerRafraichissementAuto() {
        if (autoRefreshMatieres != null && autoRefreshMatieres.isRunning()) {
            autoRefreshMatieres.restartNow();
            return;
        }

        autoRefreshMatieres = new AutoRefreshService<List<Matiere>>(PERIODE_REFRESH_MATIERES)
                .loader(() -> matiereService.rechercher(idEcoleCourante, getSearchText()))
                .onSuccess(this::appliquerRefreshMatieres)
                .onFailure(err -> System.err.println("Erreur auto-refresh matières : " + err.getMessage()))
                .start();
    }

    /**
     * Applique le résultat du refresh en fusionnant avec la liste courante.
     *
     * <p><b>Points clés :</b></p>
     * <ul>
     *   <li>{@link FusionListeUtil#fusionner} fait des modifications granulaires sur
     *       la liste observable (remove/add/set) — c'est ce qui préserve le scroll
     *       et la sélection.</li>
     *   <li>{@code listMatieres.refresh()} est NÉCESSAIRE dans le cas où les mêmes
     *       instances d'objets sont réutilisées et que leurs champs ont changé
     *       (sinon le ListView ne re-rend pas les cellules).</li>
     * </ul>
     */
    private void appliquerRefreshMatieres(List<Matiere> nouvellesMatieres) {
        if (nouvellesMatieres == null) return;

        String idSelectionne = (matiereSelectionnee != null) ? matiereSelectionnee.getId() : null;

        FusionListeUtil.fusionner(listMatieres.getItems(), nouvellesMatieres, Matiere::getId);
        listMatieres.setPlaceholder(new Label("Aucune matière enregistrée"));

        // ✅ Force le re-rendu des cellules visibles (indispensable quand on
        // fusionne en place avec des instances réutilisées).
        listMatieres.refresh();

        if (idSelectionne != null) {
            boolean toujoursPresente = nouvellesMatieres.stream()
                    .anyMatch(m -> idSelectionne.equals(m.getId()));

            if (!toujoursPresente) {
                matiereSelectionnee = null;
                activerPanneauDroit(false);
            }
        } else if (!nouvellesMatieres.isEmpty() && listMatieres.getSelectionModel().isEmpty()) {
            listMatieres.getSelectionModel().selectFirst();
        }
    }

    /* ============================================================
       Panneau droit
       ============================================================ */
    private void activerPanneauDroit(boolean actif) {
        accordionNiveaux.setDisable(!actif);
        chkAppliquerTous.setDisable(!actif);
        btnEnregistrerAffectations.setDisable(!actif);

        if (!actif) {
            lblMatiereSelectionnee.setText("Sélectionnez une matière");
            accordionNiveaux.getPanes().clear();
            casesParCleNiveau.clear();
            champsCoefficient.clear();
        }
    }

    /* ============================================================
       Chargement des matières
       ============================================================ */
    private void chargerMatieres() {
        matiereService.listerParEcoleAsync(idEcoleCourante,
                matieres -> Platform.runLater(() -> {
                    FusionListeUtil.fusionner(listMatieres.getItems(), matieres, Matiere::getId);
                    listMatieres.setPlaceholder(new Label("Aucune matière enregistrée"));
                    listMatieres.refresh();

                    if (!matieres.isEmpty() && listMatieres.getSelectionModel().isEmpty()) {
                        listMatieres.getSelectionModel().selectFirst();
                    }
                }),
                erreur -> Platform.runLater(erreur::printStackTrace));
    }

    /* ============================================================
       Recherche
       ============================================================ */
    private void lancerRecherche() {
        if (idEcoleCourante == null) return;

        matiereService.rechercherAsync(idEcoleCourante, getSearchText(),
                matieres -> Platform.runLater(() -> {
                    FusionListeUtil.fusionner(listMatieres.getItems(), matieres, Matiere::getId);

                    if (matieres.isEmpty()) {
                        listMatieres.setPlaceholder(new Label("Aucun résultat"));
                    } else {
                        listMatieres.setPlaceholder(new Label("Aucune matière enregistrée"));
                    }

                    listMatieres.refresh();

                    if (matiereSelectionnee != null
                            && matieres.stream().noneMatch(m ->
                            m.getId().equals(matiereSelectionnee.getId()))) {
                        matiereSelectionnee = null;
                        activerPanneauDroit(false);
                    }
                }),
                erreur -> Platform.runLater(erreur::printStackTrace));
    }

    private String getSearchText() {
        String t = fieldRecherche.getText();
        return (t == null || t.isBlank()) ? null : t.trim();
    }

    /* ============================================================
       Chargement des affectations → construction de l'accordéon
       ============================================================ */
    private void chargerAffectationsMatiere(Matiere matiere) {
        activerPanneauDroit(true);
        lblMatiereSelectionnee.setText(matiere.getNomMatiere() != null
                ? matiere.getNomMatiere()
                : matiere.getCodeMatiere());

        matiereNiveauService.obtenirAffectationsParCycleAsync(
                idEcoleCourante, matiere.getId(),
                parCycle -> Platform.runLater(() -> construireAccordion(parCycle)),
                erreur -> Platform.runLater(erreur::printStackTrace));
    }

    /* ============================================================
       Construction dynamique de l'accordéon
       ============================================================ */
    private void construireAccordion(Map<String, List<MatiereNiveau>> parCycle) {
        enChargement = true;

        accordionNiveaux.getPanes().clear();
        casesParCleNiveau.clear();
        champsCoefficient.clear();

        List<String> ordreCycles = List.of("PREMIER_CYCLE", "SECOND_CYCLE", "PRIMAIRE");

        for (String cycle : ordreCycles) {
            List<MatiereNiveau> affectations = parCycle.get(cycle);
            if (affectations == null || affectations.isEmpty()) continue;

            affectations.sort(Comparator
                    .comparing((MatiereNiveau mn) ->
                            mn.getNomNiveau() != null ? mn.getNomNiveau() : "")
                    .thenComparing(mn ->
                            mn.getCodeFiliere() != null ? mn.getCodeFiliere() : ""));

            VBox corps = new VBox(6);
            corps.setPadding(new Insets(8, 4, 8, 4));
            corps.getStyleClass().add("permissions-module-body");

            for (MatiereNiveau mn : affectations) {
                corps.getChildren().add(construireLigneAffectation(mn));
            }

            TitledPane pane = new TitledPane(libelleCycle(cycle), corps);
            pane.getStyleClass().add("permission-titled-pane");
            accordionNiveaux.getPanes().add(pane);
        }

        if (!accordionNiveaux.getPanes().isEmpty()) {
            accordionNiveaux.setExpandedPane(accordionNiveaux.getPanes().get(0));
        }

        enChargement = false;
    }

    private String libelleCycle(String code) {
        return switch (code) {
            case "PREMIER_CYCLE" -> "Premier cycle";
            case "SECOND_CYCLE"  -> "Second cycle";
            case "PRIMAIRE"      -> "Maternelle / Primaire";
            default              -> code;
        };
    }

    private HBox construireLigneAffectation(MatiereNiveau mn) {
        String cle = cleAffectation(mn.getIdNiveau(), mn.getIdFiliere());

        CheckBox checkObligatoire = new CheckBox();
        checkObligatoire.setSelected(mn.isObligatoire());
        checkObligatoire.getStyleClass().add("permission-check");
        checkObligatoire.selectedProperty().addListener((obs, a, n) -> {
            if (!enChargement) signalerModification();
        });

        Label libelleNiveau = new Label(libelleNiveau(mn));
        libelleNiveau.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblCoeff = new Label("Coeff.");
        lblCoeff.getStyleClass().add("field-label");

        TextField champCoeff = new TextField(
                mn.getCoefficient() != null ? mn.getCoefficient().toPlainString() : "1");
        champCoeff.getStyleClass().add("input");
        champCoeff.setPrefWidth(60);
        champCoeff.textProperty().addListener((obs, a, n) -> {
            if (!enChargement) signalerModification();
        });

        HBox ligne = new HBox(10, checkObligatoire, libelleNiveau, spacer, lblCoeff, champCoeff);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setPadding(new Insets(6, 0, 6, 0));

        casesParCleNiveau.put(cle, checkObligatoire);
        champsCoefficient.put(cle, champCoeff);

        return ligne;
    }

    private String cleAffectation(String idNiveau, String idFiliere) {
        return idNiveau + "#" + (idFiliere != null ? idFiliere : "_");
    }

    private String libelleNiveau(MatiereNiveau mn) {
        StringBuilder sb = new StringBuilder();
        sb.append(mn.getNomNiveau() != null ? mn.getNomNiveau() : mn.getIdNiveau());
        if (mn.getCodeFiliere() != null && !mn.getCodeFiliere().isBlank()) {
            sb.append(" — ").append(mn.getCodeFiliere());
        }
        return sb.toString();
    }

    private void signalerModification() {
        if (btnEnregistrerAffectations != null) {
            btnEnregistrerAffectations.setDisable(false);
        }
    }

    /* ============================================================
       Enregistrement des affectations
       ============================================================ */
    private void enregistrerAffectations() {
        if (matiereSelectionnee == null) return;

        List<LigneAffectationNiveau> lignes = new ArrayList<>();

        for (Map.Entry<String, CheckBox> entry : casesParCleNiveau.entrySet()) {
            String cle = entry.getKey();
            CheckBox check = entry.getValue();
            TextField champCoeff = champsCoefficient.get(cle);

            String[] parties = cle.split("#");
            String idNiveau  = parties[0];
            String idFiliere = "_".equals(parties[1]) ? null : parties[1];

            BigDecimal coeff = parseCoefficient(champCoeff.getText());

            lignes.add(new LigneAffectationNiveau(
                    idNiveau, idFiliere, coeff, check.isSelected()));
        }

        Stage stage = StageHelper.getStage(btnEnregistrerAffectations);
        btnEnregistrerAffectations.setDisable(true);

        matiereNiveauService.enregistrerAffectationsPourMatiereAsync(
                idEcoleCourante, matiereSelectionnee.getId(), lignes,
                resultat -> Platform.runLater(() -> {
                    btnEnregistrerAffectations.setDisable(false);
                    gererResultatEnregistrement(stage, resultat);
                    if (matiereSelectionnee != null) {
                        chargerAffectationsMatiere(matiereSelectionnee);
                    }
                }),
                erreur -> Platform.runLater(() -> {
                    btnEnregistrerAffectations.setDisable(false);
                    ToastNotification.erreur(stage, "Échec de l'enregistrement des affectations.");
                    System.err.println("Erreur enregistrement affectations : " + erreur.getMessage());
                }));
    }

    private void gererResultatEnregistrement(Stage stage, ResultatAffectationsNiveau resultat) {
        if (resultat == null) return;

        if (resultat.estSucces()) {
            ToastNotification.succes(stage,
                    "Enregistré : " + resultat.getEnregistrees()
                            + " affectation(s), " + resultat.getRetirees() + " retrait(s).");
        } else {
            String message = String.join("\n", resultat.getErreurs());
            AlertUtil.afficherErreur("Enregistrement partiel", message, stage);
        }

        auditService.tracerActionAsync(
                "ACADEMIQUE",
                "MODIFICATION_AFFECTATIONS_MATIERE",
                "Matière « " + matiereSelectionnee.getNomMatiere()
                        + " » — " + resultat.getEnregistrees() + " affectation(s), "
                        + resultat.getRetirees() + " retrait(s)"
        );
    }

    private BigDecimal parseCoefficient(String texte) {
        if (texte == null || texte.isBlank()) return BigDecimal.ONE;
        try {
            return new BigDecimal(texte.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ONE;
        }
    }

    /* ============================================================
       Appliquer à tous les niveaux
       ============================================================ */
    private void appliquerATousLesNiveaux() {
        if (matiereSelectionnee == null) return;

        Stage stage = StageHelper.getStage(chkAppliquerTous);

        if (!chkAppliquerTous.isSelected()) return;

        boolean confirme = AlertUtil.afficherConfirmation(
                "Appliquer à tous les niveaux",
                "Voulez-vous vraiment affecter \"" + matiereSelectionnee.getNomMatiere()
                        + "\" à tous les niveaux avec un coefficient de 1 ?",
                stage
        );

        if (!confirme) {
            chkAppliquerTous.setSelected(false);
            return;
        }

        matiereNiveauService.appliquerATousLesNiveauxAsync(
                idEcoleCourante, matiereSelectionnee.getId(), BigDecimal.ONE,
                resultat -> Platform.runLater(() -> {
                    chkAppliquerTous.setSelected(false);
                    ToastNotification.succes(stage,
                            "Affectée à " + resultat.getEnregistrees() + " niveau(x).");
                    chargerAffectationsMatiere(matiereSelectionnee);
                }),
                erreur -> Platform.runLater(() -> {
                    chkAppliquerTous.setSelected(false);
                    ToastNotification.erreur(stage, "Échec de l'application globale.");
                    System.err.println("Erreur application globale : " + erreur.getMessage());
                }));
    }

    /* ============================================================
       Modale création / édition (via ModalUtil)
       ============================================================ */
    private void ouvrirModalCreation() {
        String fxml = "/org/erpklassup/erpklassup/view/module2/ModalMatiere.fxml";

        MatiereModalController ctrl = ModalUtil.ouvrirModal(
                getClass(),
                fxml,
                i18n().t("matiere.modal.create.title"),
                listMatieres.getScene().getWindow(),
                500, 575
        );

        if (ctrl != null) {
            ctrl.initCreation(idEcoleCourante, () -> {
                chargerMatieres();
                if (autoRefreshMatieres != null) autoRefreshMatieres.restartNow();
            });
        }
    }

    private void ouvrirModalEdition(Matiere matiere) {
        if (matiere == null) return;

        String fxml = "/org/erpklassup/erpklassup/view/module2/ModalMatiere.fxml";

        MatiereModalController ctrl = ModalUtil.ouvrirModal(
                getClass(),
                fxml,
                i18n().t("matiere.modal.edit.title"),
                listMatieres.getScene().getWindow(),
                500, 575
        );

        if (ctrl != null) {
            ctrl.initEdition(idEcoleCourante, matiere, () -> {
                chargerMatieres();
                if (autoRefreshMatieres != null) autoRefreshMatieres.restartNow();
            });
        }
    }

    /* ============================================================
       Suppression d'une matière (via AlertUtil)
       ============================================================ */
    private void supprimerMatiere(Matiere matiere) {
        if (matiere == null) return;

        Stage stage = StageHelper.getStage(listMatieres);

        boolean confirme = AlertUtil.afficherConfirmation(
                i18n().t("matiere.confirm_delete.title"),
                i18n().t("matiere.confirm_delete.message", matiere.getNomMatiere()),
                stage
        );

        if (!confirme) return;

        matiereService.supprimerAsync(matiere.getId(),
                () -> Platform.runLater(() -> {
                    auditService.tracerActionAsync(
                            "ACADEMIQUE",
                            "SUPPRESSION_MATIERE",
                            "Suppression de la matière : " + matiere.getNomMatiere()
                    );
                    ToastNotification.succes(stage,
                            i18n().t("matiere.toast.deleted", matiere.getNomMatiere()));
                    chargerMatieres();
                    if (autoRefreshMatieres != null) autoRefreshMatieres.restartNow();
                }),
                erreur -> Platform.runLater(() -> {
                    ToastNotification.erreur(stage,
                            i18n().t("matiere.error.delete_failed"));
                    System.err.println("❌ Suppression matière : " + erreur.getMessage());
                }));
    }

    /* ============================================================
       Cellule personnalisée
       ============================================================ */
    private class CelluleMatiere extends ListCell<Matiere> {
        private final Label nom       = new Label();
        private final Label code      = new Label();
        private final VBox  conteneur = new VBox(2, nom, code);

        CelluleMatiere() {
            nom.getStyleClass().add("cell-title");
            code.getStyleClass().add("cell-subtitle");
        }

        @Override
        protected void updateItem(Matiere m, boolean vide) {
            super.updateItem(m, vide);
            if (vide || m == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            nom.setText(m.getNomMatiere() != null ? m.getNomMatiere() : "—");
            code.setText(m.getCodeMatiere() != null ? m.getCodeMatiere() : "");
            setGraphic(conteneur);
            setText(null);
        }
    }
}
//package org.erpklassup.erpklassup.controllers.actionPage.module2;
//
//import javafx.animation.PauseTransition;
//import javafx.application.Platform;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.geometry.Insets;
//import javafx.geometry.Pos;
//import javafx.scene.control.*;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.Priority;
//import javafx.scene.layout.Region;
//import javafx.scene.layout.VBox;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//import javafx.util.StringConverter;
//
//import org.erpklassup.erpklassup.dto.LigneAffectationNiveau;
//import org.erpklassup.erpklassup.dto.ResultatAffectationsNiveau;
//import org.erpklassup.erpklassup.models.module2.Matiere;
//import org.erpklassup.erpklassup.models.module2.MatiereNiveau;
//import org.erpklassup.erpklassup.service.AuditService;
//import org.erpklassup.erpklassup.service.SessionManager;
//import org.erpklassup.erpklassup.service.module2.MatiereNiveauService;
//import org.erpklassup.erpklassup.service.module2.MatiereService;
//import org.erpklassup.erpklassup.util.*;
//import org.kordamp.ikonli.javafx.FontIcon;
//
//import java.math.BigDecimal;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.ResourceBundle;
//
//public class MatiereProgrammeController implements Initializable, VueDisposable {
//
//    /* ============================================================
//       FXML
//       ============================================================ */
//    @FXML private TextField          fieldRecherche;
//    @FXML private ListView<Matiere>  listMatieres;
//    @FXML private Button             btnNouvelleMatiere;
//
//    @FXML private Label    lblMatiereSelectionnee;
//    @FXML private CheckBox chkAppliquerTous;
//    @FXML private Accordion accordionNiveaux;
//    @FXML private Button   btnEnregistrerAffectations;
//    @FXML private Button btnSupprimerMatiere;
//    @FXML private Button btnRenommerMatiere;
//
//    /* ============================================================
//       Constantes
//       ============================================================ */
//    private static final Duration PERIODE_REFRESH_MATIERES = Duration.seconds(10);
//    private static final Duration DEBOUNCE_RECHERCHE       = Duration.millis(350);
//
//    /* ============================================================
//       Services
//       ============================================================ */
//    private final MatiereService       matiereService       = new MatiereService();
//    private final MatiereNiveauService matiereNiveauService = new MatiereNiveauService();
//    private final AuditService         auditService         = new AuditService();
//
//    /* ============================================================
//       État
//       ============================================================ */
//    private final Runnable ecouteurPermissions = this::appliquerControlesAcces;
//    private final PauseTransition debounceRecherche = new PauseTransition(DEBOUNCE_RECHERCHE);
//
//    private final Map<String, CheckBox>  casesParCleNiveau = new LinkedHashMap<>();
//    private final Map<String, TextField> champsCoefficient = new LinkedHashMap<>();
//
//    private Matiere matiereSelectionnee;
//    private boolean enChargement = false;
//
//    private AutoRefreshService<List<Matiere>> autoRefreshMatieres;
//
//    private final SessionManager session = SessionManager.getInstance();
//    private String idEcoleCourante  = session.getIdEcoleCourante();
//    private String idUtilisateurConnecte;
//
//    /* ============================================================
//       i18n
//       ============================================================ */
//    private I18nManager i18n() {
//        return I18nManager.getInstance();
//    }
//
//    /* ============================================================
//       Initialisation
//       ============================================================ */
//    @Override
//    public void initialize(URL url, ResourceBundle rb) {
//        idUtilisateurConnecte = (session.getUtilisateurCourant() != null)
//                ? session.getUtilisateurCourant().getIdUtilisateur() : null;
//
//        listMatieres.setItems(FXCollections.observableArrayList());
//        listMatieres.setPlaceholder(new Label("Chargement des matières..."));
//        listMatieres.setCellFactory(lv -> new CelluleMatiere());
//
//        listMatieres.getSelectionModel().selectedItemProperty().addListener((obs, anc, nouv) -> {
//            matiereSelectionnee = nouv;
//            if (nouv != null) chargerAffectationsMatiere(nouv);
//            else activerPanneauDroit(false);
//        });
//
//        btnNouvelleMatiere.setOnAction(ev -> ouvrirModalCreation());
//        btnEnregistrerAffectations.setOnAction(ev -> enregistrerAffectations());
//
//        chkAppliquerTous.setOnAction(ev -> appliquerATousLesNiveaux());
//
//        debounceRecherche.setOnFinished(ev -> lancerRecherche());
//        fieldRecherche.textProperty().addListener((obs, anc, nouv) ->
//                debounceRecherche.playFromStart());
//
//        SessionManager.getInstance().ecouterChangementsPermissions(ecouteurPermissions);
//        appliquerControlesAcces();
//
//        configurationBtn();
//        rafraichirContexte();
//    }
//
//    private void configurationBtn(){
//        btnRenommerMatiere.setTooltip(new Tooltip("Modifier la matière"));
//        btnSupprimerMatiere.setTooltip(new Tooltip("Supprimer la matière"));
//
//        btnSupprimerMatiere.setOnAction(event -> supprimerMatiere(matiereSelectionnee));
//        btnRenommerMatiere.setOnAction(event -> ouvrirModalEdition(matiereSelectionnee));
//    }
//
//    /* ============================================================
//       Dispose
//       ============================================================ */
//    @Override
//    public void disposer() {
//        if (autoRefreshMatieres != null) {
//            autoRefreshMatieres.dispose();
//            autoRefreshMatieres = null;
//        }
//        SessionManager.getInstance().arreterEcoute(ecouteurPermissions);
//    }
//
//    /* ============================================================
//       Contrôles d'accès — commentés pour le moment
//       ============================================================ */
//    private void appliquerControlesAcces() {
//        // ControleAcces.appliquerAction(btnNouvelleMatiere, "matiere.creer");
//        // ControleAcces.appliquerAction(btnEnregistrerAffectations, "matiere.affecter");
//        // listMatieres.refresh();
//    }
//
//    /* ============================================================
//       Contexte
//       ============================================================ */
//    public void rafraichirContexte() {
//        SessionManager s = SessionManager.getInstance();
//        this.idEcoleCourante = s.getIdEcoleCourante();
//        this.idUtilisateurConnecte = (s.getUtilisateurCourant() != null)
//                ? s.getUtilisateurCourant().getIdUtilisateur() : null;
//
//        if (idEcoleCourante == null) {
//            listMatieres.setItems(FXCollections.observableArrayList());
//            listMatieres.setPlaceholder(new Label("Aucune école active"));
//            activerPanneauDroit(false);
//            return;
//        }
//
//        chargerMatieres();
//        demarrerRafraichissementAuto();
//    }
//
//    /* ============================================================
//       Auto-refresh
//       ============================================================ */
//    private void demarrerRafraichissementAuto() {
//        if (autoRefreshMatieres != null && autoRefreshMatieres.isRunning()) {
//            autoRefreshMatieres.restartNow();
//            return;
//        }
//
//        autoRefreshMatieres = new AutoRefreshService<List<Matiere>>(PERIODE_REFRESH_MATIERES)
//                .loader(() -> matiereService.rechercher(idEcoleCourante, getSearchText()))
//                .onSuccess(this::appliquerRefreshMatieres)
//                .onFailure(err -> System.err.println("Erreur auto-refresh matières : " + err.getMessage()))
//                .start();
//    }
//
//    private void appliquerRefreshMatieres(List<Matiere> nouvellesMatieres) {
//        if (nouvellesMatieres == null) return;
//
//        String idSelectionne = (matiereSelectionnee != null) ? matiereSelectionnee.getId() : null;
//
//        FusionListeUtil.fusionner(listMatieres.getItems(), nouvellesMatieres, Matiere::getId);
//        listMatieres.setPlaceholder(new Label("Aucune matière enregistrée"));
//
//        if (idSelectionne != null) {
//            boolean toujoursPresente = nouvellesMatieres.stream()
//                    .anyMatch(m -> idSelectionne.equals(m.getId()));
//
//            if (!toujoursPresente) {
//                matiereSelectionnee = null;
//                activerPanneauDroit(false);
//            }
//        } else if (!nouvellesMatieres.isEmpty() && listMatieres.getSelectionModel().isEmpty()) {
//            listMatieres.getSelectionModel().selectFirst();
//        }
//    }
//
//    /* ============================================================
//       Panneau droit
//       ============================================================ */
//    private void activerPanneauDroit(boolean actif) {
//        accordionNiveaux.setDisable(!actif);
//        chkAppliquerTous.setDisable(!actif);
//        btnEnregistrerAffectations.setDisable(!actif);
//
//        if (!actif) {
//            lblMatiereSelectionnee.setText("Sélectionnez une matière");
//            accordionNiveaux.getPanes().clear();
//            casesParCleNiveau.clear();
//            champsCoefficient.clear();
//        }
//    }
//
//    /* ============================================================
//       Chargement des matières
//       ============================================================ */
//    private void chargerMatieres() {
//        matiereService.listerParEcoleAsync(idEcoleCourante,
//                matieres -> Platform.runLater(() -> {
//                    FusionListeUtil.fusionner(listMatieres.getItems(), matieres, Matiere::getId);
//                    listMatieres.setPlaceholder(new Label("Aucune matière enregistrée"));
//
//                    if (!matieres.isEmpty() && listMatieres.getSelectionModel().isEmpty()) {
//                        listMatieres.getSelectionModel().selectFirst();
//                    }
//                }),
//                erreur -> Platform.runLater(erreur::printStackTrace));
//    }
//
//    /* ============================================================
//       Recherche
//       ============================================================ */
//    private void lancerRecherche() {
//        if (idEcoleCourante == null) return;
//
//        matiereService.rechercherAsync(idEcoleCourante, getSearchText(),
//                matieres -> Platform.runLater(() -> {
//                    FusionListeUtil.fusionner(listMatieres.getItems(), matieres, Matiere::getId);
//
//                    if (matieres.isEmpty()) {
//                        listMatieres.setPlaceholder(new Label("Aucun résultat"));
//                    } else {
//                        listMatieres.setPlaceholder(new Label("Aucune matière enregistrée"));
//                    }
//
//                    if (matiereSelectionnee != null
//                            && matieres.stream().noneMatch(m ->
//                            m.getId().equals(matiereSelectionnee.getId()))) {
//                        matiereSelectionnee = null;
//                        activerPanneauDroit(false);
//                    }
//                }),
//                erreur -> Platform.runLater(erreur::printStackTrace));
//    }
//
//    private String getSearchText() {
//        String t = fieldRecherche.getText();
//        return (t == null || t.isBlank()) ? null : t.trim();
//    }
//
//    /* ============================================================
//       Chargement des affectations → construction de l'accordéon
//       ============================================================ */
//    private void chargerAffectationsMatiere(Matiere matiere) {
//        activerPanneauDroit(true);
//        lblMatiereSelectionnee.setText(matiere.getNomMatiere() != null
//                ? matiere.getNomMatiere()
//                : matiere.getCodeMatiere());
//
//        matiereNiveauService.obtenirAffectationsParCycleAsync(
//                idEcoleCourante, matiere.getId(),
//                parCycle -> Platform.runLater(() -> construireAccordion(parCycle)),
//                erreur -> Platform.runLater(erreur::printStackTrace));
//    }
//
//    /* ============================================================
//       Construction dynamique de l'accordéon
//       ============================================================ */
//    private void construireAccordion(Map<String, List<MatiereNiveau>> parCycle) {
//        enChargement = true;
//
//        accordionNiveaux.getPanes().clear();
//        casesParCleNiveau.clear();
//        champsCoefficient.clear();
//
//        List<String> ordreCycles = List.of("PREMIER_CYCLE", "SECOND_CYCLE", "PRIMAIRE");
//
//        for (String cycle : ordreCycles) {
//            List<MatiereNiveau> affectations = parCycle.get(cycle);
//            if (affectations == null || affectations.isEmpty()) continue;
//
//            affectations.sort(Comparator
//                    .comparing((MatiereNiveau mn) ->
//                            mn.getNomNiveau() != null ? mn.getNomNiveau() : "")
//                    .thenComparing(mn ->
//                            mn.getCodeFiliere() != null ? mn.getCodeFiliere() : ""));
//
//            VBox corps = new VBox(6);
//            corps.setPadding(new Insets(8, 4, 8, 4));
//            corps.getStyleClass().add("permissions-module-body");
//
//            for (MatiereNiveau mn : affectations) {
//                corps.getChildren().add(construireLigneAffectation(mn));
//            }
//
//            TitledPane pane = new TitledPane(libelleCycle(cycle), corps);
//            pane.getStyleClass().add("permission-titled-pane");
//            accordionNiveaux.getPanes().add(pane);
//        }
//
//        if (!accordionNiveaux.getPanes().isEmpty()) {
//            accordionNiveaux.setExpandedPane(accordionNiveaux.getPanes().get(0));
//        }
//
//        enChargement = false;
//    }
//
//    private String libelleCycle(String code) {
//        return switch (code) {
//            case "PREMIER_CYCLE" -> "Premier cycle";
//            case "SECOND_CYCLE"  -> "Second cycle";
//            case "PRIMAIRE"      -> "Maternelle / Primaire";
//            default              -> code;
//        };
//    }
//
//    private HBox construireLigneAffectation(MatiereNiveau mn) {
//        String cle = cleAffectation(mn.getIdNiveau(), mn.getIdFiliere());
//
//        CheckBox checkObligatoire = new CheckBox();
//        checkObligatoire.setSelected(mn.isObligatoire());
//        checkObligatoire.getStyleClass().add("permission-check");
//        checkObligatoire.selectedProperty().addListener((obs, a, n) -> {
//            if (!enChargement) signalerModification();
//        });
//
//        Label libelleNiveau = new Label(libelleNiveau(mn));
//        libelleNiveau.getStyleClass().add("card-title");
//
//        Region spacer = new Region();
//        HBox.setHgrow(spacer, Priority.ALWAYS);
//
//        Label lblCoeff = new Label("Coeff.");
//        lblCoeff.getStyleClass().add("field-label");
//
//        TextField champCoeff = new TextField(
//                mn.getCoefficient() != null ? mn.getCoefficient().toPlainString() : "1");
//        champCoeff.getStyleClass().add("input");
//        champCoeff.setPrefWidth(60);
//        champCoeff.textProperty().addListener((obs, a, n) -> {
//            if (!enChargement) signalerModification();
//        });
//
//        HBox ligne = new HBox(10, checkObligatoire, libelleNiveau, spacer, lblCoeff, champCoeff);
//        ligne.setAlignment(Pos.CENTER_LEFT);
//        ligne.setPadding(new Insets(6, 0, 6, 0));
//
//        casesParCleNiveau.put(cle, checkObligatoire);
//        champsCoefficient.put(cle, champCoeff);
//
//        return ligne;
//    }
//
//    private String cleAffectation(String idNiveau, String idFiliere) {
//        return idNiveau + "#" + (idFiliere != null ? idFiliere : "_");
//    }
//
//    private String libelleNiveau(MatiereNiveau mn) {
//        StringBuilder sb = new StringBuilder();
//        sb.append(mn.getNomNiveau() != null ? mn.getNomNiveau() : mn.getIdNiveau());
//        if (mn.getCodeFiliere() != null && !mn.getCodeFiliere().isBlank()) {
//            sb.append(" — ").append(mn.getCodeFiliere());
//        }
//        return sb.toString();
//    }
//
//    private void signalerModification() {
//        if (btnEnregistrerAffectations != null) {
//            btnEnregistrerAffectations.setDisable(false);
//        }
//    }
//
//    /* ============================================================
//       Enregistrement des affectations
//       ============================================================ */
//    private void enregistrerAffectations() {
//        if (matiereSelectionnee == null) return;
//
//        List<LigneAffectationNiveau> lignes = new ArrayList<>();
//
//        for (Map.Entry<String, CheckBox> entry : casesParCleNiveau.entrySet()) {
//            String cle = entry.getKey();
//            CheckBox check = entry.getValue();
//            TextField champCoeff = champsCoefficient.get(cle);
//
//            String[] parties = cle.split("#");
//            String idNiveau  = parties[0];
//            String idFiliere = "_".equals(parties[1]) ? null : parties[1];
//
//            BigDecimal coeff = parseCoefficient(champCoeff.getText());
//
//            lignes.add(new LigneAffectationNiveau(
//                    idNiveau, idFiliere, coeff, check.isSelected()));
//        }
//
//        Stage stage = StageHelper.getStage(btnEnregistrerAffectations);
//        btnEnregistrerAffectations.setDisable(true);
//
//        matiereNiveauService.enregistrerAffectationsPourMatiereAsync(
//                idEcoleCourante, matiereSelectionnee.getId(), lignes,
//                resultat -> Platform.runLater(() -> {
//                    btnEnregistrerAffectations.setDisable(false);
//                    gererResultatEnregistrement(stage, resultat);
//                    if (matiereSelectionnee != null) {
//                        chargerAffectationsMatiere(matiereSelectionnee);
//                    }
//                }),
//                erreur -> Platform.runLater(() -> {
//                    btnEnregistrerAffectations.setDisable(false);
//                    ToastNotification.erreur(stage, "Échec de l'enregistrement des affectations.");
//                    System.err.println("Erreur enregistrement affectations : " + erreur.getMessage());
//                }));
//    }
//
//    private void gererResultatEnregistrement(Stage stage, ResultatAffectationsNiveau resultat) {
//        if (resultat == null) return;
//
//        if (resultat.estSucces()) {
//            ToastNotification.succes(stage,
//                    "Enregistré : " + resultat.getEnregistrees()
//                            + " affectation(s), " + resultat.getRetirees() + " retrait(s).");
//        } else {
//            String message = String.join("\n", resultat.getErreurs());
//            AlertUtil.afficherErreur("Enregistrement partiel", message, stage);
//        }
//
//        auditService.tracerActionAsync(
//                "ACADEMIQUE",
//                "MODIFICATION_AFFECTATIONS_MATIERE",
//                "Matière « " + matiereSelectionnee.getNomMatiere()
//                        + " » — " + resultat.getEnregistrees() + " affectation(s), "
//                        + resultat.getRetirees() + " retrait(s)"
//        );
//    }
//
//    private BigDecimal parseCoefficient(String texte) {
//        if (texte == null || texte.isBlank()) return BigDecimal.ONE;
//        try {
//            return new BigDecimal(texte.trim().replace(",", "."));
//        } catch (NumberFormatException e) {
//            return BigDecimal.ONE;
//        }
//    }
//
//    /* ============================================================
//       Appliquer à tous les niveaux
//       ============================================================ */
//    private void appliquerATousLesNiveaux() {
//        if (matiereSelectionnee == null) return;
//
//        Stage stage = StageHelper.getStage(chkAppliquerTous);
//
//        if (!chkAppliquerTous.isSelected()) return;
//
//        boolean confirme = AlertUtil.afficherConfirmation(
//                "Appliquer à tous les niveaux",
//                "Voulez-vous vraiment affecter \"" + matiereSelectionnee.getNomMatiere()
//                        + "\" à tous les niveaux avec un coefficient de 1 ?",
//                stage
//        );
//
//        if (!confirme) {
//            chkAppliquerTous.setSelected(false);
//            return;
//        }
//
//        matiereNiveauService.appliquerATousLesNiveauxAsync(
//                idEcoleCourante, matiereSelectionnee.getId(), BigDecimal.ONE,
//                resultat -> Platform.runLater(() -> {
//                    chkAppliquerTous.setSelected(false);
//                    ToastNotification.succes(stage,
//                            "Affectée à " + resultat.getEnregistrees() + " niveau(x).");
//                    chargerAffectationsMatiere(matiereSelectionnee);
//                }),
//                erreur -> Platform.runLater(() -> {
//                    chkAppliquerTous.setSelected(false);
//                    ToastNotification.erreur(stage, "Échec de l'application globale.");
//                    System.err.println("Erreur application globale : " + erreur.getMessage());
//                }));
//    }
//
//    /* ============================================================
//       Modale création / édition (via ModalUtil)
//       ============================================================ */
//    private void ouvrirModalCreation() {
//        String fxml = "/org/erpklassup/erpklassup/view/module2/ModalMatiere.fxml";
//
//        MatiereModalController ctrl = ModalUtil.ouvrirModal(
//                getClass(),
//                fxml,
//                i18n().t("matiere.modal.create.title"),
//                listMatieres.getScene().getWindow(),
//                500, 575
//        );
//
//        if (ctrl != null) {
//            ctrl.initCreation(idEcoleCourante, () -> {
//                chargerMatieres();
//                if (autoRefreshMatieres != null) autoRefreshMatieres.restartNow();
//            });
//        }
//    }
//
//    private void ouvrirModalEdition(Matiere matiere) {
//        if (matiere == null) return;
//
//        String fxml = "/org/erpklassup/erpklassup/view/module2/ModalMatiere.fxml";
//
//        MatiereModalController ctrl = ModalUtil.ouvrirModal(
//                getClass(),
//                fxml,
//                i18n().t("matiere.modal.edit.title"),
//                listMatieres.getScene().getWindow(),
//                500, 575
//        );
//
//        if (ctrl != null) {
//            ctrl.initEdition(idEcoleCourante, matiere, () -> {
//                chargerMatieres();
//                if (autoRefreshMatieres != null) autoRefreshMatieres.restartNow();
//            });
//        }
//    }
//
//    /* ============================================================
//       Suppression d'une matière (via AlertUtil)
//       ============================================================ */
//    private void supprimerMatiere(Matiere matiere) {
//        if (matiere == null) return;
//
//        Stage stage = StageHelper.getStage(listMatieres);
//
//        boolean confirme = AlertUtil.afficherConfirmation(
//                i18n().t("matiere.confirm_delete.title"),
//                i18n().t("matiere.confirm_delete.message", matiere.getNomMatiere()),
//                stage
//        );
//
//        if (!confirme) return;
//
//        matiereService.supprimerAsync(matiere.getId(),
//                () -> Platform.runLater(() -> {
//                    auditService.tracerActionAsync(
//                            "ACADEMIQUE",
//                            "SUPPRESSION_MATIERE",
//                            "Suppression de la matière : " + matiere.getNomMatiere()
//                    );
//                    ToastNotification.succes(stage,
//                            i18n().t("matiere.toast.deleted", matiere.getNomMatiere()));
//                    chargerMatieres();
//                    if (autoRefreshMatieres != null) autoRefreshMatieres.restartNow();
//                }),
//                erreur -> Platform.runLater(() -> {
//                    ToastNotification.erreur(stage,
//                            i18n().t("matiere.error.delete_failed"));
//                    System.err.println("❌ Suppression matière : " + erreur.getMessage());
//                }));
//    }
//
//    /* ============================================================
//       Cellule personnalisée avec boutons Éditer / Supprimer
//       ============================================================ */
//    private class CelluleMatiere extends ListCell<Matiere> {
//        private final Label code      = new Label();
//        private final Label nom       = new Label();
//        private final VBox  conteneur = new VBox(2, nom, code);
//
//        CelluleMatiere() {
//            nom.getStyleClass().add("cell-title");
//            code.getStyleClass().add("cell-subtitle");
//
//            FontIcon iconEdit = new FontIcon("fth-edit-2");
//            iconEdit.setIconSize(14);
//
//            FontIcon iconDelete = new FontIcon("fth-trash-2");
//            iconDelete.setIconSize(14);
//        }
//
//        @Override
//        protected void updateItem(Matiere m, boolean vide) {
//            super.updateItem(m, vide);
//            if (vide || m == null) {
//                setGraphic(null);
//                setText(null);
//                return;
//            }
//            nom.setText(m.getNomMatiere() != null ? m.getNomMatiere() : "—");
//            code.setText(m.getCodeMatiere() != null ? m.getCodeMatiere() : "");
//
//            // Action de suppression visible uniquement si la matière n'a pas d'enfants
//            // (géré côté service si besoin).
//            setGraphic(conteneur);
//            setText(null);
//        }
//    }
//}
