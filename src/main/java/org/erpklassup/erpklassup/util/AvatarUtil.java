package org.erpklassup.erpklassup.util;

import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.text.Normalizer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AvatarUtil {

    /* ============================================================
       ÉTATS DU PERSONNEL (couleur du contour + de la pastille)
       ============================================================ */
    public enum EtatAvatar {
        ACTIF("#22C55E", "actif"),
        EN_CONGE("#3B82F6", "En congé"),
        SUSPENDU("#F59E0B", "suspendu"),
        INACTIF("#94A3B8", "inactif"),
        PARTI("#EF4444", "Parti");

        private final String couleur;
        private final String libelle;

        EtatAvatar(String couleur, String libelle) {
            this.couleur = couleur;
            this.libelle = libelle;
        }

        public String couleur() { return couleur; }
        public String libelle() { return libelle; }

        /**
         * Convertit le statut brut (enum StatutEmploi ou texte de la base) en état d'avatar.
         * C'est le SEUL endroit à adapter si tu ajoutes de nouveaux statuts.
         * Retourne null si le statut est absent : l'avatar s'affiche alors sans contour ni pastille.
         */
        public static EtatAvatar depuis(String statut) {
            if (statut == null || statut.isBlank()) return null;

            String s = Normalizer.normalize(statut, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .trim()
                    .replaceAll("[\\s\\-]+", "_");

            return switch (s) {
                case "actif" -> ACTIF;
                case "EN_CONGE", "CONGE", "EN_CONGES" -> EN_CONGE;
                case "suspendu", "suspension" -> SUSPENDU;
                case "PARTI", "DEMISSIONNAIRE", "DEMISSION", "LICENCIE", "RETRAITE",
                     "TERMINE", "SORTI", "FIN_CONTRAT" -> PARTI;
                default -> INACTIF;
            };
        }
    }

    /* ============================================================
       CONSTANTES
       ============================================================ */
    private static final double TAILLE_DEFAUT = 34;

    /** Plus grand côté de l'image décodée (px) : net sur écran HiDPI, léger en mémoire. */
    private static final double TAILLE_CHARGEMENT = 400;

    /** Zoom du recadrage : 1.0 = carré complet, 0.92 = visage un peu plus grand. */
    private static final double ZOOM = 0.92;

    /** Position verticale du centre du visage sur une photo en portrait (0 = haut, 1 = bas). */
    private static final double CENTRE_VISAGE_Y = 0.42;

    /** Épaisseur du contour (proportionnelle à la taille, avec un minimum). */
    private static final double RATIO_CONTOUR = 0.04;
    private static final double CONTOUR_MIN = 1.5;

    /** Espace blanc entre le contour et la photo. Mets les deux valeurs à 0 pour coller le contour à la photo. */
    private static final double RATIO_ESPACE = 0.05;
    private static final double ESPACE_MIN = 1.5;

    /** Diamètre de la pastille par rapport à l'avatar. */
    private static final double RATIO_PASTILLE = 0.28;
    private static final double PASTILLE_MIN = 8;

    private static final Map<String, Image> CACHE_IMAGES = new ConcurrentHashMap<>();
    private static final String[] PALETTE = {
            "#2A3B61", "#7C3AED", "#0EA5E9", "#16A34A", "#D97706", "#DB2777", "#0891B2"
    };

    private AvatarUtil() {}

    /* ============================================================
       API PUBLIQUE
       ============================================================ */

    /** Sans statut : avatar simple, sans contour ni pastille. */
    public static StackPane creer(String cheminPhoto, String nom, String prenom, String idPourCouleur) {
        return creer(cheminPhoto, nom, prenom, idPourCouleur, TAILLE_DEFAUT, null);
    }

    public static StackPane creer(String cheminPhoto, String nom, String prenom, String idPourCouleur, double taille) {
        return creer(cheminPhoto, nom, prenom, idPourCouleur, taille, null);
    }

    /** Avec statut, taille par défaut (vue tableau). */
    public static StackPane creer(String cheminPhoto, String nom, String prenom, String idPourCouleur, String statut) {
        return creer(cheminPhoto, nom, prenom, idPourCouleur, TAILLE_DEFAUT, statut);
    }

    /** Avec statut : contour + pastille de la couleur de l'état. */
    public static StackPane creer(String cheminPhoto, String nom, String prenom,
                                  String idPourCouleur, double taille, String statut) {
        EtatAvatar etat = EtatAvatar.depuis(statut);

        StackPane conteneur = new StackPane();
        conteneur.setPrefSize(taille, taille);
        conteneur.setMaxSize(taille, taille);
        conteneur.setMinSize(taille, taille);

        // Diamètre réel de la photo : on laisse la place au contour et à l'espace blanc
        double epaisseurContour = 0;
        double diametrePhoto = taille;
        if (etat != null) {
            epaisseurContour = Math.max(CONTOUR_MIN, taille * RATIO_CONTOUR);
            double espace = Math.max(ESPACE_MIN, taille * RATIO_ESPACE);
            diametrePhoto = taille - 2 * (epaisseurContour + espace);
        }

        conteneur.getChildren().add(creerContenu(cheminPhoto, nom, prenom, idPourCouleur, diametrePhoto));

        if (etat != null) {
            Color couleur = Color.web(etat.couleur());

            Circle contour = new Circle((taille - epaisseurContour) / 2.0);
            contour.setFill(Color.TRANSPARENT);
            contour.setStroke(couleur);
            contour.setStrokeWidth(epaisseurContour);
            contour.setMouseTransparent(true);

            double d = Math.max(PASTILLE_MIN, taille * RATIO_PASTILLE);
            double bordure = Math.max(1.5, d * 0.18);
            Circle pastille = new Circle((d - bordure) / 2.0);
            pastille.setFill(couleur);
            pastille.setStroke(Color.WHITE);
            pastille.setStrokeWidth(bordure);
            StackPane.setAlignment(pastille, Pos.BOTTOM_RIGHT);

            conteneur.getChildren().addAll(contour, pastille);
            Tooltip.install(conteneur, new Tooltip(etat.libelle()));
        }
        return conteneur;
    }

    /* ============================================================
       CONTENU : PHOTO RECADRÉE OU INITIALES
       ============================================================ */
    private static Node creerContenu(String cheminPhoto, String nom, String prenom, String id, double diametre) {
        StackPane zone = new StackPane();
        zone.setPrefSize(diametre, diametre);
        zone.setMaxSize(diametre, diametre);
        zone.setMinSize(diametre, diametre);

        Image image = chargerImage(cheminPhoto);
        if (image == null || image.isError()) {
            zone.getChildren().setAll(creerInitiales(nom, prenom, id, diametre));
            return zone;
        }

        ImageView vue = new ImageView(image);
        vue.setFitWidth(diametre);
        vue.setFitHeight(diametre);
        vue.setPreserveRatio(false);  // le viewport est toujours carré : aucune déformation possible
        vue.setSmooth(true);
        vue.setCache(true);
        vue.setClip(new Circle(diametre / 2, diametre / 2, diametre / 2));
        vue.setOpacity(0);            // masquée tant que le recadrage n'est pas appliqué (évite un flash déformé)
        zone.getChildren().setAll(vue);

        if (image.getProgress() >= 1.0) {
            appliquerRecadrage(vue, image);
        } else {
            image.progressProperty().addListener((obs, ancien, progres) -> {
                if (progres.doubleValue() >= 1.0 && !image.isError()) {
                    appliquerRecadrage(vue, image);
                }
            });
        }

        image.errorProperty().addListener((obs, ancien, enErreur) -> {
            if (Boolean.TRUE.equals(enErreur)) {
                zone.getChildren().setAll(creerInitiales(nom, prenom, id, diametre));
            }
        });

        return zone;
    }

    /**
     * Recadrage « cover » : un carré est extrait de l'image, qui remplit donc tout le cercle.
     * Portrait : le carré est positionné sur le visage. Paysage ou carré : centré.
     */
    private static void appliquerRecadrage(ImageView vue, Image image) {
        double w = image.getWidth();
        double h = image.getHeight();
        if (w <= 0 || h <= 0) return;

        double cote = Math.min(w, h) * ZOOM;
        double x = (w - cote) / 2.0;
        double y = (h > w)
                ? h * CENTRE_VISAGE_Y - cote / 2.0
                : (h - cote) / 2.0;

        x = Math.max(0, Math.min(x, w - cote));
        y = Math.max(0, Math.min(y, h - cote));

        vue.setViewport(new Rectangle2D(x, y, cote, cote));
        vue.setOpacity(1);
    }

    private static Image chargerImage(String cheminPhoto) {
        if (cheminPhoto == null || cheminPhoto.isBlank()) return null;

        File fichier = new File(cheminPhoto);
        if (!fichier.exists() || !fichier.isFile()) return null;

        // La clé inclut la date de modification : une photo remplacée est rechargée
        String cle = cheminPhoto + "|" + fichier.lastModified();
        Image cache = CACHE_IMAGES.get(cle);
        if (cache != null && !cache.isError()) return cache;

        Image image = new Image(fichier.toURI().toString(),
                TAILLE_CHARGEMENT, TAILLE_CHARGEMENT, true, true, true);
        CACHE_IMAGES.put(cle, image);
        return image;
    }

    /* ============================================================
       INITIALES (repli si pas de photo)
       ============================================================ */
    private static Node creerInitiales(String nom, String prenom, String id, double diametre) {
        Circle fond = new Circle(diametre / 2);
        fond.setFill(Color.web(couleurPour(id)));
        Label initiales = new Label(initiales(nom, prenom));
        initiales.setTextFill(Color.WHITE);
        initiales.setFont(Font.font("System", FontWeight.BOLD, diametre * 0.38));
        return new StackPane(fond, initiales);
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
//package org.erpklassup.erpklassup.util;
//
//import javafx.scene.image.Image;
//import javafx.scene.image.ImageView;
//import javafx.scene.control.Label;
//import javafx.scene.layout.StackPane;
//import javafx.scene.paint.Color;
//import javafx.scene.shape.Circle;
//import javafx.scene.text.Font;
//import javafx.scene.text.FontWeight;
//

//import java.io.File;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//public final class AvatarUtil {
//
//    private static final double TAILLE_DEFAUT = 34;
//    private static final Map<String, Image> CACHE_IMAGES = new ConcurrentHashMap<>();
//    private static final String[] PALETTE = {
//            "#2A3B61", "#7C3AED", "#0EA5E9", "#16A34A", "#D97706", "#DB2777", "#0891B2"
//    };
//
//    private AvatarUtil() {}
//
//    public static StackPane creer(String cheminPhoto, String nom, String prenom, String idPourCouleur) {
//        return creer(cheminPhoto, nom, prenom, idPourCouleur, TAILLE_DEFAUT);
//    }
//
//    public static StackPane creer(String cheminPhoto, String nom, String prenom, String idPourCouleur, double taille) {
//        StackPane conteneur = new StackPane();
//        conteneur.setPrefSize(taille, taille);
//        conteneur.setMaxSize(taille, taille);
//        conteneur.setMinSize(taille, taille);
//
//        Image image = chargerImage(cheminPhoto);
//
//        if (image != null && !image.isError()) {
//            ImageView vue = new ImageView(image);
//            vue.setFitWidth(taille);
//            vue.setFitHeight(taille);
//            vue.setPreserveRatio(true);
//            vue.setSmooth(true);
//            vue.setClip(new Circle(taille / 2, taille / 2, taille / 2));
//            conteneur.getChildren().add(vue);
//        } else {
//            Circle fond = new Circle(taille / 2);
//            fond.setFill(Color.web(couleurPour(idPourCouleur)));
//            Label initiales = new Label(initiales(nom, prenom));
//            initiales.setTextFill(Color.WHITE);
//            initiales.setFont(Font.font("System", FontWeight.BOLD, taille * 0.38));
//            conteneur.getChildren().addAll(fond, initiales);
//        }
//        return conteneur;
//    }
//
//    private static Image chargerImage(String cheminPhoto) {
//        if (cheminPhoto == null || cheminPhoto.isBlank()) return null;
//        Image cache = CACHE_IMAGES.get(cheminPhoto);
//        if (cache != null) return cache;
//
//        File fichier = new File(cheminPhoto);
//        if (!fichier.exists() || !fichier.isFile()) return null;
//
//        Image image = new Image(fichier.toURI().toString(), 72, 72, false, true, true);
//        CACHE_IMAGES.put(cheminPhoto, image);
//        return image;
//    }
//
//    private static String initiales(String nom, String prenom) {
//        StringBuilder sb = new StringBuilder();
//        if (prenom != null && !prenom.isBlank()) sb.append(Character.toUpperCase(prenom.charAt(0)));
//        if (nom != null && !nom.isBlank()) sb.append(Character.toUpperCase(nom.charAt(0)));
//        return !sb.isEmpty() ? sb.toString() : "?";
//    }
//
//    private static String couleurPour(String id) {
//        if (id == null) return PALETTE[0];
//        return PALETTE[Math.abs(id.hashCode()) % PALETTE.length];
//    }
//}
