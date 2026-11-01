package org.erpklassup.erpklassup.util.grh;

import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Champ en lecture seule : libellé discret au-dessus de la valeur.
 * Utilisable directement en FXML : {@code <ChampLecture libelle="Téléphone" fx:id="telephone"/>}.
 */

public class ChampLecture extends VBox {

    private final Label libelle = new Label();
    private final Label valeur = new Label(FicheUi.VIDE);

    public ChampLecture() {
        getStyleClass().add("champ");
        libelle.getStyleClass().add("champ-libelle");
        valeur.getStyleClass().add("champ-valeur");
        valeur.setWrapText(true);
        getChildren().addAll(libelle, valeur);
    }

    public StringProperty libelleProperty() {
        return libelle.textProperty();
    }

    public String getLibelle() {
        return libelle.getText();
    }

    public void setLibelle(String texte) {
        libelle.setText(texte);
    }

    public String getValeur() {
        return valeur.getText();
    }

    /** Affiche « — » lorsque la valeur est nulle ou vide. */
    public void setValeur(String texte) {
        valeur.setText(texte == null || texte.isBlank() ? FicheUi.VIDE : texte);
    }
}

