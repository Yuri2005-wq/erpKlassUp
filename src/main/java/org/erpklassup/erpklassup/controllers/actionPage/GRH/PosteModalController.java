package org.erpklassup.erpklassup.controllers.actionPage.GRH;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.dao.GRH.DepartementDAO;
import org.erpklassup.erpklassup.models.GRH.Departement;
import org.erpklassup.erpklassup.models.GRH.Poste;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.GRH.PosteService;
import org.erpklassup.erpklassup.util.ControleAcces;
import org.erpklassup.erpklassup.util.ToastNotification;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

import static org.erpklassup.erpklassup.util.ValidationUtil.forcerCasseTitre;
import static org.erpklassup.erpklassup.util.ValidationUtil.forcerMajuscules;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class PosteModalController implements Initializable {

    /* ============================================================
       FXML
       ============================================================ */
    @FXML private FontIcon iconModal;
    @FXML private Label lblTitreModal;
    @FXML private Label lblSousTitreModal;

    @FXML private TextField txtIntitule;
    @FXML private ComboBox<DepartementItem> comboDepartement;
    @FXML private ComboBox<String> comboCategorie;
    @FXML private TextField txtDescription;
    @FXML private CheckBox chkEstActif;

    @FXML private Label errIntitule;
    @FXML private Label errDepartement;
    @FXML private Label errGlobal;

    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;

    /* ===========================================================
       SERVICES & ÉTAT
       ============================================================ */
    private final PosteService posteService = new PosteService();
    private final DepartementDAO departementDAO = new DepartementDAO();
    private final AuditService auditService = new AuditService();

    private String idEcoleCourante;
    private Poste posteEnEdition; // null si création
    private Runnable onSucces;
    private boolean modeConsultation = false;

    /* ============================================================
       INITIALISATION
       ============================================================ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Remplir le ComboBox des catégories
        comboCategorie.setItems(FXCollections.observableArrayList(
                "Administration", "Enseignement", "Encadrement", "Technique"
        ));
        ValidationChamp();
        // Focus initial
        Platform.runLater(() -> txtIntitule.requestFocus());
    }

    private void chargerDepartements(String idEcole, String idDepartementSelectionne) {
        if (idEcole == null) return;
        List<Departement> depts = departementDAO.findByEcole(idEcole);

        ObservableList<DepartementItem> items = FXCollections.observableArrayList();
        items.add(new DepartementItem(null, "— Aucun département —"));
        for (Departement d : depts) {
            items.add(new DepartementItem(d.getIdDepartement(), d.getLibelle()));
        }
        comboDepartement.setItems(items);

        if (idDepartementSelectionne != null) {
            for (DepartementItem item : items) {
                if (item.id() != null && item.id().equals(idDepartementSelectionne)) {
                    comboDepartement.getSelectionModel().select(item);
                    break;
                }
            }
        } else {
            comboDepartement.getSelectionModel().selectFirst();
        }
    }

    private void ValidationChamp(){
        forcerCasseTitre(txtDescription);
        forcerCasseTitre(txtIntitule);
    }

    /* ============================================================
       API PUBLIQUE — Appelée par PosteController
       ============================================================ */

    public void initDataForCreate(String idEcole, Runnable onSucces) {
        this.idEcoleCourante = idEcole;
        this.posteEnEdition = null;
        this.onSucces = onSucces;
        this.modeConsultation = false;

        lblTitreModal.setText("Nouveau poste");
        lblSousTitreModal.setText("Renseignez les informations du poste professionnel");
        iconModal.setIconLiteral("fas-user-tie");

        chkEstActif.setSelected(true);
        chargerDepartements(idEcole, null);
        appliquerControlesAcces();
    }

    public void initDataForUpdate(Poste poste, Runnable onSucces) {
        if (poste == null) return;

        this.idEcoleCourante = poste.getIdEcole();
        this.posteEnEdition = poste;
        this.onSucces = onSucces;
        this.modeConsultation = false;

        lblTitreModal.setText("Modifier le poste");
        lblSousTitreModal.setText("Mettez à jour les informations du poste");
        iconModal.setIconLiteral("fth-edit-2");

        txtIntitule.setText(poste.getIntitule());
        txtDescription.setText(poste.getDescription());
        chkEstActif.setSelected(poste.isEstActif());

        if (poste.getCategorie() != null) {
            comboCategorie.getSelectionModel().select(poste.getCategorie());
        }

        chargerDepartements(idEcoleCourante, poste.getIdDepartement());
        appliquerControlesAcces();
    }

    public void initDataVoir(Poste poste, Runnable onSucces) {
        if (poste == null) return;

        this.idEcoleCourante = poste.getIdEcole();
        this.posteEnEdition = poste;
        this.onSucces = onSucces;
        this.modeConsultation = true;

        lblTitreModal.setText("Consulter le poste");
        lblSousTitreModal.setText("Détails du poste professionnel");
        iconModal.setIconLiteral("fth-eye");

        txtIntitule.setText(poste.getIntitule());
        txtDescription.setText(poste.getDescription());
        chkEstActif.setSelected(poste.isEstActif());
        chkEstActif.setDisable(true);

        if (poste.getCategorie() != null) {
            comboCategorie.getSelectionModel().select(poste.getCategorie());
        }
        comboCategorie.setDisable(true);
        txtIntitule.setEditable(false);
        txtDescription.setEditable(false);
        comboDepartement.setDisable(true);

        chargerDepartements(idEcoleCourante, poste.getIdDepartement());

        // Masquer le bouton Enregistrer en mode consultation pure
        btnEnregistrer.setVisible(false);
        btnEnregistrer.setManaged(false);
        btnAnnuler.setText("Fermer");
    }

    /* ============================================================
       CONTRÔLES D'ACCÈS
       ============================================================ */
    private void appliquerControlesAcces() {
        if (modeConsultation) return;

        String actionRequise = (posteEnEdition == null) ? "grh.poste.creer" : "grh.poste.modifier";
        boolean autorise = ControleAcces.autoriseAction(actionRequise);

        // Si le système de permissions n'est pas strict, on laisse true par défaut
        btnEnregistrer.setVisible(true);
        btnEnregistrer.setManaged(true);
    }

    /* ============================================================
       ACTIONS
       ============================================================ */

    @FXML
    private void handleAnnuler() {
        fermer();
    }

    @FXML
    private void handleEnregistrer() {
        if (modeConsultation) {
            fermer();
            return;
        }

        if (!validerFormulaire()) return;

        Poste poste = (posteEnEdition == null) ? new Poste() : posteEnEdition;

        if (posteEnEdition == null) {
            poste.setIdPoste(UUID.randomUUID().toString());
            poste.setIdEcole(idEcoleCourante);
        }

        poste.setIntitule(txtIntitule.getText().trim());
        poste.setDescription(txtDescription.getText() != null ? txtDescription.getText().trim() : null);
        poste.setCategorie(comboCategorie.getSelectionModel().getSelectedItem());
        poste.setEstActif(chkEstActif.isSelected());

        DepartementItem selDept = comboDepartement.getSelectionModel().getSelectedItem();
        poste.setIdDepartement(selDept != null ? selDept.id() : null);

        btnEnregistrer.setDisable(true);

        if (posteEnEdition == null) {
            posteService.creerPosteAsync(poste,
                    () -> Platform.runLater(this::onCreationReussie),
                    err -> Platform.runLater(() -> onErreur(err)));
        } else {
            posteService.modifierPosteAsync(poste,
                    () -> Platform.runLater(this::onEditionReussie),
                    err -> Platform.runLater(() -> onErreur(err)));
        }
    }

    private void onCreationReussie() {
        btnEnregistrer.setDisable(false);

        auditService.tracerActionAsync("GRH", "CREATION_POSTE", "Poste créé : " + txtIntitule.getText().trim());
        ToastNotification.succes(getStage(), "Poste « " + txtIntitule.getText().trim() + " » créé avec succès.");

        if (onSucces != null) onSucces.run();
        fermer();
    }

    private void onEditionReussie() {
        btnEnregistrer.setDisable(false);

        auditService.tracerActionAsync("GRH", "MODIFICATION_POSTE", "Poste modifié : " + txtIntitule.getText().trim());
        ToastNotification.succes(getStage(), "Poste « " + txtIntitule.getText().trim() + " » modifié avec succès.");

        if (onSucces != null) onSucces.run();
        fermer();
    }

    private void onErreur(Throwable err) {
        btnEnregistrer.setDisable(false);
        String msg = (err != null && err.getMessage() != null) ? err.getMessage() : "Une erreur est survenue.";

        errGlobal.setText(msg);
        errGlobal.setVisible(true);
        errGlobal.setManaged(true);
    }

    /* ============================================================
       VALIDATION
       ============================================================ */
    private boolean validerFormulaire() {
        cacherToutesErreurs();
        boolean valide = true;

        String intitule = txtIntitule.getText();
        if (intitule == null || intitule.isBlank()) {
            afficherErreur(errIntitule, "L'intitulé du poste est obligatoire.");
            valide = false;
        }

        return valide;
    }

    private void afficherErreur(Label label, String message) {
        if (label == null) return;
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void cacherToutesErreurs() {
        for (Label l : new Label[]{errIntitule, errDepartement, errGlobal}) {
            if (l != null) {
                l.setVisible(false);
                l.setManaged(false);
            }
        }
    }

    /* ============================================================
       HELPERS & RECORD INTERNE POUR COMBOBOX
       ============================================================ */
    private void fermer() {
        Stage stage = getStage();
        if (stage != null) stage.close();
    }

    private Stage getStage() {
        if (btnAnnuler.getScene() == null) return null;
        return (Stage) btnAnnuler.getScene().getWindow();
    }

    private record DepartementItem(String id, String libelle) {
        @Override
        public String toString() {
            return libelle;
        }
    }
}