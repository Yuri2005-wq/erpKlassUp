package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.erpklassup.erpklassup.models.module2.GroupeMatiere;
import org.erpklassup.erpklassup.models.module2.Matiere;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.SessionManager;
import org.erpklassup.erpklassup.service.module2.AcademiqueService;
import org.erpklassup.erpklassup.service.module2.MatiereService;
import org.erpklassup.erpklassup.util.I18nManager;
import org.erpklassup.erpklassup.util.ToastNotification;

import static org.erpklassup.erpklassup.util.ValidationUtil.forcerCasseTitre;

/**
 * Controller de la modale de création / édition d'une {@link Matiere}.
 *
 * <p>Pattern identique à {@code RoleModalController} :
 * <ul>
 *   <li>{@code initCreation(...)} / {@code initEdition(...)} appelés par le parent</li>
 *   <li>Validation locale avant appel service</li>
 *   <li>Callbacks async onSucces / onErreur</li>
 *   <li>Fermeture de la Stage en cas de succès + callback parent</li>
 * </ul>
 */
public class MatiereModalController {

    /* ============================================================
       FXML
       ============================================================ */
    @FXML private Label     lblTitreModal;
    @FXML private Label     lblSousTitreModal;
    @FXML private TextField txtCode;
    @FXML private TextField txtNom;
    @FXML private TextField txtDomaine;
    @FXML private TextField txtOrdre;
    @FXML private ComboBox<Matiere>       comboMatiereParente;
    @FXML private ComboBox<GroupeMatiere> comboGroupe;
    @FXML private Button    btnAnnuler;
    @FXML private Button    btnEnregistrer;

    /* ============================================================
       SERVICES
       ============================================================ */
    private final MatiereService    matiereService    = new MatiereService();
    private final AcademiqueService academiqueService = new AcademiqueService();
    private final AuditService      auditService      = new AuditService();
    private final SessionManager    session           = SessionManager.getInstance();

    /* ============================================================
       ÉTAT
       ============================================================ */
    private String  idEcole;
    private Matiere matiereAEditer;      // null si création
    private Runnable onSuccessCallback;

    private I18nManager i18n() {
        return I18nManager.getInstance();
    }

    /* ============================================================
       INITIALISATION CRÉATION
       ============================================================ */
    public void initCreation(String idEcole, Runnable onSuccess) {
        this.idEcole = idEcole;
        this.matiereAEditer = null;
        this.onSuccessCallback = onSuccess;

        if (lblTitreModal != null)     lblTitreModal.setText(i18n().t("matiere.modal.create.title"));
        if (lblSousTitreModal != null) lblSousTitreModal.setText(i18n().t("matiere.modal.create.subtitle"));
        if (btnEnregistrer != null)    btnEnregistrer.setText(i18n().t("matiere.modal.create.button"));

        if (txtCode != null) txtCode.clear();
        if (txtNom != null) txtNom.clear();
        if (txtDomaine != null) txtDomaine.clear();
        if (txtOrdre != null) txtOrdre.setText("0");
        if (comboMatiereParente != null) comboMatiereParente.setValue(null);
        if (comboGroupe != null) comboGroupe.setValue(null);

        forcerCasseTitre(txtNom);
        chargerReferentiels();
    }

    /* ============================================================
       INITIALISATION ÉDITION
       ============================================================ */
    public void initEdition(String idEcole, Matiere matiere, Runnable onSuccess) {
        this.idEcole = idEcole;
        this.matiereAEditer = matiere;
        this.onSuccessCallback = onSuccess;

        if (lblTitreModal != null)     lblTitreModal.setText(i18n().t("matiere.modal.edit.title"));
        if (lblSousTitreModal != null) lblSousTitreModal.setText(i18n().t("matiere.modal.edit.subtitle"));
        if (btnEnregistrer != null)    btnEnregistrer.setText(i18n().t("common.save"));

        if (txtCode != null)    txtCode.setText(matiere.getCodeMatiere() != null ? matiere.getCodeMatiere() : "");
        if (txtNom != null)     txtNom.setText(matiere.getNomMatiere() != null ? matiere.getNomMatiere() : "");
        if (txtDomaine != null) txtDomaine.setText(matiere.getDomaine() != null ? matiere.getDomaine() : "");
        if (txtOrdre != null)   txtOrdre.setText(String.valueOf(matiere.getOrdreAffichage()));

        forcerCasseTitre(txtNom);
        chargerReferentiels();
    }

    /* ============================================================
       CHARGEMENT DES RÉFÉRENTIELS (groupes + matières parentes)
       ============================================================ */
    private void chargerReferentiels() {
        // Groupes de matières
        academiqueService.listerGroupesMatiereParEcoleAsync(idEcole,
                groupes -> Platform.runLater(() -> {
                    ObservableList<GroupeMatiere> liste = FXCollections.observableArrayList();
                    liste.add(null);
                    if (groupes != null) liste.addAll(groupes);
                    comboGroupe.setItems(liste);
                    comboGroupe.setConverter(new StringConverter<>() {
                        @Override public String toString(GroupeMatiere g) {
                            return g == null ? i18n().t("matiere.modal.no_group") : g.getNomGroupe();
                        }
                        @Override public GroupeMatiere fromString(String s) { return null; }
                    });

                    // Sélection en mode édition
                    if (matiereAEditer != null && matiereAEditer.getIdGroupeMatiere() != null) {
                        liste.stream()
                                .filter(g -> g != null
                                        && g.getIdGroupe().equals(matiereAEditer.getIdGroupeMatiere()))
                                .findFirst()
                                .ifPresent(comboGroupe.getSelectionModel()::select);
                    } else {
                        comboGroupe.getSelectionModel().selectFirst();
                    }
                }),
                err -> Platform.runLater(err::printStackTrace));

        // Matières parentes
        matiereService.listerParEcoleAsync(idEcole,
                matieres -> Platform.runLater(() -> {
                    ObservableList<Matiere> liste = FXCollections.observableArrayList();
                    liste.add(null);
                    if (matieres != null) {
                        // On exclut la matière en cours d'édition (pas parente d'elle-même)
                        matieres.stream()
                                .filter(m -> matiereAEditer == null
                                        || !m.getId().equals(matiereAEditer.getId()))
                                .forEach(liste::add);
                    }
                    comboMatiereParente.setItems(liste);
                    comboMatiereParente.setConverter(new StringConverter<>() {
                        @Override public String toString(Matiere m) {
                            return m == null ? i18n().t("matiere.modal.no_parent") : m.getNomMatiere();
                        }
                        @Override public Matiere fromString(String s) { return null; }
                    });

                    if (matiereAEditer != null && matiereAEditer.getIdMatiereParent() != null) {
                        liste.stream()
                                .filter(m -> m != null
                                        && m.getId().equals(matiereAEditer.getIdMatiereParent()))
                                .findFirst()
                                .ifPresent(comboMatiereParente.getSelectionModel()::select);
                    } else {
                        comboMatiereParente.getSelectionModel().selectFirst();
                    }
                }),
                err -> Platform.runLater(err::printStackTrace));
    }

    /* ============================================================
       ACTIONS
       ============================================================ */
    @FXML
    private void handleEnregistrer() {
        String code = txtCode != null && txtCode.getText() != null ? txtCode.getText().trim() : "";
        String nom  = txtNom  != null && txtNom.getText()  != null ? txtNom.getText().trim()  : "";
        String domaine = txtDomaine != null && txtDomaine.getText() != null
                ? txtDomaine.getText().trim() : null;
        int ordre = parseEntier(txtOrdre != null ? txtOrdre.getText() : "0");

        Stage stage = (Stage) btnEnregistrer.getScene().getWindow();

        // ---------- Validation ----------
        if (code.isBlank()) {
            ToastNotification.avertissement(stage, i18n().t("matiere.error.code_required"));
            return;
        }
        if (nom.isBlank()) {
            ToastNotification.avertissement(stage, i18n().t("matiere.error.name_required"));
            return;
        }
        if (idEcole == null || idEcole.isBlank()) {
            ToastNotification.erreur(stage, i18n().t("matiere.error.no_school"));
            return;
        }

        // ---------- Construction du modèle ----------
        Matiere matiere = (matiereAEditer != null) ? matiereAEditer : new Matiere();
        matiere.setIdEcole(idEcole);
        matiere.setCodeMatiere(code);
        matiere.setNomMatiere(nom);
        matiere.setDomaine(domaine);
        matiere.setOrdreAffichage(ordre);

        // Parent
        if (comboMatiereParente.getValue() != null) {
            String idParent = comboMatiereParente.getValue().getId();
            if (idParent != null && !idParent.isBlank()) {
                matiere.setIdMatiereParent(idParent);
            }
        } else {
            matiere.setIdMatiereParent(null);
        }

        // Groupe
        if (comboGroupe.getValue() != null) {
            String idGroupe = comboGroupe.getValue().getIdGroupe();
            if (idGroupe != null && !idGroupe.isBlank()) {
                matiere.setIdGroupeMatiere(idGroupe);
            }
        } else {
            matiere.setIdGroupeMatiere(null);
        }

        btnEnregistrer.setDisable(true);

        if (matiereAEditer == null) {
            // ==============================
            // MODE CRÉATION
            // ==============================
            matiereService.creerAsync(matiere,
                    () -> Platform.runLater(() -> {
                        auditService.tracerActionAsync(
                                "ACADEMIQUE", "CREATION_MATIERE",
                                "Création de la matière : " + nom);
                        ToastNotification.succes(stage,
                                i18n().t("matiere.toast.created", nom));
                        fermerEtRafraichir();
                    }),
                    erreur -> Platform.runLater(() -> {
                        btnEnregistrer.setDisable(false);
                        ToastNotification.erreur(stage,
                                i18n().t("common.error_detail", erreur.getMessage()));
                    }));
        } else {
            // ==============================
            // MODE MODIFICATION
            // ==============================
            matiereService.modifierAsync(matiere,
                    () -> Platform.runLater(() -> {
                        auditService.tracerActionAsync(
                                "ACADEMIQUE", "MODIFICATION_MATIERE",
                                "Matière modifiée : " + nom);
                        ToastNotification.succes(stage,
                                i18n().t("matiere.toast.updated"));
                        fermerEtRafraichir();
                    }),
                    erreur -> Platform.runLater(() -> {
                        btnEnregistrer.setDisable(false);
                        ToastNotification.erreur(stage,
                                i18n().t("matiere.error.update_failed"));
                    }));
        }
    }

    @FXML
    private void handleAnnuler() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }

    /* ============================================================
       UTILITAIRES
       ============================================================ */
    private void fermerEtRafraichir() {
        if (onSuccessCallback != null) onSuccessCallback.run();
        Stage stage = (Stage) btnEnregistrer.getScene().getWindow();
        stage.close();
    }

    private int parseEntier(String texte) {
        if (texte == null || texte.isBlank()) return 0;
        try { return Integer.parseInt(texte.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}