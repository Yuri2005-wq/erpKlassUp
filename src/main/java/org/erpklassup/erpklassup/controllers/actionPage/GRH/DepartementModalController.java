package org.erpklassup.erpklassup.controllers.actionPage.GRH;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.erpklassup.erpklassup.enums.SousSysteme;
import org.erpklassup.erpklassup.enums.StatutDepartement;
import org.erpklassup.erpklassup.enums.TypeDepartement;
import org.erpklassup.erpklassup.models.GRH.Departement;
import org.erpklassup.erpklassup.models.GRH.Personnel;
import org.erpklassup.erpklassup.service.GRH.DepartementService;
import org.erpklassup.erpklassup.dao.PersonnelDAO;
import org.erpklassup.erpklassup.util.ToastNotification;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

import static org.erpklassup.erpklassup.util.ValidationUtil.*;

public class DepartementModalController implements Initializable {

    /* ============================================================
       CHAMPS FXML
       ============================================================ */
    @FXML private FontIcon iconModal;
    @FXML private Label lblTitreModal;
    @FXML private Label lblSousTitreModal;

    @FXML private TextField txtCode;
    @FXML private TextField txtLibelle;
    @FXML private TextField txtCodeMineduc;
    @FXML private TextField txtBudgetAnnuel;
    @FXML private TextField txtEffectifMax;
    @FXML private TextArea txtDescription;
    @FXML private CheckBox chkConseilEnseignement;

    @FXML private ComboBox<TypeDepartement> comboType;
    @FXML private ComboBox<SousSysteme> comboSousSysteme;
    @FXML private ComboBox<Departement> comboDepartementParent;
    @FXML private ComboBox<Personnel> comboResponsable;
    @FXML private ComboBox<Personnel> comboResponsableAdjoint;
    @FXML private ComboBox<StatutDepartement> comboStatut;

    @FXML private Label errCode;
    @FXML private Label errLibelle;
    @FXML private Label errType;
    @FXML private Label errSousSysteme;
    @FXML private Label errBudget;
    @FXML private Label errGlobal;

    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;

    /* ============================================================
       SERVICES
       ============================================================ */
    private final DepartementService departementService = new DepartementService();
    private final PersonnelDAO personnelDAO = new PersonnelDAO();

    /* ============================================================
       ÉTAT
       ============================================================ */
    private String idEcole;
    private Departement departementExistant;
    private Runnable onSuccess;

    /* ============================================================
       INITIALISATION
       ============================================================ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerCombosEnums();
        cacherErreurs();
        validerChamp();
        Platform.runLater(txtCode::requestFocus);
    }

    private void validerChamp(){
        forcerMajuscules(txtCode);
        forcerCasseTitre(txtLibelle);
        forcerMajuscules(txtCodeMineduc);
        forcerDecimal(txtBudgetAnnuel);
        forcerEntier(txtEffectifMax);
    }

    private void configurerCombosEnums() {
        /* ---------- Type de Département ---------- */
        ObservableList<TypeDepartement> types = FXCollections.observableArrayList(TypeDepartement.values());
        StringConverter<TypeDepartement> convType = new StringConverter<>() {
            @Override public String toString(TypeDepartement t) { return t == null ? "" : t.getLibelle(); }
            @Override public TypeDepartement fromString(String s) { return null; }
        };
        comboType.setConverter(convType);
        comboType.setButtonCell(creerListCell(convType));
        comboType.setItems(types);
        comboType.getSelectionModel().select(TypeDepartement.ADMINISTRATIF);

        /* ---------- Sous-Système ---------- */
        ObservableList<SousSysteme> sousSystemes = FXCollections.observableArrayList(SousSysteme.values());
        StringConverter<SousSysteme> convSousSyst = new StringConverter<>() {
            @Override public String toString(SousSysteme s) { return s == null ? "" : s.getLibelle(); }
            @Override public SousSysteme fromString(String s) { return null; }
        };
        comboSousSysteme.setConverter(convSousSyst);
        comboSousSysteme.setButtonCell(creerListCell(convSousSyst));
        comboSousSysteme.setItems(sousSystemes);
        comboSousSysteme.getSelectionModel().select(SousSysteme.BILINGUE);

        /* ---------- Statut ---------- */
        ObservableList<StatutDepartement> statuts = FXCollections.observableArrayList(StatutDepartement.values());
        StringConverter<StatutDepartement> convStatut = new StringConverter<>() {
            @Override public String toString(StatutDepartement st) { return st == null ? "" : st.getLibelle(); }
            @Override public StatutDepartement fromString(String s) { return null; }
        };
        comboStatut.setConverter(convStatut);
        comboStatut.setButtonCell(creerListCell(convStatut));
        comboStatut.setItems(statuts);
        comboStatut.getSelectionModel().select(StatutDepartement.ACTIF);
    }

    /* ============================================================
       API PUBLIQUE D'INITIALISATION
       ============================================================ */
    public void initDataForCreate(String idEcole, Runnable onSuccess) {
        this.idEcole = idEcole;
        this.departementExistant = null;
        this.onSuccess = onSuccess;

        lblTitreModal.setText("Nouveau département");
        lblSousTitreModal.setText("Créer une nouvelle entité dans la structure organisationnelle");
        iconModal.setIconLiteral("fas-sitemap");

        chargerReferentiels();
    }

    public void initDataForUpdate(Departement departement, Runnable onSuccess) {
        this.departementExistant = departement;
        this.idEcole = departement.getIdEcole();
        this.onSuccess = onSuccess;

        lblTitreModal.setText("Modifier le département");
        lblSousTitreModal.setText("Département : " + departement.getLibelle());
        iconModal.setIconLiteral("fas-edit");

        chargerReferentiels();
        remplirFormulaire(departement);
    }


    public void initDataVoir(Departement departement, Runnable onSuccess) {
        this.departementExistant = departement;
        this.idEcole = departement.getIdEcole();
        this.onSuccess = onSuccess;

        lblTitreModal.setText("Détail sur le département");
        lblSousTitreModal.setText("Département : " + departement.getLibelle());
        iconModal.setIconLiteral("fas-edit");

        chargerReferentiels();
        remplirFormulaire(departement);

        appliquerModeLectureSeule();
    }

    /* ============================================================
       CHARGEMENT ASYNCHRONE DES RÉFÉRENTIELS
       ============================================================ */
    private void chargerReferentiels() {
        if (idEcole == null) {
            afficherErreurGlobale("impossible d'effectuer l'opération.");
            return;
        }

        /* ---------- DEPARTEMENTS PARENTS ---------- */
        departementService.chargerTousParEcoleAsync(idEcole,
                parents -> Platform.runLater(() -> {
                    ObservableList<Departement> liste = FXCollections.observableArrayList();
                    liste.add(null); // Option racine "Aucun"
                    if (parents != null) {
                        // Filtre pour ne pas afficher le département lui-même comme parent potentiel
                        parents.stream()
                                .filter(d -> departementExistant == null || !d.getIdDepartement().equals(departementExistant.getIdDepartement()))
                                .forEach(liste::add);
                    }

                    StringConverter<Departement> convParent = new StringConverter<>() {
                        @Override public String toString(Departement d) { return d == null ? "— Aucun (Département Racine) —" : d.getLibelle(); }
                        @Override public Departement fromString(String s) { return null; }
                    };
                    comboDepartementParent.setConverter(convParent);
                    comboDepartementParent.setButtonCell(creerListCell(convParent));
                    comboDepartementParent.setItems(liste);
                    comboDepartementParent.getSelectionModel().selectFirst();

                    if (departementExistant != null && departementExistant.getIdDepartementParent() != null) {
                        liste.stream()
                                .filter(p -> p != null && p.getIdDepartement().equals(departementExistant.getIdDepartementParent()))
                                .findFirst()
                                .ifPresent(comboDepartementParent.getSelectionModel()::select);
                    }
                }),
                err -> Platform.runLater(() -> System.err.println("❌ Erreur chargement départements parents : " + err.getMessage()))
        );

        /* ---------- PERSONNEL (RESPONSABLES) ---------- */
        // Exécution asynchrone sur la base du DAO personnel
        org.erpklassup.erpklassup.service.AppExecutor.get().submit(() -> {
            try {
                var personnels = personnelDAO.findByEcole(idEcole);
                Platform.runLater(() -> {
                    ObservableList<Personnel> liste = FXCollections.observableArrayList();
                    liste.add(null); // Option "Non assigné"
                    if (personnels != null) liste.addAll(personnels);

                    StringConverter<Personnel> convPers = new StringConverter<>() {
                        @Override public String toString(Personnel p) {
                            return p == null ? "— Non assigné —" : (p.getNom() + " " + (p.getPrenom() != null ? p.getPrenom() : "")).trim();
                        }
                        @Override public Personnel fromString(String s) { return null; }
                    };

                    comboResponsable.setConverter(convPers);
                    comboResponsable.setButtonCell(creerListCell(convPers));
                    comboResponsable.setItems(liste);
                    comboResponsable.getSelectionModel().selectFirst();

                    comboResponsableAdjoint.setConverter(convPers);
                    comboResponsableAdjoint.setButtonCell(creerListCell(convPers));
                    comboResponsableAdjoint.setItems(liste);
                    comboResponsableAdjoint.getSelectionModel().selectFirst();

                    // Sélection des responsables en édition
                    if (departementExistant != null) {
                        if (departementExistant.getIdResponsable() != null) {
                            liste.stream()
                                    .filter(p -> p != null && p.getIdPersonnel().equals(departementExistant.getIdResponsable()))
                                    .findFirst()
                                    .ifPresent(comboResponsable.getSelectionModel()::select);
                        }
                        if (departementExistant.getIdResponsableAdjoint() != null) {
                            liste.stream()
                                    .filter(p -> p != null && p.getIdPersonnel().equals(departementExistant.getIdResponsableAdjoint()))
                                    .findFirst()
                                    .ifPresent(comboResponsableAdjoint.getSelectionModel()::select);
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> System.err.println("❌ Erreur chargement personnel : " + e.getMessage()));
            }
        });
    }

    /* ============================================================
       REMPLISSAGE (MODE ÉDITION)
       ============================================================ */
    private void remplirFormulaire(Departement d) {
        txtCode.setText(d.getCode());
        txtLibelle.setText(d.getLibelle());
        txtCodeMineduc.setText(d.getCodeMineduc());
        txtDescription.setText(d.getDescription());
        chkConseilEnseignement.setSelected(Boolean.TRUE.equals(d.getEstConseilEnseignement()));

        if (d.getBudgetAnnuel() != null) {
            txtBudgetAnnuel.setText(d.getBudgetAnnuel().toPlainString());
        }
        if (d.getEffectifMax() != null) {
            txtEffectifMax.setText(String.valueOf(d.getEffectifMax()));
        }

        if (d.getType() != null) comboType.getSelectionModel().select(d.getType());
        if (d.getSousSysteme() != null) comboSousSysteme.getSelectionModel().select(d.getSousSysteme());
        if (d.getStatut() != null) comboStatut.getSelectionModel().select(d.getStatut());
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
        if (!validerFormulaire()) return;

        Departement d = (departementExistant != null) ? departementExistant : new Departement();

        d.setIdEcole(idEcole);
        d.setCode(txtCode.getText().trim());
        d.setLibelle(txtLibelle.getText().trim());
        d.setType(comboType.getSelectionModel().getSelectedItem());
        d.setSousSysteme(comboSousSysteme.getSelectionModel().getSelectedItem());
        d.setStatut(comboStatut.getSelectionModel().getSelectedItem());
        d.setEstConseilEnseignement(chkConseilEnseignement.isSelected());
        d.setCodeMineduc(isBlank(txtCodeMineduc.getText()) ? null : txtCodeMineduc.getText().trim());
        d.setDescription(isBlank(txtDescription.getText()) ? null : txtDescription.getText().trim());

        // Budget
        if (!isBlank(txtBudgetAnnuel.getText())) {
            d.setBudgetAnnuel(new BigDecimal(txtBudgetAnnuel.getText().trim()));
        } else {
            d.setBudgetAnnuel(null);
        }

        // Effectif Max
        if (!isBlank(txtEffectifMax.getText())) {
            d.setEffectifMax(Integer.parseInt(txtEffectifMax.getText().trim()));
        } else {
            d.setEffectifMax(null);
        }

        // Parent & Responsables
        Departement parent = comboDepartementParent.getSelectionModel().getSelectedItem();
        d.setIdDepartementParent(parent != null ? parent.getIdDepartement() : null);

        Personnel resp = comboResponsable.getSelectionModel().getSelectedItem();
        d.setIdResponsable(resp != null ? resp.getIdPersonnel() : null);

        Personnel respAdj = comboResponsableAdjoint.getSelectionModel().getSelectedItem();
        d.setIdResponsableAdjoint(respAdj != null ? respAdj.getIdPersonnel() : null);

        boolean estCreation = (departementExistant == null);
        if (estCreation && (d.getIdDepartement() == null || d.getIdDepartement().isBlank())) {
            d.setIdDepartement(java.util.UUID.randomUUID().toString());
        }

        btnEnregistrer.setDisable(true);
        btnAnnuler.setDisable(true);
        cacherErreurs();

        Runnable onSuccesHandler = () -> Platform.runLater(() -> {
            btnEnregistrer.setDisable(false);
            btnAnnuler.setDisable(false);
            ToastNotification.succes(getStage(), estCreation ? "Département créé avec succès." : "Département mis à jour.");
            if (onSuccess != null) onSuccess.run();
            fermer();
        });

        java.util.function.Consumer<Throwable> onErreurHandler = err -> Platform.runLater(() -> {
            btnEnregistrer.setDisable(false);
            btnAnnuler.setDisable(false);
            gererErreurEnregistrement(err);
        });

        if (estCreation) {
            departementService.creerDepartementAsync(d, onSuccesHandler, onErreurHandler);
        } else {
            departementService.modifierDepartementAsync(d, onSuccesHandler, onErreurHandler);
        }
    }

    /* ============================================================
       VALIDATION DU FORMULAIRE
       ============================================================ */
    private boolean validerFormulaire() {
        cacherErreurs();
        boolean ok = true;

        if (isBlank(txtCode.getText())) {
            afficherErreur(errCode, "Le code du département est obligatoire.");
            ok = false;
        }
        if (isBlank(txtLibelle.getText())) {
            afficherErreur(errLibelle, "Le nom du département est obligatoire.");
            ok = false;
        }
        if (comboType.getSelectionModel().getSelectedItem() == null) {
            afficherErreur(errType, "Veuillez choisir un type.");
            ok = false;
        }
        if (comboSousSysteme.getSelectionModel().getSelectedItem() == null) {
            afficherErreur(errSousSysteme, "Veuillez choisir un sous-système.");
            ok = false;
        }

        if (!isBlank(txtBudgetAnnuel.getText())) {
            try {
                BigDecimal budget = new BigDecimal(txtBudgetAnnuel.getText().trim());
                if (budget.compareTo(BigDecimal.ZERO) < 0) {
                    afficherErreur(errBudget, "Le budget ne peut pas être négatif.");
                    ok = false;
                }
            } catch (NumberFormatException e) {
                afficherErreur(errBudget, "Montant de budget invalide.");
                ok = false;
            }
        }

        return ok;
    }

    private void gererErreurEnregistrement(Throwable erreur) {
        String msg = (erreur != null && erreur.getMessage() != null)
                ? erreur.getMessage()
                : "Échec de l'enregistrement du département.";

        if (msg.toLowerCase().contains("code")) {
            afficherErreur(errCode, msg);
        } else if (msg.toLowerCase().contains("libellé") || msg.toLowerCase().contains("nom")) {
            afficherErreur(errLibelle, msg);
        } else {
            afficherErreurGlobale(msg);
        }
    }

    /* ============================================================
       GESTION DES AFFICHAGES D'ERREURS
       ============================================================ */
    private void afficherErreur(Label label, String msg) {
        if (label == null) return;
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void afficherErreurGlobale(String msg) {
        if (errGlobal == null) return;
        errGlobal.setText(msg);
        errGlobal.setVisible(true);
        errGlobal.setManaged(true);
    }

    private void cacherErreurs() {
        for (Label l : new Label[]{errCode, errLibelle, errType, errSousSysteme, errBudget, errGlobal}) {
            if (l != null) {
                l.setVisible(false);
                l.setManaged(false);
            }
        }
    }

    /* ============================================================
       UTILITAIRES
       ============================================================ */
    private void fermer() {
        Stage stage = getStage();
        if (stage != null) stage.close();
    }

    private Stage getStage() {
        if (btnAnnuler == null || btnAnnuler.getScene() == null) return null;
        return (Stage) btnAnnuler.getScene().getWindow();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private <T> ListCell<T> creerListCell(StringConverter<T> converter) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : converter.toString(item));
            }
        };
    }

    /* ============================================================
   MODE LECTURE SEULE (VUE DÉTAIL)
   ============================================================ */
    private void appliquerModeLectureSeule() {
        // Champs texte
        txtCode.setEditable(false);
        txtLibelle.setEditable(false);
        txtCodeMineduc.setEditable(false);
        txtBudgetAnnuel.setEditable(false);
        txtEffectifMax.setEditable(false);
        txtDescription.setEditable(false);

        // ComboBox : on empêche l'ouverture du popup ET la saisie
        for (ComboBox<?> cb : new ComboBox<?>[]{
                comboType, comboSousSysteme, comboDepartementParent,
                comboResponsable, comboResponsableAdjoint, comboStatut}) {
            cb.setDisable(true);
        }

        // CheckBox
        chkConseilEnseignement.setDisable(true);

        // Bouton enregistrer
        btnEnregistrer.setVisible(false);
        btnEnregistrer.setManaged(false);

        // Bouton annuler -> devient "Fermer"
        btnAnnuler.setText("Fermer");
    }
}