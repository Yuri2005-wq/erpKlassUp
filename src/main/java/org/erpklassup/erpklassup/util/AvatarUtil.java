package org.erpklassup.erpklassup.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AvatarUtil {

    private static final double TAILLE_DEFAUT = 36;
    private static final Map<String, Image> CACHE_IMAGES = new ConcurrentHashMap<>();
    private static final String[] PALETTE = {
            "#2A3B61", "#7C3AED", "#0EA5E9", "#16A34A", "#D97706", "#DB2777", "#0891B2"
    };

    private AvatarUtil() {}

    public static StackPane creer(String cheminPhoto, String nom, String prenom, String idPourCouleur) {
        return creer(cheminPhoto, nom, prenom, idPourCouleur, TAILLE_DEFAUT);
    }

    public static StackPane creer(String cheminPhoto, String nom, String prenom, String idPourCouleur, double taille) {
        StackPane conteneur = new StackPane();
        conteneur.setPrefSize(taille, taille);
        conteneur.setMaxSize(taille, taille);
        conteneur.setMinSize(taille, taille);

        Image image = chargerImage(cheminPhoto);

        if (image != null && !image.isError()) {
            ImageView vue = new ImageView(image);
            vue.setFitWidth(taille);
            vue.setFitHeight(taille);
            vue.setPreserveRatio(false);
            vue.setSmooth(true);
            vue.setClip(new Circle(taille / 2, taille / 2, taille / 2));
            conteneur.getChildren().add(vue);
        } else {
            Circle fond = new Circle(taille / 2);
            fond.setFill(Color.web(couleurPour(idPourCouleur)));
            Label initiales = new Label(initiales(nom, prenom));
            initiales.setTextFill(Color.WHITE);
            initiales.setFont(Font.font("System", FontWeight.BOLD, taille * 0.38));
            conteneur.getChildren().addAll(fond, initiales);
        }
        return conteneur;
    }

    private static Image chargerImage(String cheminPhoto) {
        if (cheminPhoto == null || cheminPhoto.isBlank()) return null;
        Image cache = CACHE_IMAGES.get(cheminPhoto);
        if (cache != null) return cache;

        File fichier = new File(cheminPhoto);
        if (!fichier.exists() || !fichier.isFile()) return null;

        Image image = new Image(fichier.toURI().toString(), 72, 72, false, true, true);
        CACHE_IMAGES.put(cheminPhoto, image);
        return image;
    }

    private static String initiales(String nom, String prenom) {
        StringBuilder sb = new StringBuilder();
        if (prenom != null && !prenom.isBlank()) sb.append(Character.toUpperCase(prenom.charAt(0)));
        if (nom != null && !nom.isBlank()) sb.append(Character.toUpperCase(nom.charAt(0)));
        return !sb.isEmpty() ? sb.toString() : "?";
    }

    private static String couleurPour(String id) {
        if (id == null) return PALETTE[0];
        return PALETTE[Math.abs(id.hashCode()) % PALETTE.length];
    }
}
