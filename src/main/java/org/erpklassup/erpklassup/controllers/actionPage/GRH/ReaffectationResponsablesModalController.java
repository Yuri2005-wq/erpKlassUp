package org.erpklassup.erpklassup.controllers.actionPage.GRH;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.erpklassup.erpklassup.dto.FiltrePersonnel;
import org.erpklassup.erpklassup.dto.PersonnelLigne;
import org.erpklassup.erpklassup.models.GRH.Departement;
import org.erpklassup.erpklassup.service.GRH.DepartementService;
import org.erpklassup.erpklassup.service.GRH.PersonnelService;
import org.erpklassup.erpklassup.util.AlertUtil;
import org.erpklassup.erpklassup.util.ToastNotification;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReaffectationResponsablesModalController {

    /* ============================================================
       COMPOSANTS FXML
       ============================================================ */
    @FXML private ComboBox<PersonnelLigne> comboAncienResponsable;
    @FXML private ComboBox<PersonnelLigne> comboNouveauResponsable;
    @FXML private Button btnTransfererTout;
    @FXML private TextField fieldFiltreDept;
    @FXML private Button btnRetirerSelection;

    @FXML private TableView<Departement> tableResponsables;
    @FXML private TableColumn<Departement, String> colDeptLibelle;
    @FXML private TableColumn<Departement, String> colDeptResponsableActuel;
    @FXML private TableColumn<Departement, PersonnelLigne> colChangerResponsable;

    @FXML private Label lblMessage;
    @FXML private Label lblNbModifs;
    @FXML private Button btnFermer;
    @FXML private Button btnEnregistrerModifs;

    /* ============================================================
       SERVICES & ÉTAT
       ============================================================ */
    private final DepartementService departementService = new DepartementService();
    private final PersonnelService personnelService = new PersonnelService();

    private String idEcoleCourante;
    private Runnable onModificationsEnregistrees;

    // Stocke les modifications en attente (ID Département -> Nouvel ID Personnel / null)
    private final Map<String, String> modificationsMap = new HashMap<>();
    private ObservableList<Departement> listeDepartements = FXCollections.observableArrayList();
    private ObservableList<PersonnelLigne> listePersonnel = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurerTable();
    }

    /**
     * Initialise les données de l'école et charge les listes de départements et du personnel.
     */
    public void initData(String idEcole, Runnable onEnregistre) {
        this.idEcoleCourante = idEcole;
        this.onModificationsEnregistrees = onEnregistre;

        chargerPersonnel();
        chargerDepartements();
    }

    private void chargerPersonnel() {
        if (idEcoleCourante == null) return;

        FiltrePersonnel filtre = new FiltrePersonnel(
                idEcoleCourante, null, null, null, null, 1, Integer.MAX_VALUE
        );

        personnelService.rechercherAsync(filtre,
                resultat -> Platform.runLater(() -> {
                    listePersonnel.setAll(resultat.elements());
                    // Configurer les ComboBox du transfert rapide
                    configurerCellulesComboBox(comboAncienResponsable);
                    configurerCellulesComboBox(comboNouveauResponsable);

                    comboAncienResponsable.setItems(listePersonnel);
                    comboNouveauResponsable.setItems(listePersonnel);
                }),
                err -> System.err.println("❌ Erreur chargement personnel modale : " + err.getMessage())
        );
    }

    private void chargerDepartements() {
        if (idEcoleCourante == null) return;

        departementService.chargerTousParEcoleAsync(idEcoleCourante,
                depts -> Platform.runLater(() -> {
                    listeDepartements = FXCollections.observableArrayList(depts);
                    tableResponsables.setItems(listeDepartements);
                }),
                err -> System.err.println("❌ Erreur chargement départements modale : " + err.getMessage())
        );
    }

    private void configurerCellulesComboBox(ComboBox<PersonnelLigne> combo) {
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PersonnelLigne item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.nomComplet() + " (" + (item.poste() != null ? item.poste() : "Employé") + ")");
            }
        });
        combo.setButtonCell(combo.getCellFactory().call(null));
    }

    private void configurerTable() {
        tableResponsables.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colDeptLibelle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLibelle()));

        // Nom du responsable actuel (résolu via l'ID)
        colDeptResponsableActuel.setCellValueFactory(d -> {
            String idResp = d.getValue().getIdResponsable();
            if (idResp == null || idResp.isBlank()) return new SimpleStringProperty("Non assigné");

            String nom = listePersonnel.stream()
                    .filter(p -> p.idPersonnel().equals(idResp))
                    .map(PersonnelLigne::nomComplet)
                    .findFirst()
                    .orElse("Inconnu");
            return new SimpleStringProperty(nom);
        });

        // Colonne interactive pour changer le responsable en 1 clic
        colChangerResponsable.setCellValueFactory(d -> new SimpleObjectProperty<>());
        colChangerResponsable.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<PersonnelLigne> comboChef = new ComboBox<>();

            {
                comboChef.setMaxWidth(Double.MAX_VALUE);
                configurerCellulesComboBox(comboChef);

                comboChef.valueProperty().addListener((obs, anc, nouv) -> {
                    Departement dept = getTableView().getItems().get(getIndex());
                    if (dept != null) {
                        String nouveauId = (nouv != null) ? nouv.idPersonnel() : null;
                        modificationsMap.put(dept.getIdDepartement(), nouveauId);
                        mettreAJourCompteur();
                    }
                });
            }

            @Override
            protected void updateItem(PersonnelLigne item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Departement dept = getTableView().getItems().get(getIndex());
                    comboChef.setItems(listePersonnel);

                    // Pré-sélectionner le responsable actuel ou modifié
                    if (dept != null) {
                        String idActuel = modificationsMap.containsKey(dept.getIdDepartement())
                                ? modificationsMap.get(dept.getIdDepartement())
                                : dept.getIdResponsable();

                        if (idActuel != null) {
                            listePersonnel.stream()
                                    .filter(p -> p.idPersonnel().equals(idActuel))
                                    .findFirst()
                                    .ifPresent(comboChef::setValue);
                        } else {
                            comboChef.getSelectionModel().clearSelection();
                        }
                    }
                    setGraphic(comboChef);
                }
            }
        });
    }

    /* ============================================================
       ACTIONS MÉTIER
       ============================================================ */

    @FXML
    private void handleTransfererTout() {
        PersonnelLigne ancien = comboAncienResponsable.getValue();
        PersonnelLigne nouveau = comboNouveauResponsable.getValue();

        if (ancien == null || nouveau == null) {
            AlertUtil.afficherAvertissement("Validation", "Veuillez sélectionner l'ancien et le nouveau responsable.", getFenêtre());
            return;
        }

        if (ancien.idPersonnel().equals(nouveau.idPersonnel())) {
            AlertUtil.afficherAvertissement("Validation", "L'ancien et le nouveau responsable doivent être différents.", getFenêtre());
            return;
        }

        // Marquer tous les départements de l'ancien responsable vers le nouveau
        int count = 0;
        for (Departement dept : listeDepartements) {
            if (ancien.idPersonnel().equals(dept.getIdResponsable())) {
                modificationsMap.put(dept.getIdDepartement(), nouveau.idPersonnel());
                count++;
            }
        }

        if (count > 0) {
            tableResponsables.refresh();
            mettreAJourCompteur();
            ToastNotification.succes(getFenêtre(), count + " département(s) réaffecté(s) en attente d'enregistrement.");
        } else {
            ToastNotification.info(getFenêtre(), "Aucun département trouvé sous la responsabilité de cet employé.");
        }
    }

    @FXML
    private void handleRetirerSelection() {
        Departement selected = tableResponsables.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.afficherAvertissement("Sélection", "Veuillez sélectionner un département dans le tableau.", getFenêtre());
            return;
        }

        modificationsMap.put(selected.getIdDepartement(), null);
        tableResponsables.refresh();
        mettreAJourCompteur();
        ToastNotification.info(getFenêtre(), "Responsable retiré pour : " + selected.getLibelle());
    }

    @FXML
    private void handleEnregistrerModifs() {
        if (modificationsMap.isEmpty()) {
            ToastNotification.info(getFenêtre(), "Aucune modification à enregistrer.");
            return;
        }

        boolean ok = AlertUtil.afficherConfirmation("Confirmation",
                "Voulez-vous enregistrer les changements de responsables pour " + modificationsMap.size() + " département(s) ?",
                getFenêtre());

        if (!ok) return;

        // Traitement par lot sécurisé via le service
        final int[] successCount = {0};
        for (Map.Entry<String, String> entry : modificationsMap.entrySet()) {
            String idDept = entry.getKey();
            String idResp = entry.getValue();

            // Recherche du département pour garder son adjoint actuel s'il existe
            Departement dept = listeDepartements.stream()
                    .filter(d -> d.getIdDepartement().equals(idDept))
                    .findFirst().orElse(null);

            String idAdjoint = (dept != null) ? dept.getIdResponsableAdjoint() : null;

            departementService.reassignerResponsablesAsync(idDept, idResp, idAdjoint,
                    () -> {
                        successCount[0]++;
                        if (successCount[0] == modificationsMap.size()) {
                            Platform.runLater(() -> {
                                ToastNotification.succes(getFenêtre(), "Modifications enregistrées avec succès !");
                                if (onModificationsEnregistrees != null) {
                                    onModificationsEnregistrees.run();
                                }
                                fermerFenetre();
                            });
                        }
                    },
                    err -> System.err.println("❌ Erreur réaffectation lot : " + err.getMessage())
            );
        }
    }

    @FXML
    private void handleFermer() {
        fermerFenetre();
    }

    private void mettreAJourCompteur() {
        int nb = modificationsMap.size();
        lblNbModifs.setText(nb + " modification(s) en attente");
    }

    private Stage getFenêtre() {
        return (Stage) btnFermer.getScene().getWindow();
    }

    private void fermerFenetre() {
        getFenêtre().close();
    }
}