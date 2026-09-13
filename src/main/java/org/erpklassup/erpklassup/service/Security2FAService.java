package org.erpklassup.erpklassup.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import javafx.scene.image.Image;
import org.erpklassup.erpklassup.dao.CodeSecours2FADAO;
import org.erpklassup.erpklassup.dao.UtilisateurDAO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

public class Security2FAService {

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final CodeSecours2FADAO codeSecoursDAO = new CodeSecours2FADAO();

    // ==========================================
    // GÉNÉRATION CLÉ / QR
    // ==========================================

    public String genererCleSecrete() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    public boolean verifierCodeTOTP(String secret2FA, int codeSaisi) {
        if (secret2FA == null || secret2FA.trim().isEmpty()) return false;
        return gAuth.authorize(secret2FA, codeSaisi);
    }

    public Image genererQrCode(String username, String nomEcole, String secret2FA) {
        try {
            String otpauthUrl = String.format(
                    "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                    nomEcole, username, secret2FA, nomEcole);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(otpauthUrl, BarcodeFormat.QR_CODE, 220, 220);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", out);

            return new Image(new ByteArrayInputStream(out.toByteArray()));
        } catch (Exception e) {
            System.err.println("❌ Erreur génération QR : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ==========================================
    // CODES DE SECOURS (hachés bcrypt en BDD)
    // ==========================================

    /**
     * Génère 8 codes de secours, les enregistre HACHÉS en BDD,
     * et retourne les codes EN CLAIR pour affichage unique.
     */
    public List<String> genererCodesSecours(String idUtilisateur, int nombre) {
        return codeSecoursDAO.genererEtEnregistrer(idUtilisateur, nombre);
    }

    /**
     * Vérifie un code de secours saisi par l'utilisateur et le consomme.
     */
    public boolean verifierCodeSecours(String idUtilisateur, String codeSaisi) {
        return codeSecoursDAO.verifierEtConsommer(idUtilisateur, codeSaisi);
    }

    /**
     * Supprime tous les codes de secours (à appeler lors de la désactivation 2FA).
     */
    public void supprimerCodesSecours(String idUtilisateur) {
        codeSecoursDAO.supprimerTousLesCodes(idUtilisateur);
    }

    /**
     * Compte les codes restants.
     */
    public int compterCodesRestants(String idUtilisateur) {
        return codeSecoursDAO.compterCodesRestants(idUtilisateur);
    }
}