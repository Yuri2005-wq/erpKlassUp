package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.erpklassup.erpklassup.models.module2.AnneeScolaire;
import org.erpklassup.erpklassup.models.module2.Classe;
import org.erpklassup.erpklassup.models.module2.Filiere;
import org.erpklassup.erpklassup.models.module2.Niveau;
import org.erpklassup.erpklassup.models.module2.Salle;
import org.erpklassup.erpklassup.service.module2.AcademiqueService;
import org.erpklassup.erpklassup.service.module2.AnneeScolaireService;
import org.erpklassup.erpklassup.service.module2.ClasseService;
import org.erpklassup.erpklassup.service.module2.SalleService;
import org.erpklassup.erpklassup.util.ToastNotification;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

public class ClasseModalController implements Initializable {

    /* ============================================================
       FXML
       ============================================================ */
    @FXML private FontIcon iconModal;
    @FXML private Label lblTitreModal;
    @FXML private Label lblSousTitreModal;

    @FXML private TextField txtCodeClasse;
    @FXML private TextField txtCapacite;
    @FXML private TextField txtNomClasse;

    @FXML private ComboBox<Niveau>          comboNiveau;
    @FXML private ComboBox<Filiere>         comboFiliere;
    @FXML private ComboBox<Classe.Vacation> comboVacation;
    @FXML private ComboBox<String>          comboTitulaire;
    @FXML private ComboBox<Salle>           comboSalle;

    @FXML private Label errCodeClasse;
    @FXML private Label errCapacite;
    @FXML private Label errNomClasse;
    @FXML private Label errNiveau;
    @FXML private Label errVacation;
    @FXML private Label errGlobal;

    @FXML private Button btnAnnuler;
    @FXML private Button btnEnregistrer;

    /* ============================================================
       SERVICES
       ============================================================ */
    private final ClasseService        classeService     = new ClasseService();
    private final AcademiqueService    academiqueService = new AcademiqueService();
    private final AnneeScolaireService anneeService      = new AnneeScolaireService();
    private final SalleService         salleService      = new SalleService();

    /* ============================================================
       ÉTAT
       ============================================================ */
    private String   idEcole;
    private String   idAnnee;
    private Classe   classeExistante;
    private Runnable onSuccess;

    /* ============================================================
       INITIALISATION
       ============================================================ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerCombos();
        cacherErreurs();
        Platform.runLater(txtCodeClasse::requestFocus);
    }

    private void configurerCombos() {
        /* ---------- Vacation ---------- */
        ObservableList<Classe.Vacation> vacations =
                FXCollections.observableArrayList(Classe.Vacation.values());

        StringConverter<Classe.Vacation> convVac = new StringConverter<>() {
            @Override public String toString(Classe.Vacation v) {
                if (v == null) return "";
                return v.getLibelle();
            }
            @Override public Classe.Vacation fromString(String s) { return null; }
        };
        comboVacation.setConverter(convVac);
        comboVacation.setButtonCell(boutonCellule(convVac));
        comboVacation.setItems(vacations);
        comboVacation.getSelectionModel().select(Classe.Vacation.JOURNEE_CONTINUE);

        /* ---------- Titulaire (placeholder) ---------- */
        comboTitulaire.setItems(FXCollections.observableArrayList("— Aucun —"));
        comboTitulaire.getSelectionModel().selectFirst();
    }

    /* ============================================================
       API PUBLIQUE
       ============================================================ */
    public void initData(String idEcole, String idAnnee,
                         Classe classeExistante, Runnable onSuccess) {
        this.idEcole         = idEcole;
        this.classeExistante = classeExistante;
        this.onSuccess       = onSuccess;

        System.out.println("🟡 [ClasseModal] initData — idEcole=" + idEcole
                + " | idAnnee=" + idAnnee
                + " | mode=" + (classeExistante == null ? "CREATION" : "EDITION"));

        if (idAnnee != null && !idAnnee.isBlank()) {
            this.idAnnee = idAnnee;
            chargerReferentiels();
            if (classeExistante != null) remplirFormulaire(classeExistante);
            return;
        }

        chargerAnneeCourantePuisReferentiels();
    }

    /* ============================================================
       CHARGEMENT ANNÉE COURANTE AUTO
       ============================================================ */
    private void chargerAnneeCourantePuisReferentiels() {
        if (idEcole == null) {
            afficherErreurGlobale("Aucune école active — impossible de créer une classe.");
            return;
        }

        anneeService.chargerCouranteAsync(idEcole,
                optAnnee -> Platform.runLater(() -> {
                    if (optAnnee.isEmpty()) {
                        afficherErreurGlobale(
                                "Aucune année scolaire active. Définissez-en une dans « Années scolaires ».");
                        return;
                    }
                    AnneeScolaire annee = optAnnee.get();
                    this.idAnnee = annee.getId();

                    lblSousTitreModal.setText("Année scolaire : " + annee.getLibelle());

                    chargerReferentiels();
                    if (classeExistante != null) remplirFormulaire(classeExistante);
                }),
                erreur -> Platform.runLater(() -> {
                    System.err.println("❌ Année courante : " + erreur.getMessage());
                    afficherErreurGlobale("Impossible de charger l'année scolaire courante.");
                }));
    }

    /* ============================================================
       CHARGEMENT RÉFÉRENTIELS (Niveaux, Filières, Salles)
       ============================================================ */
    private void chargerReferentiels() {
        System.out.println("🟡 [ClasseModal] chargerReferentiels — idEcole=" + idEcole);

        /* ---------- NIVEAUX (SANS JETON) ---------- */
        academiqueService.listerNiveauxParEcoleSansJetonAsync(idEcole,
                niveaux -> Platform.runLater(() -> {
                    System.out.println("📚 [ClasseModal] Niveaux reçus : "
                            + (niveaux == null ? "null" : niveaux.size()));

                    ObservableList<Niveau> liste = FXCollections.observableArrayList();
                    if (niveaux != null) liste.addAll(niveaux);

                    StringConverter<Niveau> conv = new StringConverter<>() {
                        @Override public String toString(Niveau n) {
                            if (n == null) return "— Choisir un niveau —";
                            return n.getNomNiveau() != null ? n.getNomNiveau() : "Niveau sans nom";
                        }
                        @Override public Niveau fromString(String s) { return null; }
                    };
                    comboNiveau.setConverter(conv);
                    comboNiveau.setButtonCell(boutonCellule(conv));
                    comboNiveau.setItems(liste);

                    // Sélection en mode édition
                    if (classeExistante != null && classeExistante.getIdNiveau() != null) {
                        liste.stream()
                                .filter(n -> n.getId().equals(classeExistante.getIdNiveau()))
                                .findFirst()
                                .ifPresent(comboNiveau.getSelectionModel()::select);
                    }
                }),
                err -> Platform.runLater(() -> {
                    System.err.println("❌ [ClasseModal] Niveaux : " + err.getMessage());
                    err.printStackTrace();
                }));

        /* ---------- FILIÈRES (SANS JETON) ---------- */
        academiqueService.listerFilieresParEcoleSansJetonAsync(idEcole,
                filieres -> Platform.runLater(() -> {
                    System.out.println("🎓 [ClasseModal] Filières reçues : "
                            + (filieres == null ? "null" : filieres.size()));

                    ObservableList<Filiere> liste = FXCollections.observableArrayList();
                    liste.add(null); // "Aucune"
                    if (filieres != null) liste.addAll(filieres);

                    StringConverter<Filiere> conv = new StringConverter<>() {
                        @Override public String toString(Filiere f) {
                            return f == null ? "— Aucune —" : f.getCodeFiliere();
                        }
                        @Override public Filiere fromString(String s) { return null; }
                    };
                    comboFiliere.setConverter(conv);
                    comboFiliere.setButtonCell(boutonCellule(conv));
                    comboFiliere.setItems(liste);
                    comboFiliere.getSelectionModel().selectFirst();

                    if (classeExistante != null && classeExistante.getIdFiliere() != null) {
                        liste.stream()
                                .filter(f -> f != null && f.getId().equals(classeExistante.getIdFiliere()))
                                .findFirst()
                                .ifPresent(comboFiliere.getSelectionModel()::select);
                    }
                }),
                err -> Platform.runLater(() -> {
                    System.err.println("❌ [ClasseModal] Filières : " + err.getMessage());
                    err.printStackTrace();
                }));

        /* ---------- SALLES ---------- */
        salleService.listerSallesAsync(idEcole,
                salles -> Platform.runLater(() -> {
                    System.out.println("🏫 [ClasseModal] Salles reçues : "
                            + (salles == null ? "null" : salles.size()));

                    ObservableList<Salle> liste = FXCollections.observableArrayList();
                    liste.add(null); // "Aucune"
                    if (salles != null) liste.addAll(salles);

                    StringConverter<Salle> conv = new StringConverter<>() {
                        @Override public String toString(Salle s) {
                            return s == null ? "— Aucune —"
                                    : s.getCodeSalle() + " (" + s.getNomSalle() + ")";
                        }
                        @Override public Salle fromString(String s) { return null; }
                    };
                    comboSalle.setConverter(conv);
                    comboSalle.setButtonCell(boutonCellule(conv));
                    comboSalle.setItems(liste);
                    comboSalle.getSelectionModel().selectFirst();

                    if (classeExistante != null && classeExistante.getIdSalle() != null) {
                        liste.stream()
                                .filter(s -> s != null && s.getId().equals(classeExistante.getIdSalle()))
                                .findFirst()
                                .ifPresent(comboSalle.getSelectionModel()::select);
                    }
                }),
                err -> Platform.runLater(() -> {
                    System.err.println("❌ [ClasseModal] Salles : " + err.getMessage());
                    err.printStackTrace();
                }));
    }

    /* ============================================================
       REMPLISSAGE (MODE ÉDITION)
       ============================================================ */
    private void remplirFormulaire(Classe c) {
        lblTitreModal.setText("Modifier la classe");
        lblSousTitreModal.setText("Année scolaire : "
                + (c.getLibelleAnnee() != null ? c.getLibelleAnnee() : idAnnee));
        iconModal.setIconLiteral("fas-edit");

        txtCodeClasse.setText(c.getCodeClasse());
        txtNomClasse.setText(c.getNomClasse());
        txtCapacite.setText(String.valueOf(c.getCapacite()));

        if (c.getVacation() != null) {
            comboVacation.getSelectionModel().select(c.getVacation());
        }
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
        // =================================================
        // CONTROLE D'ACCES (commenté pour le moment)
        // =================================================
        // String action = (classeExistante == null) ? "classe.creer" : "classe.modifier";
        // if (!ControleAcces.autoriseAction(action)) {
        //     ToastNotification.avertissement(getStage(), "Permission refusée.");
        //     return;
        // }

        if (!validerFormulaire()) return;

        Classe c = (classeExistante != null) ? classeExistante : new Classe();

        c.setIdEcole(idEcole);
        c.setIdAnnee(idAnnee);
        c.setCodeClasse(txtCodeClasse.getText().trim());
        c.setNomClasse(txtNomClasse.getText().trim());
        c.setCapacite(Integer.parseInt(txtCapacite.getText().trim()));
        c.setVacation(comboVacation.getSelectionModel().getSelectedItem());

        Niveau niveau = comboNiveau.getSelectionModel().getSelectedItem();
        if (niveau != null) c.setIdNiveau(niveau.getId());

        Filiere filiere = comboFiliere.getSelectionModel().getSelectedItem();
        c.setIdFiliere(filiere != null ? filiere.getId() : null);

        Salle salle = comboSalle.getSelectionModel().getSelectedItem();
        c.setIdSalle(salle != null ? salle.getId() : null);

        c.setIdEnseignantTitulaire(null);

        boolean estCreation = (classeExistante == null);
        if (estCreation && (c.getId() == null || c.getId().isBlank())) {
            c.setId(java.util.UUID.randomUUID().toString());
        }

        btnEnregistrer.setDisable(true);
        btnAnnuler.setDisable(true);
        cacherErreurs();

        Runnable onSucces = () -> Platform.runLater(() -> {
            btnEnregistrer.setDisable(false);
            btnAnnuler.setDisable(false);
            ToastNotification.succes(getStage(),
                    estCreation ? "Classe créée." : "Classe mise à jour.");
            if (onSuccess != null) onSuccess.run();
            fermer();
        });

        java.util.function.Consumer<Throwable> onErreur = err -> Platform.runLater(() -> {
            btnEnregistrer.setDisable(false);
            btnAnnuler.setDisable(false);
            gererErreurEnregistrement(err);
        });

        if (estCreation) {
            classeService.creerAsync(c, onSucces, onErreur);
        } else {
            classeService.modifierAsync(c, onSucces, onErreur);
        }
    }

    /* ============================================================
       VALIDATION
       ============================================================ */
    private boolean validerFormulaire() {
        cacherErreurs();
        boolean ok = true;

        if (isBlank(txtCodeClasse.getText())) {
            afficherErreur(errCodeClasse, "Le code classe est obligatoire.");
            ok = false;
        }
        if (isBlank(txtNomClasse.getText())) {
            afficherErreur(errNomClasse, "Le nom de la classe est obligatoire.");
            ok = false;
        }
        if (isBlank(txtCapacite.getText())) {
            afficherErreur(errCapacite, "La capacité est obligatoire.");
            ok = false;
        } else {
            try {
                int cap = Integer.parseInt(txtCapacite.getText().trim());
                if (cap <= 0) {
                    afficherErreur(errCapacite, "La capacité doit être supérieure à 0.");
                    ok = false;
                }
            } catch (NumberFormatException e) {
                afficherErreur(errCapacite, "La capacité doit être un nombre valide.");
                ok = false;
            }
        }
        if (comboNiveau.getSelectionModel().getSelectedItem() == null) {
            afficherErreur(errNiveau, "Le niveau est obligatoire.");
            ok = false;
        }
        if (comboVacation.getSelectionModel().getSelectedItem() == null) {
            afficherErreur(errVacation, "La vacation est obligatoire.");
            ok = false;
        }
        if (idAnnee == null) {
            afficherErreurGlobale("Aucune année scolaire active — impossible d'enregistrer.");
            ok = false;
        }

        return ok;
    }

    private void gererErreurEnregistrement(Throwable erreur) {
        System.err.println("❌ Erreur enregistrement classe : " + erreur.getMessage());
        if (erreur != null) erreur.printStackTrace();

        String msg = (erreur != null && erreur.getMessage() != null)
                ? erreur.getMessage()
                : "Échec de l'enregistrement de la classe.";

        if (msg.toLowerCase().contains("existe")) {
            afficherErreur(errCodeClasse, msg);
        } else {
            afficherErreurGlobale(msg);
        }
    }

    /* ============================================================
       AFFICHAGE DES ERREURS
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
        for (Label l : new Label[]{
                errCodeClasse, errNomClasse, errCapacite,
                errNiveau, errVacation, errGlobal}) {
            if (l != null) { l.setVisible(false); l.setManaged(false); }
        }
    }

    /* ============================================================
       FERMETURE
       ============================================================ */
    private void fermer() {
        Stage stage = getStage();
        if (stage != null) stage.close();
    }

    private Stage getStage() {
        if (btnAnnuler == null || btnAnnuler.getScene() == null) return null;
        return (Stage) btnAnnuler.getScene().getWindow();
    }

    /* ============================================================
       UTILITAIRES
       ============================================================ */
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
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
}