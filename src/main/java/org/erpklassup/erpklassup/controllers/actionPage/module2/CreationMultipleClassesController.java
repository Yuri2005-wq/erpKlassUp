package org.erpklassup.erpklassup.controllers.actionPage.module2;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.erpklassup.erpklassup.dto.ResultatCreationMasse;
import org.erpklassup.erpklassup.models.module2.Classe;
import org.erpklassup.erpklassup.models.module2.Niveau;
import org.erpklassup.erpklassup.models.module2.Salle;
import org.erpklassup.erpklassup.service.AuditService;
import org.erpklassup.erpklassup.service.module2.AcademiqueService;
import org.erpklassup.erpklassup.service.module2.AnneeScolaireService;
import org.erpklassup.erpklassup.service.module2.ClasseService;
import org.erpklassup.erpklassup.service.module2.SalleService;
import org.erpklassup.erpklassup.util.AlertUtil;
import org.erpklassup.erpklassup.util.StageHelper;
import org.erpklassup.erpklassup.util.ToastNotification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreationMultipleClassesController {

    /* FXML */
    @javafx.fxml.FXML private Label                       lblAnneeScolaire;
    @javafx.fxml.FXML private ComboBox<Classe.Vacation>   comboVacation;
    @javafx.fxml.FXML private CheckBox                    chkSelectionnerTout;
    @javafx.fxml.FXML private VBox                        conteneurNiveaux;
    @javafx.fxml.FXML private Label                       lblApercuResume;
    @javafx.fxml.FXML private Label                       lblApercuDetails;
    @javafx.fxml.FXML private Button                      btnAnnuler;
    @javafx.fxml.FXML private Button                      btnCreer;

    /* Services */
    private final ClasseService        classeService     = new ClasseService();
    private final AcademiqueService    academiqueService = new AcademiqueService();
    private final AnneeScolaireService anneeService      = new AnneeScolaireService();
    private final SalleService         salleService      = new SalleService();
    private final AuditService         auditService      = new AuditService();

    /* État */
    private String idEcole;
    private String idAnnee;
    private String libelleAnnee;
    private Runnable onSuccess;

    private ObservableList<Salle> sallesDisponibles = FXCollections.observableArrayList();

    /** Pour chaque niveau : sa ligne UI. */
    private final Map<Niveau, LigneNiveau> lignesParNiveau = new LinkedHashMap<>();

    /* ============================================================
       Classe interne — une ligne du tableau
       ============================================================ */
    private static class LigneNiveau {
        Niveau            niveau;
        CheckBox          chk;
        Spinner<Integer>  spinNb;
        Spinner<Integer>  spinCap;
        TextField         txtPrefixe;
        ComboBox<Salle>   comboSalle;
        HBox              conteneur;
    }

    /* ============================================================
       API PUBLIQUE
       ============================================================ */
    public void initData(String idEcole, String idAnnee, Runnable onSuccess) {
        this.idEcole   = idEcole;
        this.idAnnee   = idAnnee;
        this.onSuccess = onSuccess;

        configurerVacation();
        configurerSelectionTout();
        configurerBoutons();

        chargerAnneePuisSallesEtNiveaux();
    }

    /* ============================================================
       VACATION
       ============================================================ */
    private void configurerVacation() {
        comboVacation.setItems(FXCollections.observableArrayList(Classe.Vacation.values()));

        StringConverter<Classe.Vacation> conv = new StringConverter<>() {
            @Override public String toString(Classe.Vacation v) {
                return v == null ? "" : v.getLibelle();
            }
            @Override public Classe.Vacation fromString(String s) { return null; }
        };
        comboVacation.setConverter(conv);
        comboVacation.setButtonCell(boutonCellule(conv));
        comboVacation.getSelectionModel().select(Classe.Vacation.JOURNEE_CONTINUE);

        comboVacation.getSelectionModel().selectedItemProperty()
                .addListener((obs, a, n) -> mettreAJourApercu());
    }

    /* ============================================================
       CHARGEMENT
       ============================================================ */
    private void chargerAnneePuisSallesEtNiveaux() {
        anneeService.chargerCouranteAsync(idEcole,
                optAnnee -> Platform.runLater(() -> {
                    if (optAnnee.isEmpty()) {
                        AlertUtil.afficherInformation(
                                "Aucune année active",
                                "Aucune année scolaire n'est active. Impossible de créer des classes.",
                                StageHelper.getStage(btnAnnuler));
                        fermer();
                        return;
                    }
                    this.idAnnee      = optAnnee.get().getId();
                    this.libelleAnnee = optAnnee.get().getLibelle();
                    lblAnneeScolaire.setText(libelleAnnee);

                    chargerSallesPuisNiveaux();
                }),
                err -> Platform.runLater(() -> {
                    ToastNotification.erreur(StageHelper.getStage(btnAnnuler),
                            "Erreur de chargement de l'année.");
                    fermer();
                }));
    }

    private void chargerSallesPuisNiveaux() {
        salleService.listerSallesAsync(idEcole,
                salles -> Platform.runLater(() -> {
                    sallesDisponibles.setAll(salles != null ? salles : List.of());
                    chargerNiveaux();
                }),
                err -> Platform.runLater(() -> {
                    System.err.println("Erreur salles : " + err.getMessage());
                    chargerNiveaux();
                }));
    }

    private void chargerNiveaux() {
        academiqueService.listerNiveauxParEcoleSansJetonAsync(idEcole,
                niveaux -> Platform.runLater(() -> {
                    conteneurNiveaux.getChildren().clear();
                    lignesParNiveau.clear();

                    if (niveaux == null || niveaux.isEmpty()) {
                        Label vide = new Label("Aucun niveau disponible pour cette école");
                        vide.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
                        conteneurNiveaux.getChildren().add(vide);
                        mettreAJourApercu();
                        return;
                    }

                    for (Niveau n : niveaux) {
                        LigneNiveau ligne = construireLigneNiveau(n);
                        lignesParNiveau.put(n, ligne);
                        conteneurNiveaux.getChildren().add(ligne.conteneur);
                    }

                    mettreAJourApercu();
                }),
                err -> Platform.runLater(() -> {
                    ToastNotification.erreur(StageHelper.getStage(btnAnnuler),
                            "Erreur de chargement des niveaux.");
                }));
    }

    /* ============================================================
       CONSTRUCTION D'UNE LIGNE
       ============================================================ */
    private LigneNiveau construireLigneNiveau(Niveau niveau) {
        LigneNiveau l = new LigneNiveau();
        l.niveau = niveau;

        /* 1) Checkbox */
        l.chk = new CheckBox();
        l.chk.setPrefWidth(30);
        l.chk.selectedProperty().addListener((obs, a, n) -> {
            l.spinNb.setDisable(!n);
            l.spinCap.setDisable(!n);
            l.txtPrefixe.setDisable(!n);
            l.comboSalle.setDisable(!n);

            if (n) {
                l.conteneur.setStyle("-fx-background-color: #EFF6FF; -fx-background-radius: 8;");
            } else {
                l.conteneur.setStyle("");
            }
            mettreAJourApercu();
        });

        /* 2) Nom du niveau */
        Label lblNom = new Label(niveau.getNomNiveau());
        lblNom.setPrefWidth(140);
        lblNom.setStyle("-fx-font-weight: 600; -fx-text-fill: #1E293B;");

        /* 3) Spinner nb */
        l.spinNb = new Spinner<>(1, 20, 1);
        l.spinNb.setPrefWidth(80);
        l.spinNb.setEditable(true);
        l.spinNb.setDisable(true);
        l.spinNb.valueProperty().addListener((obs, a, n) -> mettreAJourApercu());

        /* 4) Spinner capacité */
        l.spinCap = new Spinner<>(10, 100, 40);
        l.spinCap.setPrefWidth(90);
        l.spinCap.setEditable(true);
        l.spinCap.setDisable(true);

        /* 5) Préfixe */
        l.txtPrefixe = new TextField(suggererPrefixe(niveau));
        l.txtPrefixe.setPrefWidth(70);
        l.txtPrefixe.setDisable(true);
        l.txtPrefixe.textProperty().addListener((obs, a, n) -> mettreAJourApercu());

        /* 6) ComboBox salle */
        l.comboSalle = new ComboBox<>();
        l.comboSalle.setPrefWidth(220);
        l.comboSalle.setDisable(true);

        ObservableList<Salle> options = FXCollections.observableArrayList();
        options.add(null); // "Aucune"
        options.addAll(sallesDisponibles);
        l.comboSalle.setItems(options);

        StringConverter<Salle> convSalle = new StringConverter<>() {
            @Override public String toString(Salle s) {
                if (s == null) return "— Aucune salle —";
                return s.getCodeSalle() + " — " + s.getNomSalle();
            }
            @Override public Salle fromString(String s) { return null; }
        };
        l.comboSalle.setConverter(convSalle);
        l.comboSalle.setButtonCell(boutonCellule(convSalle));
        l.comboSalle.getSelectionModel().selectFirst();

        /* 7) HBox */
        l.conteneur = new HBox(10, l.chk, lblNom, l.spinNb, l.spinCap, l.txtPrefixe, l.comboSalle);
        l.conteneur.setAlignment(Pos.CENTER_LEFT);
        l.conteneur.setPadding(new Insets(6, 8, 6, 8));

        HBox.setHgrow(lblNom, Priority.SOMETIMES);
        HBox.setHgrow(l.comboSalle, Priority.SOMETIMES);

        return l;
    }

    private String suggererPrefixe(Niveau niveau) {
        String nom = niveau.getNomNiveau();
        if (nom == null || nom.isBlank()) return "CLS";
        String nettoye = nom.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (nettoye.length() <= 3) return nettoye;

        StringBuilder sb = new StringBuilder();
        for (char c : nettoye.toCharArray()) {
            if (Character.isDigit(c) || "BCDFGHJKLMNPQRSTVWXYZ".indexOf(c) >= 0) {
                sb.append(c);
            }
            if (sb.length() >= 3) break;
        }
        return sb.length() > 0 ? sb.toString() : nettoye.substring(0, 3);
    }

    /* ============================================================
       TOUT SÉLECTIONNER
       ============================================================ */
    private void configurerSelectionTout() {
        chkSelectionnerTout.setOnAction(ev -> {
            boolean coche = chkSelectionnerTout.isSelected();
            for (LigneNiveau ligne : lignesParNiveau.values()) {
                ligne.chk.setSelected(coche);
            }
        });
    }

    /* ============================================================
       APERÇU
       ============================================================ */
    private void mettreAJourApercu() {
        List<String> codesGeneres = new ArrayList<>();
        int total = 0;
        List<String> alertesSalles = new ArrayList<>();

        Classe.Vacation vacation = comboVacation.getSelectionModel().getSelectedItem();

        for (LigneNiveau ligne : lignesParNiveau.values()) {
            if (!ligne.chk.isSelected()) continue;

            int nb = ligne.spinNb.getValue();
            String prefixe = ligne.txtPrefixe.getText() != null
                    ? ligne.txtPrefixe.getText().trim() : "";
            if (prefixe.isBlank()) prefixe = suggererPrefixe(ligne.niveau);

            for (int i = 1; i <= nb; i++) codesGeneres.add(prefixe + i);
            total += nb;

            // Vérif conflit salle/vacation
            Salle salle = ligne.comboSalle.getSelectionModel().getSelectedItem();
            if (salle != null && vacation != null) {
                if (ligne.spinNb.getValue() > 1) {
                    alertesSalles.add("« " + ligne.niveau.getNomNiveau()
                            + " » : plusieurs classes ne peuvent pas partager la même salle.");
                }
            }
        }

        if (total == 0) {
            lblApercuResume.setText("Aucune classe à créer");
            lblApercuDetails.setText("Cochez au moins un niveau.");
            btnCreer.setDisable(true);
            return;
        }

        btnCreer.setDisable(!alertesSalles.isEmpty());
        lblApercuResume.setText(total + " classe" + (total > 1 ? "s" : "") + " seront créées");

        StringBuilder details = new StringBuilder();
        int max = Math.min(codesGeneres.size(), 12);
        details.append(String.join(", ", codesGeneres.subList(0, max)));
        if (codesGeneres.size() > max) details.append("…");

        if (!alertesSalles.isEmpty()) {
            details.append("\n⚠️ ").append(String.join(" / ", alertesSalles));
        }

        lblApercuDetails.setText(details.toString());
    }

    /* ============================================================
       BOUTONS
       ============================================================ */
    private void configurerBoutons() {
        btnAnnuler.setOnAction(ev -> fermer());
        btnCreer.setOnAction(ev -> handleCreer());
    }

    @javafx.fxml.FXML
    private void handleAnnuler() { fermer(); }

    @javafx.fxml.FXML
    private void handleCreer() {
        List<Classe> classes = construireClasses();

        if (classes.isEmpty()) {
            ToastNotification.avertissement(StageHelper.getStage(btnCreer),
                    "Aucune classe sélectionnée.");
            return;
        }

        Stage stage = StageHelper.getStage(btnCreer);
        btnCreer.setDisable(true);
        btnAnnuler.setDisable(true);   // ← empêche de fermer pendant l'appel

        classeService.creerEnMasseAsync(idEcole, idAnnee, classes,
                resultat -> Platform.runLater(() -> {
                    // On réactive AVANT de gérer le résultat (au cas où on reste ouvert)
                    btnCreer.setDisable(false);
                    btnAnnuler.setDisable(false);
                    gererResultat(stage, resultat);
                }),
                erreur -> Platform.runLater(() -> {
                    btnCreer.setDisable(false);
                    btnAnnuler.setDisable(false);
                    ToastNotification.erreur(stage,
                            erreur.getMessage() != null
                                    ? erreur.getMessage()
                                    : "Échec de la création multiple.");
                }));
    }
//    private void handleCreer() {
//        List<Classe> classes = construireClasses();
//
//        if (classes.isEmpty()) {
//            ToastNotification.avertissement(StageHelper.getStage(btnCreer),
//                    "Aucune classe sélectionnée.");
//            return;
//        }
//
//        Stage stage = StageHelper.getStage(btnCreer);
//        btnCreer.setDisable(true);
//
//        classeService.creerEnMasseAsync(idEcole, idAnnee, classes,
//                resultat -> Platform.runLater(() -> {
//                    btnCreer.setDisable(false);
//                    gererResultat(stage, resultat);
//                }),
//                erreur -> Platform.runLater(() -> {
//                    btnCreer.setDisable(false);
//                    ToastNotification.erreur(stage,
//                            erreur.getMessage() != null
//                                    ? erreur.getMessage()
//                                    : "Échec de la création multiple.");
//                }));
//    }

    private List<Classe> construireClasses() {
        List<Classe> classes = new ArrayList<>();
        Classe.Vacation vacation = comboVacation.getSelectionModel().getSelectedItem();
        if (vacation == null) vacation = Classe.Vacation.JOURNEE_CONTINUE;

        for (LigneNiveau ligne : lignesParNiveau.values()) {
            if (!ligne.chk.isSelected()) continue;

            int nb = ligne.spinNb.getValue();
            int cap = ligne.spinCap.getValue();
            String prefixe = ligne.txtPrefixe.getText() != null
                    ? ligne.txtPrefixe.getText().trim() : "";
            if (prefixe.isBlank()) prefixe = suggererPrefixe(ligne.niveau);

            Salle salle = ligne.comboSalle.getSelectionModel().getSelectedItem();
            String idSalle = (salle != null) ? salle.getId() : null;

            for (int i = 1; i <= nb; i++) {
                Classe c = new Classe();
                c.setIdEcole(idEcole);
                c.setIdAnnee(idAnnee);
                c.setIdNiveau(ligne.niveau.getId());
                c.setCodeClasse(prefixe + i);
                c.setNomClasse(ligne.niveau.getNomNiveau() + " " + i);
                c.setVacation(vacation);
                c.setCapacite(cap);
                // Une seule salle pour la première classe, pas pour les suivantes
                c.setIdSalle(i == 1 ? idSalle : null);
                classes.add(c);
            }
        }
        return classes;
    }

    /* ============================================================
       RÉSULTAT
       ============================================================ */
//    private void gererResultat(Stage stage, ResultatCreationMasse resultat) {
//        if (resultat == null) return;
//
//        auditService.tracerActionAsync("ACADEMIQUE", "CREATION_MULTIPLE_CLASSES",
//                "Création multiple — " + resultat.creees() + " créées, "
//                        + resultat.ignorees() + " ignorées");
//
//        if (resultat.estSucces()) {
//            ToastNotification.succes(stage,
//                    resultat.creees() + " classe(s) créée(s) avec succès.");
//        } else {
//            StringBuilder msg = new StringBuilder();
//            if (resultat.creees() > 0) msg.append("✅ ").append(resultat.creees()).append(" créée(s).\n");
//            if (resultat.ignorees() > 0) msg.append("⚠️ ").append(resultat.ignorees()).append(" ignorée(s) (doublons).\n");
//            if (!resultat.erreurs().isEmpty()) {
//                msg.append("❌ ").append(resultat.erreurs().size()).append(" erreur(s) :\n");
//                int max = Math.min(resultat.erreurs().size(), 5);
//                msg.append(String.join("\n", resultat.erreurs().subList(0, max)));
//                if (resultat.erreurs().size() > max) msg.append("\n…");
//            }
//            AlertUtil.afficherInformation("Résultat", msg.toString(), stage);
//        }
//
//        if (onSuccess != null) onSuccess.run();
//        fermer();
//    }

    private void gererResultat(Stage stage, ResultatCreationMasse resultat) {
        if (resultat == null) return;

        // Audit
        auditService.tracerActionAsync("ACADEMIQUE", "CREATION_MULTIPLE_CLASSES",
                "Création multiple — " + resultat.creees() + " créées, "
                        + resultat.ignorees() + " ignorées");

        /* ═══════════ CAS 1 : SUCCÈS TOTAL ═══════════ */
        // Toutes les classes demandées ont été créées
        if (resultat.estSucces()) {
            ToastNotification.succes(stage,
                    resultat.creees() + " classe(s) créée(s) avec succès.");
            if (onSuccess != null) onSuccess.run();
            fermer();
            return;
        }

        /* ═══════════ CAS 2 : ÉCHEC TOTAL ═══════════ */
        // Aucune classe créée → on garde la modal ouverte pour correction
        if (resultat.creees() == 0) {
            StringBuilder msg = new StringBuilder();
            msg.append("Aucune classe n'a pu être créée.\n\n");

            if (resultat.ignorees() > 0) {
                msg.append("• ").append(resultat.ignorees())
                        .append(" ignorée(s) (doublons déjà existants)\n\n");
            }
            if (!resultat.erreurs().isEmpty()) {
                int max = Math.min(resultat.erreurs().size(), 8);
                msg.append(String.join("\n", resultat.erreurs().subList(0, max)));
                if (resultat.erreurs().size() > max) msg.append("\n…");
            }

            // ⚠️ PAS de fermer() — l'utilisateur peut corriger et réessayer
            AlertUtil.afficherInformation("Échec de la création", msg.toString(), stage);
            return;
        }

        /* ═══════════ CAS 3 : SUCCÈS PARTIEL ═══════════ */
        // Au moins 1 classe créée MAIS avec des erreurs/ignorées → on ferme
        StringBuilder msg = new StringBuilder();
        msg.append("✅ ").append(resultat.creees()).append(" classe(s) créée(s).\n");

        if (resultat.ignorees() > 0) {
            msg.append("⚠️ ").append(resultat.ignorees())
                    .append(" ignorée(s) (doublons).\n");
        }
        if (!resultat.erreurs().isEmpty()) {
            msg.append("❌ ").append(resultat.erreurs().size()).append(" erreur(s) :\n");
            int max = Math.min(resultat.erreurs().size(), 5);
            msg.append(String.join("\n", resultat.erreurs().subList(0, max)));
            if (resultat.erreurs().size() > max) msg.append("\n…");
        }

        AlertUtil.afficherInformation("Résultat partiel", msg.toString(), stage);
        if (onSuccess != null) onSuccess.run();
        fermer();
    }


    /* ============================================================
       FERMETURE
       ============================================================ */
    private void fermer() {
        Stage stage = StageHelper.getStage(btnAnnuler);
        if (stage != null) stage.close();
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